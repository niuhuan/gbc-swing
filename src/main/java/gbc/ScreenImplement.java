package gbc;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

/**
 * Graphics
 */
class ScreenImplement extends ScreenAbstract {

    private int[] frameBuffer;

    // tiles & image cache
    private int[] transparentImage = new int[0];
    private int[][] tileImage;
    private boolean[] tileReadState; // true if there are any images to be invalidated

    private int[] tempPix;

    private int windowSourceLine;


    public ScreenImplement(byte[] registers, byte[][] memory, byte[] oam, boolean gbcFeatures, int[] defaultPalette, ScreenListener screenListener, int maxFrameSkip) {
        super(registers, memory, oam, gbcFeatures, screenListener, maxFrameSkip);
        colors = defaultPalette;
        gbcMask = 0x80000000;
        transparentCutoff = gbcFeatures ? 32 : 4;
        tileImage = new int[tileCount * colorCount][];
        tileReadState = new boolean[tileCount];
        tempPix = new int[8 * 8];
        frameBuffer = new int[8 * 8 * 20 * 18];
    }

    /**
     * Writes data to the specified video RAM address
     */
    public final void addressWrite(int addr, byte data) {
        if (videoRam[addr] == data)
            return;

        if (addr < 0x1800) { // Bkg Tile data area
            int tileIndex = (addr >> 4) + tileOffset;

            if (tileReadState[tileIndex]) {
                int r = tileImage.length - tileCount + tileIndex;

                do {
                    tileImage[r] = null;
                    r -= tileCount;
                } while (r >= 0);
                tileReadState[tileIndex] = false;
            }
        }
        videoRam[addr] = data;
    }

    /**
     * This must be called by the ProcessingChip for each scanline drawn by the display hardware.
     */
    public final void notifyScanline(int line) {
        if (skipping || line >= 144) {
            return;
        }

        if (line == 0) {
            windowSourceLine = 0;
        }

        // determine the left edge of the window (160 if window is inactive)
        int windowLeft;
        if (winEnabled && (registers[0x4A] & 0xff) <= line) {
            windowLeft = (registers[0x4B] & 0xff) - 7;
            if (windowLeft > 160)
                windowLeft = 160;
        } else
            windowLeft = 160;

        // step 1: background+window
        boolean skippedAnything = drawBackgroundForLine(line, windowLeft, 0);

        // At this point, the high (alpha) byte in the frameBuffer is 0xff for colors 1,2,3 and
        // 0x00 for color_style 0. Foreground sprites draw on all colors, background sprites draw on
        // top of color_style 0 only.

        // step 2: sprites
        drawSpritesForLine(line);

        // step 3: prio tiles+window
        if (skippedAnything) {
            drawBackgroundForLine(line, windowLeft, 0x80);
        }

        if (windowLeft < 160)
            windowSourceLine++;

        // step 4: to buffer (only last line)
        if (line == 143) {
            updateFrameBufferImage();
        }
    }

    /**
     * Invalidate all tiles in the tile cache for the given palette
     */
    public final void invalidateAll(int pal) {
        int start = pal * tileCount * 4;
        int stop = (pal + 1) * tileCount * 4;
        for (int r = start; r < stop; r++) {
            tileImage[r] = null;
        }
    }

    @Override
    public void setGBCPalette(int index, int data) {
        super.setGBCPalette(index, data);
        if ((index & 0x6) == 0) {
            gbcPalette[index >> 1] &= 0x00ffffff;
        }
    }

    private final void drawSpritesForLine(int line) {
        if (!spritesEnabled)
            return;

        int minSpriteY = doubledSprites ? line - 15 : line - 7;

        // either only do priorityFlag == 0 (all foreground),
        // or first 0x80 (background) and then 0 (foreground)
        int priorityFlag = spritePriorityEnabled ? 0x80 : 0;

        for (; priorityFlag >= 0; priorityFlag -= 0x80) {
            int oamIx = 159;

            while (oamIx >= 0) {
                int attributes = 0xff & this.oam[oamIx--];

                if ((attributes & 0x80) == priorityFlag || !spritePriorityEnabled) {
                    int tileNum = (0xff & this.oam[oamIx--]);
                    int spriteX = (0xff & this.oam[oamIx--]) - 8;
                    int spriteY = (0xff & this.oam[oamIx--]) - 16;

                    int offset = line - spriteY;
                    if (spriteX >= 160 || spriteY < minSpriteY || offset < 0)
                        continue;

                    if (doubledSprites) {
                        tileNum &= 0xFE;
                    }

                    int spriteAttrib = (attributes >> 5) & 0x03; // flipx: from bit 0x20 to 0x01, flipy: from bit 0x40 to 0x02

                    if (this.gbcFeatures) {
                        spriteAttrib += 0x20 + ((attributes & 0x07) << 2); // palette
                        tileNum += (384 >> 3) * (attributes & 0x08); // tile vram bank
                    } else {
                        // attributes 0x10: 0x00 = OBJ1 palette, 0x10 = OBJ2 palette
                        // spriteAttrib: 0x04: OBJ1 palette, 0x08: OBJ2 palette
                        spriteAttrib += 4 + ((attributes & 0x10) >> 2);
                    }

                    if (priorityFlag == 0x80) {
                        // background
                        if (doubledSprites) {
                            if ((spriteAttrib & TILE_FLIPY) != 0) {
                                drawPartBgSprite((tileNum | 1) - (offset >> 3), spriteX, line, offset & 7, spriteAttrib);
                            } else {
                                drawPartBgSprite((tileNum & -2) + (offset >> 3), spriteX, line, offset & 7, spriteAttrib);
                            }
                        } else {
                            drawPartBgSprite(tileNum, spriteX, line, offset, spriteAttrib);
                        }
                    } else {
                        // foreground
                        if (doubledSprites) {
                            if ((spriteAttrib & TILE_FLIPY) != 0) {
                                drawPartFgSprite((tileNum | 1) - (offset >> 3), spriteX, line, offset & 7, spriteAttrib);
                            } else {
                                drawPartFgSprite((tileNum & -2) + (offset >> 3), spriteX, line, offset & 7, spriteAttrib);
                            }
                        } else {
                            drawPartFgSprite(tileNum, spriteX, line, offset, spriteAttrib);
                        }
                    }
                } else {
                    oamIx -= 3;
                }
            }
        }
    }

    private boolean drawBackgroundForLine(int line, int windowLeft, int priority) {
        boolean skippedTile = false;

        int sourceY = line + (this.registers[0x42] & 0xff);
        int sourceImageLine = sourceY & 7;

        int tileNum;
        int tileX = (this.registers[0x43] & 0xff) >> 3;
        int memStart = (hiBgTileMapAddress ? 0x1c00 : 0x1800) + ((sourceY & 0xf8) << 2);

        int screenX = -(this.registers[0x43] & 7);
        for (; screenX < windowLeft; tileX++, screenX += 8) {
            if (bgWindowDataSelect) {
                tileNum = videoRamBanks[0][memStart + (tileX & 0x1f)] & 0xff;
            } else {
                tileNum = 256 + videoRamBanks[0][memStart + (tileX & 0x1f)];
            }

            int tileAttrib = 0;

            if (this.gbcFeatures) {
                int mapAttrib = videoRamBanks[1][memStart + (tileX & 0x1f)];

                if ((mapAttrib & 0x80) != priority) {
                    skippedTile = true;
                    continue;
                }

                tileAttrib += (mapAttrib & 0x07) << 2; // palette
                tileAttrib += (mapAttrib >> 5) & 0x03; // mirroring
                tileNum += 384 * ((mapAttrib >> 3) & 0x01); // tile vram bank
            }

            drawPartCopy(tileNum, screenX, line, sourceImageLine, tileAttrib);
        }

        if (windowLeft < 160) {
            // window!
            int windowStartAddress = hiWinTileMapAddress ? 0x1c00 : 0x1800;

            int tileAddress;

            int windowSourceTileY = windowSourceLine >> 3;
            int windowSourceTileLine = windowSourceLine & 7;

            tileAddress = windowStartAddress + (windowSourceTileY * 32);

            for (screenX = windowLeft; screenX < 160; tileAddress++, screenX += 8) {
                if (bgWindowDataSelect) {
                    tileNum = videoRamBanks[0][tileAddress] & 0xff;
                } else {
                    tileNum = 256 + videoRamBanks[0][tileAddress];
                }

                int tileAttrib = 0;

                if (this.gbcFeatures) {
                    int mapAttrib = videoRamBanks[1][tileAddress];

                    if ((mapAttrib & 0x80) != priority) {
                        skippedTile = true;
                        continue;
                    }

                    tileAttrib += (mapAttrib & 0x07) << 2; // palette
                    tileAttrib += (mapAttrib >> 5) & 0x03; // mirroring
                    tileNum += 384 * ((mapAttrib >> 3) & 0x01); // tile vram bank
                }

                drawPartCopy(tileNum, screenX, line, windowSourceTileLine, tileAttrib);
            }
        }
        return skippedTile;
    }

    private final void updateFrameBufferImage() {
        if (!lcdEnabled) {
            int[] buffer = frameBuffer;
            for (int i = 0; i < buffer.length; i++)
                buffer[i] = -1;
            frameBufferImage = createImage(width, height, buffer);
            return;
        }
        frameBufferImage = createImage(width, height, frameBuffer);
    }

    /**
     * Create the image of a tile in the tile cache by reading the relevant data from video
     * memory
     */
    private final int[] updateImage(int tileIndex, int attribs) {
        int index = tileIndex + tileCount * attribs;

        boolean otherBank = (tileIndex >= 384);

        int offset = otherBank ? ((tileIndex - 384) << 4) : (tileIndex << 4);

        int paletteStart = attribs & 0xfc;

        byte[] vram = otherBank ? videoRamBanks[1] : videoRamBanks[0];
        int[] palette = this.gbcFeatures ? gbcPalette : gbPalette;
        boolean transparent = attribs >= transparentCutoff;

        int pixix = 0;
        int pixixdx = 1;
        int pixixdy = 0;

        if ((attribs & TILE_FLIPY) != 0) {
            pixixdy = -2 * 8;
            pixix = 8 * (8 - 1);
        }
        if ((attribs & TILE_FLIPX) == 0) {
            pixixdx = -1;
            pixix += 8 - 1;
            pixixdy += 8 * 2;
        }

        for (int y = 8; --y >= 0; ) {
            int num = weaveLookup[vram[offset++] & 0xff] +
                    (weaveLookup[vram[offset++] & 0xff] << 1);
            if (num != 0)
                transparent = false;

            for (int x = 8; --x >= 0; ) {
                tempPix[pixix] = palette[paletteStart + (num & 3)];
                pixix += pixixdx;

                num >>= 2;
            }
            pixix += pixixdy;
        }

        if (transparent) {
            tileImage[index] = transparentImage;
        } else {
            tileImage[index] = tempPix;
            tempPix = new int[8 * 8];
        }

        tileReadState[tileIndex] = true;

        return tileImage[index];
    }

    // draws one scanline of the block
    // ignores alpha byte, just copies pixels
    private final void drawPartCopy(int tileIndex, int x, int y, int sourceLine, int attribs) {
        int ix = tileIndex + tileCount * attribs;
        int[] im = tileImage[ix];

        if (im == null) {
            im = updateImage(tileIndex, attribs);
        }

        int dst = x + y * 160;
        int src = sourceLine * 8;
        int dstEnd = (x + 8 > 160) ? ((y + 1) * 160) : (dst + 8);

        if (x < 0) { // adjust left
            dst -= x;
            src -= x;
        }

        while (dst < dstEnd)
            frameBuffer[dst++] = im[src++];
    }

    // draws one scanline of the block
    // overwrites background when source pixel is opaque
    private final void drawPartFgSprite(int tileIndex, int x, int y, int sourceLine, int attribs) {
        int ix = tileIndex + tileCount * attribs;
        int[] im = tileImage[ix];

        if (im == null) {
            im = updateImage(tileIndex, attribs);
        }

        if (im == transparentImage) {
            return;
        }

        int dst = x + y * 160;
        int src = sourceLine * 8;
        int dstEnd = (x + 8 > 160) ? ((y + 1) * 160) : (dst + 8);

        if (x < 0) { // adjust left
            dst -= x;
            src -= x;
        }

        while (dst < dstEnd) {
            if (im[src] < 0) // fast check for 0xff in high byte
                frameBuffer[dst] = im[src];

            dst++;
            src++;
        }
    }

    // draws one scanline of the block
    // overwrites background when source pixel is opaque and background is transparent
    private final void drawPartBgSprite(int tileIndex, int x, int y, int sourceLine, int attribs) {
        int ix = tileIndex + tileCount * attribs;
        int[] im = tileImage[ix];

        if (im == null) {
            im = updateImage(tileIndex, attribs);
        }

        if (im == transparentImage) {
            return;
        }

        int dst = x + y * 160;
        int src = sourceLine * 8;
        int dstEnd = (x + 8 > 160) ? ((y + 1) * 160) : (dst + 8);

        if (x < 0) { // adjust left
            dst -= x;
            src -= x;
        }

        while (dst < dstEnd) {
            if (im[src] < 0 && frameBuffer[dst] >= 0) // fast check for 0xff and 0x00 in high byte
                frameBuffer[dst] = im[src];

            dst++;
            src++;
        }
    }


    private BufferedImage createImage(int width, int height, int[] pixes) {
        BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        int[] target = ((DataBufferInt) bi.getRaster().getDataBuffer()).getData();
        System.arraycopy(pixes, 0, target, 0, target.length);
        return bi;
    }
}
