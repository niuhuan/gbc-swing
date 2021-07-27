/*
 * Copyright (C) 2006-2012 Klaus Reimer <k@ailis.de>
 * Based on hq4x.cpp Copyright (C) 2003 Maxim Stepin <maxst@hiend3d.com>
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package gbcswing.tools.filter;

import java.awt.image.BufferedImage;


/**
 * Java implementation of the hq4x magnification filters. Based on Maxim Stepin's
 * C implementation.
 *
 * @author niuhuan (niuhuancn@outlook.com)
 */

public class Hq4xFilter extends Hq0xFilter
{
    /** Helper array LUT16to32 */
    private static final int[] LUT16TO32 = buildLut16to32();

    /** Helper array RGBtoYUV */
    private static final int[] RGBTOYUV = buildRgbToYuv();

    /** Ymask constant */
    private static final int YMASK = 0x00FF0000;

    /** Umask constant */
    private static final int UMASK = 0x0000FF00;

    /** Vmask constant */
    private static final int VMASK = 0x000000FF;

    /** trY constant */
    private static final int TRY = 0x00300000;

    /** trU constant */
    private static final int TRU = 0x00000700;

    /** trV constant */
    private static final int TRV = 0x00000006;

    /** Internal YUV1 helper variable */
    private int yuv1;

    /** Internal YUV2 helper variable */
    private int yuv2;


    /**
     * Builds and returns the LUT16To32 table.
     *
     * @return The LUT16To32 table
     */

    private static int[] buildLut16to32()
    {
        final int[] lut16to32 = new int[65536];
        for (int i = 0; i < 65536; i++)
        {
            lut16to32[i] = ((i & 0xF800) << 8) + ((i & 0x07E0) << 5)
                + ((i & 0x001F) << 3);
        }
        return lut16to32;
    }


    /**
     * Builds and returns the RgbToYuv table.
     *
     * @return The RgbToYuv table
     */

    private static int[] buildRgbToYuv()
    {
        final int[] rgbToYuv = new int[65536];
        for (int i = 0; i < 32; i++)
        {
            for (int j = 0; j < 64; j++)
            {
                for (int k = 0; k < 32; k++)
                {
                    int r, g, b, y, u, v;

                    r = i << 3;
                    g = j << 2;
                    b = k << 3;
                    y = (r + g + b) >> 2;
                    u = 128 + ((r - b) >> 2);
                    v = 128 + ((-r + 2 * g - b) >> 3);
                    rgbToYuv[(i << 11) + (j << 5) + k] = (y << 16) + (u << 8)
                        + v;
                }
            }
        }
        return rgbToYuv;
    }


    /**
     * @see _ImageFilter#getScaleFactor()
     */

    @Override
    public int getScaleFactor()
    {
        return 4;
    }


    /**
     * @see _ImageFilter#scale(int[], int, int)
     */

    @Override
    public int[] scale(final int[] image, final int width, final int height)
    {
        return scalePixels(convertRgb888To565(image), width, height);
    }


    /**
     * Interpolation method 1.
     *
     * @param pixels
     *            The pixel array
     * @param offset
     *            The offset of the pixel to calculate
     * @param c1 TODO Document me.
     * @param c2 TODO Document me.
     */

    private void interp1(final int[] pixels, final int offset, final int c1, final int c2)
    {
        pixels[offset] = (c1 * 3 + c2) >> 2;
    }


    /**
     * Interpolation method 2.
     *
     * @param pixels
     *            The pixel array
     * @param offset
     *            The offset of the pixel to calculate
     * @param c1 TODO Document me.
     * @param c2 TODO Document me.
     * @param c3 TODO Document me.
     */

    private void interp2(final int[] pixels, final int offset, final int c1, final int c2, final int c3)
    {
        pixels[offset] = (c1 * 2 + c2 + c3) >> 2;
    }


    /**
     * Interpolation method 3.
     *
     * @param pixels
     *            The pixel array
     * @param offset
     *            The offset of the pixel to calculate
     * @param c1 TODO Document me.
     * @param c2 TODO Document me.
     */

    private void interp3(final int[] pixels, final int offset, final int c1, final int c2)
    {
        pixels[offset] = ((((c1 & 0x00FF00) * 7 + (c2 & 0x00FF00)) & 0x0007F800) + (((c1 & 0xFF00FF) * 7 + (c2 & 0xFF00FF)) & 0x07F807F8)) >> 3;
    }


    /**
     * Interpolation method 5.
     *
     * @param pixels
     *            The pixel array
     * @param offset
     *            The offset of the pixel to calculate
     * @param c1 TODO Document me.
     * @param c2 TODO Document me.
     */

    private void interp5(final int[] pixels, final int offset, final int c1, final int c2)
    {
        pixels[offset] = (c1 + c2) >> 1;
    }


    /**
     * Interpolation method 6.
     *
     * @param pixels
     *            The pixel array
     * @param offset
     *            The offset of the pixel to calculate
     * @param c1 TODO Document me.
     * @param c2 TODO Document me.
     * @param c3 TODO Document me.
     */

    private void interp6(final int[] pixels, final int offset, final int c1, final int c2, final int c3)
    {
        pixels[offset] = ((((c1 & 0x00FF00) * 5 + (c2 & 0x00FF00) * 2 + (c3 & 0x00FF00)) & 0x0007F800) + (((c1 & 0xFF00FF)
            * 5 + (c2 & 0xFF00FF) * 2 + (c3 & 0xFF00FF)) & 0x07F807F8)) >> 3;
    }


    /**
     * Interpolation method 7.
     *
     * @param pixels
     *            The pixel array
     * @param offset
     *            The offset of the pixel to calculate
     * @param c1 TODO Document me.
     * @param c2 TODO Document me.
     * @param c3 TODO Document me.
     */

    private void interp7(final int[] pixels, final int offset, final int c1, final int c2, final int c3)
    {
        pixels[offset] = ((((c1 & 0x00FF00) * 6 + (c2 & 0x00FF00) + (c3 & 0x00FF00)) & 0x0007F800) + (((c1 & 0xFF00FF)
            * 6 + (c2 & 0xFF00FF) + (c3 & 0xFF00FF)) & 0x07F807F8)) >> 3;
    }


    /**
     * Interpolation method 8.
     *
     * @param pixels
     *            The pixel array
     * @param offset
     *            The offset of the pixel to calculate
     * @param c1 TODO Document me.
     * @param c2 TODO Document me.
     */

    private void interp8(final int[] pixels, final int offset, final int c1, final int c2)
    {
        pixels[offset] = ((((c1 & 0x00FF00) * 5 + (c2 & 0x00FF00) * 3) & 0x0007F800) + (((c1 & 0xFF00FF) * 5 + (c2 & 0xFF00FF) * 3) & 0x07F807F8)) >> 3;
    }


    /**
     * Diff
     *
     * @param w1 TODO Document me.
     * @param w2 TODO Document me.
     * @return TODO Document me.
     */

    private boolean diff(final int w1, final int w2)
    {
        this.yuv1 = RGBTOYUV[w1];
        this.yuv2 = RGBTOYUV[w2];
        return ((Math.abs((this.yuv1 & YMASK) - (this.yuv2 & YMASK)) > TRY)
            || (Math.abs((this.yuv1 & UMASK) - (this.yuv2 & UMASK)) > TRU) || (Math
            .abs((this.yuv1 & VMASK) - (this.yuv2 & VMASK)) > TRV));
    }


    /**
     * Scaled an RGB 565 pixel data array with the hq3x algorithm and returns
     * the new RGB data.
     *
     * @param pixels
     *            The RGB 565 pixel data of the original picture
     * @param width
     *            The width of the picture
     * @param height
     *            The height of the picture
     * @return The hq3x scaled pixel data
     */

    private int[] scalePixels(final int[] pixels, final int width, final int height)
    {
        int index, newIndex;
        int newWidth, newHeight, newWidth2, newWidth3;
        int prevLine, newLine;
        final int w[] = new int[10];
        final int c[] = new int[10];
        int[] newPixels;
        int newIndexWidth, newIndexWidth2, newIndexWidth3;

        // Calculate new image height
        newWidth = width * 4;
        newHeight = height * 4;
        newWidth2 = newWidth * 2;
        newWidth3 = newWidth * 3;

        // Allocate pixel array for new picture
        newPixels = new int[newWidth * newHeight];

        index = 0;
        newIndex = 0;
        for (int j = 0; j < height; j++)
        {
            if (j > 0)
            {
                prevLine = -width;
            }
            else
            {
                prevLine = 0;
            }
            if (j < height - 1)
            {
                newLine = width;
            }
            else
            {
                newLine = 0;
            }

            for (int i = 0; i < width; i++)
            {
                int pattern, flag;

                w[2] = pixels[index + prevLine];
                w[5] = pixels[index];
                w[8] = pixels[index + newLine];

                if (i > 0)
                {
                    w[1] = pixels[index + prevLine - 1];
                    w[4] = pixels[index - 1];
                    w[7] = pixels[index + newLine - 1];
                }
                else
                {
                    w[1] = w[2];
                    w[4] = w[5];
                    w[7] = w[8];
                }

                if (i < width - 1)
                {
                    w[3] = pixels[index + prevLine + 1];
                    w[6] = pixels[index + 1];
                    w[9] = pixels[index + newLine + 1];
                }
                else
                {
                    w[3] = w[2];
                    w[6] = w[5];
                    w[9] = w[8];
                }

                pattern = 0;
                flag = 1;

                this.yuv1 = RGBTOYUV[w[5]];

                for (int k = 1; k <= 9; k++)
                {
                    if (k == 5) continue;

                    if (w[k] != w[5])
                    {
                        this.yuv2 = RGBTOYUV[w[k]];
                        if ((Math
                            .abs((this.yuv1 & YMASK) - (this.yuv2 & YMASK)) > TRY)
                            || (Math.abs((this.yuv1 & UMASK)
                                - (this.yuv2 & UMASK)) > TRU)
                            || (Math.abs((this.yuv1 & VMASK)
                                - (this.yuv2 & VMASK)) > TRV))
                        {
                            pattern |= flag;
                        }
                    }
                    flag <<= 1;
                }

                for (int k = 1; k <= 9; k++)
                {
                    c[k] = LUT16TO32[w[k]];
                }

                newIndexWidth = newIndex + newWidth;
                newIndexWidth2 = newIndex + newWidth2;
                newIndexWidth3 = newIndex + newWidth3;

                switch (pattern)
                {
                    case 0:
                    case 1:
                    case 4:
                    case 32:
                    case 128:
                    case 5:
                    case 132:
                    case 160:
                    case 33:
                    case 129:
                    case 36:
                    case 133:
                    case 164:
                    case 161:
                    case 37:
                    case 165:
                    {
                        interp2(newPixels, newIndex, c[5], c[2], c[4]);
                        interp6(newPixels, newIndex + 1, c[5], c[2], c[4]);
                        interp6(newPixels, newIndex + 2, c[5], c[2], c[6]);
                        interp2(newPixels, newIndex + 3, c[5], c[2], c[6]);
                        interp6(newPixels, newIndexWidth, c[5], c[4], c[2]);
                        interp7(newPixels, newIndexWidth + 1, c[5], c[4], c[2]);
                        interp7(newPixels, newIndexWidth + 2, c[5], c[6], c[2]);
                        interp6(newPixels, newIndexWidth + 3, c[5], c[6], c[2]);
                        interp6(newPixels, newIndexWidth2, c[5], c[4], c[8]);
                        interp7(newPixels, newIndexWidth2 + 1, c[5], c[4], c[8]);
                        interp7(newPixels, newIndexWidth2 + 2, c[5], c[6], c[8]);
                        interp6(newPixels, newIndexWidth2 + 3, c[5], c[6],
                            c[8]);
                        interp2(newPixels, newIndexWidth3, c[5], c[8], c[4]);
                        interp6(newPixels, newIndexWidth3 + 1, c[5], c[8], c[4]);
                        interp6(newPixels, newIndexWidth3 + 2, c[5], c[8], c[6]);
                        interp2(newPixels, newIndexWidth3 + 3, c[5], c[8],
                            c[6]);
                        break;
                    }
                    case 2:
                    case 34:
                    case 130:
                    case 162:
                    {
                        interp8(newPixels, newIndex, c[5], c[1]);
                        interp1(newPixels, newIndex + 1, c[5], c[1]);
                        interp1(newPixels, newIndex + 2, c[5], c[3]);
                        interp8(newPixels, newIndex + 3, c[5], c[3]);
                        interp6(newPixels, newIndexWidth, c[5], c[4], c[1]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[1]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[3]);
                        interp6(newPixels, newIndexWidth + 3, c[5], c[6], c[3]);
                        interp6(newPixels, newIndexWidth2, c[5], c[4], c[8]);
                        interp7(newPixels, newIndexWidth2 + 1, c[5], c[4], c[8]);
                        interp7(newPixels, newIndexWidth2 + 2, c[5], c[6], c[8]);
                        interp6(newPixels, newIndexWidth2 + 3, c[5], c[6],
                            c[8]);
                        interp2(newPixels, newIndexWidth3, c[5], c[8], c[4]);
                        interp6(newPixels, newIndexWidth3 + 1, c[5], c[8], c[4]);
                        interp6(newPixels, newIndexWidth3 + 2, c[5], c[8], c[6]);
                        interp2(newPixels, newIndexWidth3 + 3, c[5], c[8],
                            c[6]);
                        break;
                    }
                    case 16:
                    case 17:
                    case 48:
                    case 49:
                    {
                        interp2(newPixels, newIndex, c[5], c[2], c[4]);
                        interp6(newPixels, newIndex + 1, c[5], c[2], c[4]);
                        interp6(newPixels, newIndex + 2, c[5], c[2], c[3]);
                        interp8(newPixels, newIndex + 3, c[5], c[3]);
                        interp6(newPixels, newIndexWidth, c[5], c[4], c[2]);
                        interp7(newPixels, newIndexWidth + 1, c[5], c[4], c[2]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[3]);
                        interp1(newPixels, newIndexWidth + 3, c[5], c[3]);
                        interp6(newPixels, newIndexWidth2, c[5], c[4], c[8]);
                        interp7(newPixels, newIndexWidth2 + 1, c[5], c[4], c[8]);
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[9]);
                        interp1(newPixels, newIndexWidth2 + 3, c[5], c[9]);
                        interp2(newPixels, newIndexWidth3, c[5], c[8], c[4]);
                        interp6(newPixels, newIndexWidth3 + 1, c[5], c[8], c[4]);
                        interp6(newPixels, newIndexWidth3 + 2, c[5], c[8], c[9]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[9]);
                        break;
                    }
                    case 64:
                    case 65:
                    case 68:
                    case 69:
                    {
                        interp2(newPixels, newIndex, c[5], c[2], c[4]);
                        interp6(newPixels, newIndex + 1, c[5], c[2], c[4]);
                        interp6(newPixels, newIndex + 2, c[5], c[2], c[6]);
                        interp2(newPixels, newIndex + 3, c[5], c[2], c[6]);
                        interp6(newPixels, newIndexWidth, c[5], c[4], c[2]);
                        interp7(newPixels, newIndexWidth + 1, c[5], c[4], c[2]);
                        interp7(newPixels, newIndexWidth + 2, c[5], c[6], c[2]);
                        interp6(newPixels, newIndexWidth + 3, c[5], c[6], c[2]);
                        interp6(newPixels, newIndexWidth2, c[5], c[4], c[7]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[7]);
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[9]);
                        interp6(newPixels, newIndexWidth2 + 3, c[5], c[6],
                            c[9]);
                        interp8(newPixels, newIndexWidth3, c[5], c[7]);
                        interp1(newPixels, newIndexWidth3 + 1, c[5], c[7]);
                        interp1(newPixels, newIndexWidth3 + 2, c[5], c[9]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[9]);
                        break;
                    }
                    case 8:
                    case 12:
                    case 136:
                    case 140:
                    {
                        interp8(newPixels, newIndex, c[5], c[1]);
                        interp6(newPixels, newIndex + 1, c[5], c[2], c[1]);
                        interp6(newPixels, newIndex + 2, c[5], c[2], c[6]);
                        interp2(newPixels, newIndex + 3, c[5], c[2], c[6]);
                        interp1(newPixels, newIndexWidth, c[5], c[1]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[1]);
                        interp7(newPixels, newIndexWidth + 2, c[5], c[6], c[2]);
                        interp6(newPixels, newIndexWidth + 3, c[5], c[6], c[2]);
                        interp1(newPixels, newIndexWidth2, c[5], c[7]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[7]);
                        interp7(newPixels, newIndexWidth2 + 2, c[5], c[6], c[8]);
                        interp6(newPixels, newIndexWidth2 + 3, c[5], c[6],
                            c[8]);
                        interp8(newPixels, newIndexWidth3, c[5], c[7]);
                        interp6(newPixels, newIndexWidth3 + 1, c[5], c[8], c[7]);
                        interp6(newPixels, newIndexWidth3 + 2, c[5], c[8], c[6]);
                        interp2(newPixels, newIndexWidth3 + 3, c[5], c[8],
                            c[6]);
                        break;
                    }
                    case 3:
                    case 35:
                    case 131:
                    case 163:
                    {
                        interp8(newPixels, newIndex, c[5], c[4]);
                        interp3(newPixels, newIndex + 1, c[5], c[4]);
                        interp1(newPixels, newIndex + 2, c[5], c[3]);
                        interp8(newPixels, newIndex + 3, c[5], c[3]);
                        interp8(newPixels, newIndexWidth, c[5], c[4]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[4]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[3]);
                        interp6(newPixels, newIndexWidth + 3, c[5], c[6], c[3]);
                        interp6(newPixels, newIndexWidth2, c[5], c[4], c[8]);
                        interp7(newPixels, newIndexWidth2 + 1, c[5], c[4], c[8]);
                        interp7(newPixels, newIndexWidth2 + 2, c[5], c[6], c[8]);
                        interp6(newPixels, newIndexWidth2 + 3, c[5], c[6],
                            c[8]);
                        interp2(newPixels, newIndexWidth3, c[5], c[8], c[4]);
                        interp6(newPixels, newIndexWidth3 + 1, c[5], c[8], c[4]);
                        interp6(newPixels, newIndexWidth3 + 2, c[5], c[8], c[6]);
                        interp2(newPixels, newIndexWidth3 + 3, c[5], c[8],
                            c[6]);
                        break;
                    }
                    case 6:
                    case 38:
                    case 134:
                    case 166:
                    {
                        interp8(newPixels, newIndex, c[5], c[1]);
                        interp1(newPixels, newIndex + 1, c[5], c[1]);
                        interp3(newPixels, newIndex + 2, c[5], c[6]);
                        interp8(newPixels, newIndex + 3, c[5], c[6]);
                        interp6(newPixels, newIndexWidth, c[5], c[4], c[1]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[1]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[6]);
                        interp8(newPixels, newIndexWidth + 3, c[5], c[6]);
                        interp6(newPixels, newIndexWidth2, c[5], c[4], c[8]);
                        interp7(newPixels, newIndexWidth2 + 1, c[5], c[4], c[8]);
                        interp7(newPixels, newIndexWidth2 + 2, c[5], c[6], c[8]);
                        interp6(newPixels, newIndexWidth2 + 3, c[5], c[6],
                            c[8]);
                        interp2(newPixels, newIndexWidth3, c[5], c[8], c[4]);
                        interp6(newPixels, newIndexWidth3 + 1, c[5], c[8], c[4]);
                        interp6(newPixels, newIndexWidth3 + 2, c[5], c[8], c[6]);
                        interp2(newPixels, newIndexWidth3 + 3, c[5], c[8],
                            c[6]);
                        break;
                    }
                    case 20:
                    case 21:
                    case 52:
                    case 53:
                    {
                        interp2(newPixels, newIndex, c[5], c[2], c[4]);
                        interp6(newPixels, newIndex + 1, c[5], c[2], c[4]);
                        interp8(newPixels, newIndex + 2, c[5], c[2]);
                        interp8(newPixels, newIndex + 3, c[5], c[2]);
                        interp6(newPixels, newIndexWidth, c[5], c[4], c[2]);
                        interp7(newPixels, newIndexWidth + 1, c[5], c[4], c[2]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[2]);
                        interp3(newPixels, newIndexWidth + 3, c[5], c[2]);
                        interp6(newPixels, newIndexWidth2, c[5], c[4], c[8]);
                        interp7(newPixels, newIndexWidth2 + 1, c[5], c[4], c[8]);
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[9]);
                        interp1(newPixels, newIndexWidth2 + 3, c[5], c[9]);
                        interp2(newPixels, newIndexWidth3, c[5], c[8], c[4]);
                        interp6(newPixels, newIndexWidth3 + 1, c[5], c[8], c[4]);
                        interp6(newPixels, newIndexWidth3 + 2, c[5], c[8], c[9]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[9]);
                        break;
                    }
                    case 144:
                    case 145:
                    case 176:
                    case 177:
                    {
                        interp2(newPixels, newIndex, c[5], c[2], c[4]);
                        interp6(newPixels, newIndex + 1, c[5], c[2], c[4]);
                        interp6(newPixels, newIndex + 2, c[5], c[2], c[3]);
                        interp8(newPixels, newIndex + 3, c[5], c[3]);
                        interp6(newPixels, newIndexWidth, c[5], c[4], c[2]);
                        interp7(newPixels, newIndexWidth + 1, c[5], c[4], c[2]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[3]);
                        interp1(newPixels, newIndexWidth + 3, c[5], c[3]);
                        interp6(newPixels, newIndexWidth2, c[5], c[4], c[8]);
                        interp7(newPixels, newIndexWidth2 + 1, c[5], c[4], c[8]);
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[8]);
                        interp3(newPixels, newIndexWidth2 + 3, c[5], c[8]);
                        interp2(newPixels, newIndexWidth3, c[5], c[8], c[4]);
                        interp6(newPixels, newIndexWidth3 + 1, c[5], c[8], c[4]);
                        interp8(newPixels, newIndexWidth3 + 2, c[5], c[8]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[8]);
                        break;
                    }
                    case 192:
                    case 193:
                    case 196:
                    case 197:
                    {
                        interp2(newPixels, newIndex, c[5], c[2], c[4]);
                        interp6(newPixels, newIndex + 1, c[5], c[2], c[4]);
                        interp6(newPixels, newIndex + 2, c[5], c[2], c[6]);
                        interp2(newPixels, newIndex + 3, c[5], c[2], c[6]);
                        interp6(newPixels, newIndexWidth, c[5], c[4], c[2]);
                        interp7(newPixels, newIndexWidth + 1, c[5], c[4], c[2]);
                        interp7(newPixels, newIndexWidth + 2, c[5], c[6], c[2]);
                        interp6(newPixels, newIndexWidth + 3, c[5], c[6], c[2]);
                        interp6(newPixels, newIndexWidth2, c[5], c[4], c[7]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[7]);
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[6]);
                        interp8(newPixels, newIndexWidth2 + 3, c[5], c[6]);
                        interp8(newPixels, newIndexWidth3, c[5], c[7]);
                        interp1(newPixels, newIndexWidth3 + 1, c[5], c[7]);
                        interp3(newPixels, newIndexWidth3 + 2, c[5], c[6]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[6]);
                        break;
                    }
                    case 96:
                    case 97:
                    case 100:
                    case 101:
                    {
                        interp2(newPixels, newIndex, c[5], c[2], c[4]);
                        interp6(newPixels, newIndex + 1, c[5], c[2], c[4]);
                        interp6(newPixels, newIndex + 2, c[5], c[2], c[6]);
                        interp2(newPixels, newIndex + 3, c[5], c[2], c[6]);
                        interp6(newPixels, newIndexWidth, c[5], c[4], c[2]);
                        interp7(newPixels, newIndexWidth + 1, c[5], c[4], c[2]);
                        interp7(newPixels, newIndexWidth + 2, c[5], c[6], c[2]);
                        interp6(newPixels, newIndexWidth + 3, c[5], c[6], c[2]);
                        interp8(newPixels, newIndexWidth2, c[5], c[4]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[4]);
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[9]);
                        interp6(newPixels, newIndexWidth2 + 3, c[5], c[6],
                            c[9]);
                        interp8(newPixels, newIndexWidth3, c[5], c[4]);
                        interp3(newPixels, newIndexWidth3 + 1, c[5], c[4]);
                        interp1(newPixels, newIndexWidth3 + 2, c[5], c[9]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[9]);
                        break;
                    }
                    case 40:
                    case 44:
                    case 168:
                    case 172:
                    {
                        interp8(newPixels, newIndex, c[5], c[1]);
                        interp6(newPixels, newIndex + 1, c[5], c[2], c[1]);
                        interp6(newPixels, newIndex + 2, c[5], c[2], c[6]);
                        interp2(newPixels, newIndex + 3, c[5], c[2], c[6]);
                        interp1(newPixels, newIndexWidth, c[5], c[1]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[1]);
                        interp7(newPixels, newIndexWidth + 2, c[5], c[6], c[2]);
                        interp6(newPixels, newIndexWidth + 3, c[5], c[6], c[2]);
                        interp3(newPixels, newIndexWidth2, c[5], c[8]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[8]);
                        interp7(newPixels, newIndexWidth2 + 2, c[5], c[6], c[8]);
                        interp6(newPixels, newIndexWidth2 + 3, c[5], c[6],
                            c[8]);
                        interp8(newPixels, newIndexWidth3, c[5], c[8]);
                        interp8(newPixels, newIndexWidth3 + 1, c[5], c[8]);
                        interp6(newPixels, newIndexWidth3 + 2, c[5], c[8], c[6]);
                        interp2(newPixels, newIndexWidth3 + 3, c[5], c[8],
                            c[6]);
                        break;
                    }
                    case 9:
                    case 13:
                    case 137:
                    case 141:
                    {
                        interp8(newPixels, newIndex, c[5], c[2]);
                        interp8(newPixels, newIndex + 1, c[5], c[2]);
                        interp6(newPixels, newIndex + 2, c[5], c[2], c[6]);
                        interp2(newPixels, newIndex + 3, c[5], c[2], c[6]);
                        interp3(newPixels, newIndexWidth, c[5], c[2]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[2]);
                        interp7(newPixels, newIndexWidth + 2, c[5], c[6], c[2]);
                        interp6(newPixels, newIndexWidth + 3, c[5], c[6], c[2]);
                        interp1(newPixels, newIndexWidth2, c[5], c[7]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[7]);
                        interp7(newPixels, newIndexWidth2 + 2, c[5], c[6], c[8]);
                        interp6(newPixels, newIndexWidth2 + 3, c[5], c[6],
                            c[8]);
                        interp8(newPixels, newIndexWidth3, c[5], c[7]);
                        interp6(newPixels, newIndexWidth3 + 1, c[5], c[8], c[7]);
                        interp6(newPixels, newIndexWidth3 + 2, c[5], c[8], c[6]);
                        interp2(newPixels, newIndexWidth3 + 3, c[5], c[8],
                            c[6]);
                        break;
                    }
                    case 18:
                    case 50:
                    {
                        interp8(newPixels, newIndex, c[5], c[1]);
                        interp1(newPixels, newIndex + 1, c[5], c[1]);
                        if (diff(w[2], w[6]))
                        {
                            interp1(newPixels, newIndex + 2, c[5], c[3]);
                            interp8(newPixels, newIndex + 3, c[5], c[3]);
                            interp3(newPixels, newIndexWidth + 2, c[5], c[3]);
                            interp1(newPixels, newIndexWidth + 3, c[5], c[3]);
                        }
                        else
                        {
                            interp5(newPixels, newIndex + 2, c[2], c[5]);
                            interp5(newPixels, newIndex + 3, c[2], c[6]);
                            newPixels[newIndexWidth + 2] = c[5];
                            interp5(newPixels, newIndexWidth + 3, c[6], c[5]);
                        }
                        interp6(newPixels, newIndexWidth, c[5], c[4], c[1]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[1]);
                        interp6(newPixels, newIndexWidth2, c[5], c[4], c[8]);
                        interp7(newPixels, newIndexWidth2 + 1, c[5], c[4], c[8]);
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[9]);
                        interp1(newPixels, newIndexWidth2 + 3, c[5], c[9]);
                        interp2(newPixels, newIndexWidth3, c[5], c[8], c[4]);
                        interp6(newPixels, newIndexWidth3 + 1, c[5], c[8], c[4]);
                        interp6(newPixels, newIndexWidth3 + 2, c[5], c[8], c[9]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[9]);
                        break;
                    }
                    case 80:
                    case 81:
                    {
                        interp2(newPixels, newIndex, c[5], c[2], c[4]);
                        interp6(newPixels, newIndex + 1, c[5], c[2], c[4]);
                        interp6(newPixels, newIndex + 2, c[5], c[2], c[3]);
                        interp8(newPixels, newIndex + 3, c[5], c[3]);
                        interp6(newPixels, newIndexWidth, c[5], c[4], c[2]);
                        interp7(newPixels, newIndexWidth + 1, c[5], c[4], c[2]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[3]);
                        interp1(newPixels, newIndexWidth + 3, c[5], c[3]);
                        interp6(newPixels, newIndexWidth2, c[5], c[4], c[7]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[7]);
                        if (diff(w[6], w[8]))
                        {
                            interp3(newPixels, newIndexWidth2 + 2, c[5], c[9]);
                            interp1(newPixels, newIndexWidth2 + 3, c[5], c[9]);
                            interp1(newPixels, newIndexWidth3 + 2, c[5], c[9]);
                            interp8(newPixels, newIndexWidth3 + 3, c[5], c[9]);
                        }
                        else
                        {
                            newPixels[newIndexWidth2 + 2] = c[5];
                            interp5(newPixels, newIndexWidth2 + 3, c[6], c[5]);
                            interp5(newPixels, newIndexWidth3 + 2, c[8], c[5]);
                            interp5(newPixels, newIndexWidth3 + 3, c[8], c[6]);
                        }
                        interp8(newPixels, newIndexWidth3, c[5], c[7]);
                        interp1(newPixels, newIndexWidth3 + 1, c[5], c[7]);
                        break;
                    }
                    case 72:
                    case 76:
                    {
                        interp8(newPixels, newIndex, c[5], c[1]);
                        interp6(newPixels, newIndex + 1, c[5], c[2], c[1]);
                        interp6(newPixels, newIndex + 2, c[5], c[2], c[6]);
                        interp2(newPixels, newIndex + 3, c[5], c[2], c[6]);
                        interp1(newPixels, newIndexWidth, c[5], c[1]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[1]);
                        interp7(newPixels, newIndexWidth + 2, c[5], c[6], c[2]);
                        interp6(newPixels, newIndexWidth + 3, c[5], c[6], c[2]);
                        if (diff(w[8], w[4]))
                        {
                            interp1(newPixels, newIndexWidth2, c[5], c[7]);
                            interp3(newPixels, newIndexWidth2 + 1, c[5], c[7]);
                            interp8(newPixels, newIndexWidth3, c[5], c[7]);
                            interp1(newPixels, newIndexWidth3 + 1, c[5], c[7]);
                        }
                        else
                        {
                            interp5(newPixels, newIndexWidth2, c[4], c[5]);
                            newPixels[newIndexWidth2 + 1] = c[5];
                            interp5(newPixels, newIndexWidth3, c[8], c[4]);
                            interp5(newPixels, newIndexWidth3 + 1, c[8], c[5]);
                        }
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[9]);
                        interp6(newPixels, newIndexWidth2 + 3, c[5], c[6],
                            c[9]);
                        interp1(newPixels, newIndexWidth3 + 2, c[5], c[9]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[9]);
                        break;
                    }
                    case 10:
                    case 138:
                    {
                        if (diff(w[4], w[2]))
                        {
                            interp8(newPixels, newIndex, c[5], c[1]);
                            interp1(newPixels, newIndex + 1, c[5], c[1]);
                            interp1(newPixels, newIndexWidth, c[5], c[1]);
                            interp3(newPixels, newIndexWidth + 1, c[5], c[1]);
                        }
                        else
                        {
                            interp5(newPixels, newIndex, c[2], c[4]);
                            interp5(newPixels, newIndex + 1, c[2], c[5]);
                            interp5(newPixels, newIndexWidth, c[4], c[5]);
                            newPixels[newIndexWidth + 1] = c[5];
                        }
                        interp1(newPixels, newIndex + 2, c[5], c[3]);
                        interp8(newPixels, newIndex + 3, c[5], c[3]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[3]);
                        interp6(newPixels, newIndexWidth + 3, c[5], c[6], c[3]);
                        interp1(newPixels, newIndexWidth2, c[5], c[7]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[7]);
                        interp7(newPixels, newIndexWidth2 + 2, c[5], c[6], c[8]);
                        interp6(newPixels, newIndexWidth2 + 3, c[5], c[6],
                            c[8]);
                        interp8(newPixels, newIndexWidth3, c[5], c[7]);
                        interp6(newPixels, newIndexWidth3 + 1, c[5], c[8], c[7]);
                        interp6(newPixels, newIndexWidth3 + 2, c[5], c[8], c[6]);
                        interp2(newPixels, newIndexWidth3 + 3, c[5], c[8],
                            c[6]);
                        break;
                    }
                    case 66:
                    {
                        interp8(newPixels, newIndex, c[5], c[1]);
                        interp1(newPixels, newIndex + 1, c[5], c[1]);
                        interp1(newPixels, newIndex + 2, c[5], c[3]);
                        interp8(newPixels, newIndex + 3, c[5], c[3]);
                        interp6(newPixels, newIndexWidth, c[5], c[4], c[1]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[1]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[3]);
                        interp6(newPixels, newIndexWidth + 3, c[5], c[6], c[3]);
                        interp6(newPixels, newIndexWidth2, c[5], c[4], c[7]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[7]);
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[9]);
                        interp6(newPixels, newIndexWidth2 + 3, c[5], c[6],
                            c[9]);
                        interp8(newPixels, newIndexWidth3, c[5], c[7]);
                        interp1(newPixels, newIndexWidth3 + 1, c[5], c[7]);
                        interp1(newPixels, newIndexWidth3 + 2, c[5], c[9]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[9]);
                        break;
                    }
                    case 24:
                    {
                        interp8(newPixels, newIndex, c[5], c[1]);
                        interp6(newPixels, newIndex + 1, c[5], c[2], c[1]);
                        interp6(newPixels, newIndex + 2, c[5], c[2], c[3]);
                        interp8(newPixels, newIndex + 3, c[5], c[3]);
                        interp1(newPixels, newIndexWidth, c[5], c[1]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[1]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[3]);
                        interp1(newPixels, newIndexWidth + 3, c[5], c[3]);
                        interp1(newPixels, newIndexWidth2, c[5], c[7]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[7]);
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[9]);
                        interp1(newPixels, newIndexWidth2 + 3, c[5], c[9]);
                        interp8(newPixels, newIndexWidth3, c[5], c[7]);
                        interp6(newPixels, newIndexWidth3 + 1, c[5], c[8], c[7]);
                        interp6(newPixels, newIndexWidth3 + 2, c[5], c[8], c[9]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[9]);
                        break;
                    }
                    case 7:
                    case 39:
                    case 135:
                    {
                        interp8(newPixels, newIndex, c[5], c[4]);
                        interp3(newPixels, newIndex + 1, c[5], c[4]);
                        interp3(newPixels, newIndex + 2, c[5], c[6]);
                        interp8(newPixels, newIndex + 3, c[5], c[6]);
                        interp8(newPixels, newIndexWidth, c[5], c[4]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[4]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[6]);
                        interp8(newPixels, newIndexWidth + 3, c[5], c[6]);
                        interp6(newPixels, newIndexWidth2, c[5], c[4], c[8]);
                        interp7(newPixels, newIndexWidth2 + 1, c[5], c[4], c[8]);
                        interp7(newPixels, newIndexWidth2 + 2, c[5], c[6], c[8]);
                        interp6(newPixels, newIndexWidth2 + 3, c[5], c[6],
                            c[8]);
                        interp2(newPixels, newIndexWidth3, c[5], c[8], c[4]);
                        interp6(newPixels, newIndexWidth3 + 1, c[5], c[8], c[4]);
                        interp6(newPixels, newIndexWidth3 + 2, c[5], c[8], c[6]);
                        interp2(newPixels, newIndexWidth3 + 3, c[5], c[8],
                            c[6]);
                        break;
                    }
                    case 148:
                    case 149:
                    case 180:
                    {
                        interp2(newPixels, newIndex, c[5], c[2], c[4]);
                        interp6(newPixels, newIndex + 1, c[5], c[2], c[4]);
                        interp8(newPixels, newIndex + 2, c[5], c[2]);
                        interp8(newPixels, newIndex + 3, c[5], c[2]);
                        interp6(newPixels, newIndexWidth, c[5], c[4], c[2]);
                        interp7(newPixels, newIndexWidth + 1, c[5], c[4], c[2]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[2]);
                        interp3(newPixels, newIndexWidth + 3, c[5], c[2]);
                        interp6(newPixels, newIndexWidth2, c[5], c[4], c[8]);
                        interp7(newPixels, newIndexWidth2 + 1, c[5], c[4], c[8]);
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[8]);
                        interp3(newPixels, newIndexWidth2 + 3, c[5], c[8]);
                        interp2(newPixels, newIndexWidth3, c[5], c[8], c[4]);
                        interp6(newPixels, newIndexWidth3 + 1, c[5], c[8], c[4]);
                        interp8(newPixels, newIndexWidth3 + 2, c[5], c[8]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[8]);
                        break;
                    }
                    case 224:
                    case 228:
                    case 225:
                    {
                        interp2(newPixels, newIndex, c[5], c[2], c[4]);
                        interp6(newPixels, newIndex + 1, c[5], c[2], c[4]);
                        interp6(newPixels, newIndex + 2, c[5], c[2], c[6]);
                        interp2(newPixels, newIndex + 3, c[5], c[2], c[6]);
                        interp6(newPixels, newIndexWidth, c[5], c[4], c[2]);
                        interp7(newPixels, newIndexWidth + 1, c[5], c[4], c[2]);
                        interp7(newPixels, newIndexWidth + 2, c[5], c[6], c[2]);
                        interp6(newPixels, newIndexWidth + 3, c[5], c[6], c[2]);
                        interp8(newPixels, newIndexWidth2, c[5], c[4]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[4]);
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[6]);
                        interp8(newPixels, newIndexWidth2 + 3, c[5], c[6]);
                        interp8(newPixels, newIndexWidth3, c[5], c[4]);
                        interp3(newPixels, newIndexWidth3 + 1, c[5], c[4]);
                        interp3(newPixels, newIndexWidth3 + 2, c[5], c[6]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[6]);
                        break;
                    }
                    case 41:
                    case 169:
                    case 45:
                    {
                        interp8(newPixels, newIndex, c[5], c[2]);
                        interp8(newPixels, newIndex + 1, c[5], c[2]);
                        interp6(newPixels, newIndex + 2, c[5], c[2], c[6]);
                        interp2(newPixels, newIndex + 3, c[5], c[2], c[6]);
                        interp3(newPixels, newIndexWidth, c[5], c[2]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[2]);
                        interp7(newPixels, newIndexWidth + 2, c[5], c[6], c[2]);
                        interp6(newPixels, newIndexWidth + 3, c[5], c[6], c[2]);
                        interp3(newPixels, newIndexWidth2, c[5], c[8]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[8]);
                        interp7(newPixels, newIndexWidth2 + 2, c[5], c[6], c[8]);
                        interp6(newPixels, newIndexWidth2 + 3, c[5], c[6],
                            c[8]);
                        interp8(newPixels, newIndexWidth3, c[5], c[8]);
                        interp8(newPixels, newIndexWidth3 + 1, c[5], c[8]);
                        interp6(newPixels, newIndexWidth3 + 2, c[5], c[8], c[6]);
                        interp2(newPixels, newIndexWidth3 + 3, c[5], c[8],
                            c[6]);
                        break;
                    }
                    case 22:
                    case 54:
                    {
                        interp8(newPixels, newIndex, c[5], c[1]);
                        interp1(newPixels, newIndex + 1, c[5], c[1]);
                        if (diff(w[2], w[6]))
                        {
                            newPixels[newIndex + 2] = c[5];
                            newPixels[newIndex + 3] = c[5];
                            newPixels[newIndexWidth + 3] = c[5];
                        }
                        else
                        {
                            interp5(newPixels, newIndex + 2, c[2], c[5]);
                            interp5(newPixels, newIndex + 3, c[2], c[6]);
                            interp5(newPixels, newIndexWidth + 3, c[6], c[5]);
                        }
                        interp6(newPixels, newIndexWidth, c[5], c[4], c[1]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[1]);
                        newPixels[newIndexWidth + 2] = c[5];
                        interp6(newPixels, newIndexWidth2, c[5], c[4], c[8]);
                        interp7(newPixels, newIndexWidth2 + 1, c[5], c[4], c[8]);
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[9]);
                        interp1(newPixels, newIndexWidth2 + 3, c[5], c[9]);
                        interp2(newPixels, newIndexWidth3, c[5], c[8], c[4]);
                        interp6(newPixels, newIndexWidth3 + 1, c[5], c[8], c[4]);
                        interp6(newPixels, newIndexWidth3 + 2, c[5], c[8], c[9]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[9]);
                        break;
                    }
                    case 208:
                    case 209:
                    {
                        interp2(newPixels, newIndex, c[5], c[2], c[4]);
                        interp6(newPixels, newIndex + 1, c[5], c[2], c[4]);
                        interp6(newPixels, newIndex + 2, c[5], c[2], c[3]);
                        interp8(newPixels, newIndex + 3, c[5], c[3]);
                        interp6(newPixels, newIndexWidth, c[5], c[4], c[2]);
                        interp7(newPixels, newIndexWidth + 1, c[5], c[4], c[2]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[3]);
                        interp1(newPixels, newIndexWidth + 3, c[5], c[3]);
                        interp6(newPixels, newIndexWidth2, c[5], c[4], c[7]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[7]);
                        newPixels[newIndexWidth2 + 2] = c[5];
                        if (diff(w[6], w[8]))
                        {
                            newPixels[newIndexWidth2 + 3] = c[5];
                            newPixels[newIndexWidth3 + 2] = c[5];
                            newPixels[newIndexWidth3 + 3] = c[5];
                        }
                        else
                        {
                            interp5(newPixels, newIndexWidth2 + 3, c[6], c[5]);
                            interp5(newPixels, newIndexWidth3 + 2, c[8], c[5]);
                            interp5(newPixels, newIndexWidth3 + 3, c[8], c[6]);
                        }
                        interp8(newPixels, newIndexWidth3, c[5], c[7]);
                        interp1(newPixels, newIndexWidth3 + 1, c[5], c[7]);
                        break;
                    }
                    case 104:
                    case 108:
                    {
                        interp8(newPixels, newIndex, c[5], c[1]);
                        interp6(newPixels, newIndex + 1, c[5], c[2], c[1]);
                        interp6(newPixels, newIndex + 2, c[5], c[2], c[6]);
                        interp2(newPixels, newIndex + 3, c[5], c[2], c[6]);
                        interp1(newPixels, newIndexWidth, c[5], c[1]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[1]);
                        interp7(newPixels, newIndexWidth + 2, c[5], c[6], c[2]);
                        interp6(newPixels, newIndexWidth + 3, c[5], c[6], c[2]);
                        if (diff(w[8], w[4]))
                        {
                            newPixels[newIndexWidth2] = c[5];
                            newPixels[newIndexWidth3] = c[5];
                            newPixels[newIndexWidth3 + 1] = c[5];
                        }
                        else
                        {
                            interp5(newPixels, newIndexWidth2, c[4], c[5]);
                            interp5(newPixels, newIndexWidth3, c[8], c[4]);
                            interp5(newPixels, newIndexWidth3 + 1, c[8], c[5]);
                        }
                        newPixels[newIndexWidth2 + 1] = c[5];
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[9]);
                        interp6(newPixels, newIndexWidth2 + 3, c[5], c[6],
                            c[9]);
                        interp1(newPixels, newIndexWidth3 + 2, c[5], c[9]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[9]);
                        break;
                    }
                    case 11:
                    case 139:
                    {
                        if (diff(w[4], w[2]))
                        {
                            newPixels[newIndex] = c[5];
                            newPixels[newIndex + 1] = c[5];
                            newPixels[newIndexWidth] = c[5];
                        }
                        else
                        {
                            interp5(newPixels, newIndex, c[2], c[4]);
                            interp5(newPixels, newIndex + 1, c[2], c[5]);
                            interp5(newPixels, newIndexWidth, c[4], c[5]);
                        }
                        interp1(newPixels, newIndex + 2, c[5], c[3]);
                        interp8(newPixels, newIndex + 3, c[5], c[3]);
                        newPixels[newIndexWidth + 1] = c[5];
                        interp3(newPixels, newIndexWidth + 2, c[5], c[3]);
                        interp6(newPixels, newIndexWidth + 3, c[5], c[6], c[3]);
                        interp1(newPixels, newIndexWidth2, c[5], c[7]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[7]);
                        interp7(newPixels, newIndexWidth2 + 2, c[5], c[6], c[8]);
                        interp6(newPixels, newIndexWidth2 + 3, c[5], c[6],
                            c[8]);
                        interp8(newPixels, newIndexWidth3, c[5], c[7]);
                        interp6(newPixels, newIndexWidth3 + 1, c[5], c[8], c[7]);
                        interp6(newPixels, newIndexWidth3 + 2, c[5], c[8], c[6]);
                        interp2(newPixels, newIndexWidth3 + 3, c[5], c[8],
                            c[6]);
                        break;
                    }
                    case 19:
                    case 51:
                    {
                        if (diff(w[2], w[6]))
                        {
                            interp8(newPixels, newIndex, c[5], c[4]);
                            interp3(newPixels, newIndex + 1, c[5], c[4]);
                            interp1(newPixels, newIndex + 2, c[5], c[3]);
                            interp8(newPixels, newIndex + 3, c[5], c[3]);
                            interp3(newPixels, newIndexWidth + 2, c[5], c[3]);
                            interp1(newPixels, newIndexWidth + 3, c[5], c[3]);
                        }
                        else
                        {
                            interp1(newPixels, newIndex, c[5], c[2]);
                            interp1(newPixels, newIndex + 1, c[2], c[5]);
                            interp8(newPixels, newIndex + 2, c[2], c[6]);
                            interp5(newPixels, newIndex + 3, c[2], c[6]);
                            interp7(newPixels, newIndexWidth + 2, c[5], c[6],
                                c[2]);
                            interp2(newPixels, newIndexWidth + 3, c[6], c[5],
                                c[2]);
                        }
                        interp8(newPixels, newIndexWidth, c[5], c[4]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[4]);
                        interp6(newPixels, newIndexWidth2, c[5], c[4], c[8]);
                        interp7(newPixels, newIndexWidth2 + 1, c[5], c[4], c[8]);
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[9]);
                        interp1(newPixels, newIndexWidth2 + 3, c[5], c[9]);
                        interp2(newPixels, newIndexWidth3, c[5], c[8], c[4]);
                        interp6(newPixels, newIndexWidth3 + 1, c[5], c[8], c[4]);
                        interp6(newPixels, newIndexWidth3 + 2, c[5], c[8], c[9]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[9]);
                        break;
                    }
                    case 146:
                    case 178:
                    {
                        interp8(newPixels, newIndex, c[5], c[1]);
                        interp1(newPixels, newIndex + 1, c[5], c[1]);
                        if (diff(w[2], w[6]))
                        {
                            interp1(newPixels, newIndex + 2, c[5], c[3]);
                            interp8(newPixels, newIndex + 3, c[5], c[3]);
                            interp3(newPixels, newIndexWidth + 2, c[5], c[3]);
                            interp1(newPixels, newIndexWidth + 3, c[5], c[3]);
                            interp3(newPixels, newIndexWidth2 + 3, c[5], c[8]);
                            interp8(newPixels, newIndexWidth3 + 3, c[5], c[8]);
                        }
                        else
                        {
                            interp2(newPixels, newIndex + 2, c[2], c[5], c[6]);
                            interp5(newPixels, newIndex + 3, c[2], c[6]);
                            interp7(newPixels, newIndexWidth + 2, c[5], c[6],
                                c[2]);
                            interp8(newPixels, newIndexWidth + 3, c[6], c[2]);
                            interp1(newPixels, newIndexWidth2 + 3, c[6], c[5]);
                            interp1(newPixels, newIndexWidth3 + 3, c[5], c[6]);
                        }
                        interp6(newPixels, newIndexWidth, c[5], c[4], c[1]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[1]);
                        interp6(newPixels, newIndexWidth2, c[5], c[4], c[8]);
                        interp7(newPixels, newIndexWidth2 + 1, c[5], c[4], c[8]);
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[8]);
                        interp2(newPixels, newIndexWidth3, c[5], c[8], c[4]);
                        interp6(newPixels, newIndexWidth3 + 1, c[5], c[8], c[4]);
                        interp8(newPixels, newIndexWidth3 + 2, c[5], c[8]);
                        break;
                    }
                    case 84:
                    case 85:
                    {
                        interp2(newPixels, newIndex, c[5], c[2], c[4]);
                        interp6(newPixels, newIndex + 1, c[5], c[2], c[4]);
                        interp8(newPixels, newIndex + 2, c[5], c[2]);
                        if (diff(w[6], w[8]))
                        {
                            interp8(newPixels, newIndex + 3, c[5], c[2]);
                            interp3(newPixels, newIndexWidth + 3, c[5], c[2]);
                            interp3(newPixels, newIndexWidth2 + 2, c[5], c[9]);
                            interp1(newPixels, newIndexWidth2 + 3, c[5], c[9]);
                            interp1(newPixels, newIndexWidth3 + 2, c[5], c[9]);
                            interp8(newPixels, newIndexWidth3 + 3, c[5], c[9]);
                        }
                        else
                        {
                            interp1(newPixels, newIndex + 3, c[5], c[6]);
                            interp1(newPixels, newIndexWidth + 3, c[6], c[5]);
                            interp7(newPixels, newIndexWidth2 + 2, c[5], c[6],
                                c[8]);
                            interp8(newPixels, newIndexWidth2 + 3, c[6], c[8]);
                            interp2(newPixels, newIndexWidth3 + 2, c[8], c[5],
                                c[6]);
                            interp5(newPixels, newIndexWidth3 + 3, c[8], c[6]);
                        }
                        interp6(newPixels, newIndexWidth, c[5], c[4], c[2]);
                        interp7(newPixels, newIndexWidth + 1, c[5], c[4], c[2]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[2]);
                        interp6(newPixels, newIndexWidth2, c[5], c[4], c[7]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[7]);
                        interp8(newPixels, newIndexWidth3, c[5], c[7]);
                        interp1(newPixels, newIndexWidth3 + 1, c[5], c[7]);
                        break;
                    }
                    case 112:
                    case 113:
                    {
                        interp2(newPixels, newIndex, c[5], c[2], c[4]);
                        interp6(newPixels, newIndex + 1, c[5], c[2], c[4]);
                        interp6(newPixels, newIndex + 2, c[5], c[2], c[3]);
                        interp8(newPixels, newIndex + 3, c[5], c[3]);
                        interp6(newPixels, newIndexWidth, c[5], c[4], c[2]);
                        interp7(newPixels, newIndexWidth + 1, c[5], c[4], c[2]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[3]);
                        interp1(newPixels, newIndexWidth + 3, c[5], c[3]);
                        interp8(newPixels, newIndexWidth2, c[5], c[4]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[4]);
                        if (diff(w[6], w[8]))
                        {
                            interp3(newPixels, newIndexWidth2 + 2, c[5], c[9]);
                            interp1(newPixels, newIndexWidth2 + 3, c[5], c[9]);
                            interp8(newPixels, newIndexWidth3, c[5], c[4]);
                            interp3(newPixels, newIndexWidth3 + 1, c[5], c[4]);
                            interp1(newPixels, newIndexWidth3 + 2, c[5], c[9]);
                            interp8(newPixels, newIndexWidth3 + 3, c[5], c[9]);
                        }
                        else
                        {
                            interp7(newPixels, newIndexWidth2 + 2, c[5], c[6],
                                c[8]);
                            interp2(newPixels, newIndexWidth2 + 3, c[6], c[5],
                                c[8]);
                            interp1(newPixels, newIndexWidth3, c[5], c[8]);
                            interp1(newPixels, newIndexWidth3 + 1, c[8], c[5]);
                            interp8(newPixels, newIndexWidth3 + 2, c[8], c[6]);
                            interp5(newPixels, newIndexWidth3 + 3, c[8], c[6]);
                        }
                        break;
                    }
                    case 200:
                    case 204:
                    {
                        interp8(newPixels, newIndex, c[5], c[1]);
                        interp6(newPixels, newIndex + 1, c[5], c[2], c[1]);
                        interp6(newPixels, newIndex + 2, c[5], c[2], c[6]);
                        interp2(newPixels, newIndex + 3, c[5], c[2], c[6]);
                        interp1(newPixels, newIndexWidth, c[5], c[1]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[1]);
                        interp7(newPixels, newIndexWidth + 2, c[5], c[6], c[2]);
                        interp6(newPixels, newIndexWidth + 3, c[5], c[6], c[2]);
                        if (diff(w[8], w[4]))
                        {
                            interp1(newPixels, newIndexWidth2, c[5], c[7]);
                            interp3(newPixels, newIndexWidth2 + 1, c[5], c[7]);
                            interp8(newPixels, newIndexWidth3, c[5], c[7]);
                            interp1(newPixels, newIndexWidth3 + 1, c[5], c[7]);
                            interp3(newPixels, newIndexWidth3 + 2, c[5], c[6]);
                            interp8(newPixels, newIndexWidth3 + 3, c[5], c[6]);
                        }
                        else
                        {
                            interp2(newPixels, newIndexWidth2, c[4], c[5], c[8]);
                            interp7(newPixels, newIndexWidth2 + 1, c[5], c[4],
                                c[8]);
                            interp5(newPixels, newIndexWidth3, c[8], c[4]);
                            interp8(newPixels, newIndexWidth3 + 1, c[8], c[4]);
                            interp1(newPixels, newIndexWidth3 + 2, c[8], c[5]);
                            interp1(newPixels, newIndexWidth3 + 3, c[5], c[8]);
                        }
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[6]);
                        interp8(newPixels, newIndexWidth2 + 3, c[5], c[6]);
                        break;
                    }
                    case 73:
                    case 77:
                    {
                        if (diff(w[8], w[4]))
                        {
                            interp8(newPixels, newIndex, c[5], c[2]);
                            interp3(newPixels, newIndexWidth, c[5], c[2]);
                            interp1(newPixels, newIndexWidth2, c[5], c[7]);
                            interp3(newPixels, newIndexWidth2 + 1, c[5], c[7]);
                            interp8(newPixels, newIndexWidth3, c[5], c[7]);
                            interp1(newPixels, newIndexWidth3 + 1, c[5], c[7]);
                        }
                        else
                        {
                            interp1(newPixels, newIndex, c[5], c[4]);
                            interp1(newPixels, newIndexWidth, c[4], c[5]);
                            interp8(newPixels, newIndexWidth2, c[4], c[8]);
                            interp7(newPixels, newIndexWidth2 + 1, c[5], c[4],
                                c[8]);
                            interp5(newPixels, newIndexWidth3, c[8], c[4]);
                            interp2(newPixels, newIndexWidth3 + 1, c[8], c[5],
                                c[4]);
                        }
                        interp8(newPixels, newIndex + 1, c[5], c[2]);
                        interp6(newPixels, newIndex + 2, c[5], c[2], c[6]);
                        interp2(newPixels, newIndex + 3, c[5], c[2], c[6]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[2]);
                        interp7(newPixels, newIndexWidth + 2, c[5], c[6], c[2]);
                        interp6(newPixels, newIndexWidth + 3, c[5], c[6], c[2]);
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[9]);
                        interp6(newPixels, newIndexWidth2 + 3, c[5], c[6],
                            c[9]);
                        interp1(newPixels, newIndexWidth3 + 2, c[5], c[9]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[9]);
                        break;
                    }
                    case 42:
                    case 170:
                    {
                        if (diff(w[4], w[2]))
                        {
                            interp8(newPixels, newIndex, c[5], c[1]);
                            interp1(newPixels, newIndex + 1, c[5], c[1]);
                            interp1(newPixels, newIndexWidth, c[5], c[1]);
                            interp3(newPixels, newIndexWidth + 1, c[5], c[1]);
                            interp3(newPixels, newIndexWidth2, c[5], c[8]);
                            interp8(newPixels, newIndexWidth3, c[5], c[8]);
                        }
                        else
                        {
                            interp5(newPixels, newIndex, c[2], c[4]);
                            interp2(newPixels, newIndex + 1, c[2], c[5], c[4]);
                            interp8(newPixels, newIndexWidth, c[4], c[2]);
                            interp7(newPixels, newIndexWidth + 1, c[5], c[4],
                                c[2]);
                            interp1(newPixels, newIndexWidth2, c[4], c[5]);
                            interp1(newPixels, newIndexWidth3, c[5], c[4]);
                        }
                        interp1(newPixels, newIndex + 2, c[5], c[3]);
                        interp8(newPixels, newIndex + 3, c[5], c[3]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[3]);
                        interp6(newPixels, newIndexWidth + 3, c[5], c[6], c[3]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[8]);
                        interp7(newPixels, newIndexWidth2 + 2, c[5], c[6], c[8]);
                        interp6(newPixels, newIndexWidth2 + 3, c[5], c[6],
                            c[8]);
                        interp8(newPixels, newIndexWidth3 + 1, c[5], c[8]);
                        interp6(newPixels, newIndexWidth3 + 2, c[5], c[8], c[6]);
                        interp2(newPixels, newIndexWidth3 + 3, c[5], c[8],
                            c[6]);
                        break;
                    }
                    case 14:
                    case 142:
                    {
                        if (diff(w[4], w[2]))
                        {
                            interp8(newPixels, newIndex, c[5], c[1]);
                            interp1(newPixels, newIndex + 1, c[5], c[1]);
                            interp3(newPixels, newIndex + 2, c[5], c[6]);
                            interp8(newPixels, newIndex + 3, c[5], c[6]);
                            interp1(newPixels, newIndexWidth, c[5], c[1]);
                            interp3(newPixels, newIndexWidth + 1, c[5], c[1]);
                        }
                        else
                        {
                            interp5(newPixels, newIndex, c[2], c[4]);
                            interp8(newPixels, newIndex + 1, c[2], c[4]);
                            interp1(newPixels, newIndex + 2, c[2], c[5]);
                            interp1(newPixels, newIndex + 3, c[5], c[2]);
                            interp2(newPixels, newIndexWidth, c[4], c[5], c[2]);
                            interp7(newPixels, newIndexWidth + 1, c[5], c[4],
                                c[2]);
                        }
                        interp3(newPixels, newIndexWidth + 2, c[5], c[6]);
                        interp8(newPixels, newIndexWidth + 3, c[5], c[6]);
                        interp1(newPixels, newIndexWidth2, c[5], c[7]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[7]);
                        interp7(newPixels, newIndexWidth2 + 2, c[5], c[6], c[8]);
                        interp6(newPixels, newIndexWidth2 + 3, c[5], c[6],
                            c[8]);
                        interp8(newPixels, newIndexWidth3, c[5], c[7]);
                        interp6(newPixels, newIndexWidth3 + 1, c[5], c[8], c[7]);
                        interp6(newPixels, newIndexWidth3 + 2, c[5], c[8], c[6]);
                        interp2(newPixels, newIndexWidth3 + 3, c[5], c[8],
                            c[6]);
                        break;
                    }
                    case 67:
                    {
                        interp8(newPixels, newIndex, c[5], c[4]);
                        interp3(newPixels, newIndex + 1, c[5], c[4]);
                        interp1(newPixels, newIndex + 2, c[5], c[3]);
                        interp8(newPixels, newIndex + 3, c[5], c[3]);
                        interp8(newPixels, newIndexWidth, c[5], c[4]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[4]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[3]);
                        interp6(newPixels, newIndexWidth + 3, c[5], c[6], c[3]);
                        interp6(newPixels, newIndexWidth2, c[5], c[4], c[7]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[7]);
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[9]);
                        interp6(newPixels, newIndexWidth2 + 3, c[5], c[6],
                            c[9]);
                        interp8(newPixels, newIndexWidth3, c[5], c[7]);
                        interp1(newPixels, newIndexWidth3 + 1, c[5], c[7]);
                        interp1(newPixels, newIndexWidth3 + 2, c[5], c[9]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[9]);
                        break;
                    }
                    case 70:
                    {
                        interp8(newPixels, newIndex, c[5], c[1]);
                        interp1(newPixels, newIndex + 1, c[5], c[1]);
                        interp3(newPixels, newIndex + 2, c[5], c[6]);
                        interp8(newPixels, newIndex + 3, c[5], c[6]);
                        interp6(newPixels, newIndexWidth, c[5], c[4], c[1]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[1]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[6]);
                        interp8(newPixels, newIndexWidth + 3, c[5], c[6]);
                        interp6(newPixels, newIndexWidth2, c[5], c[4], c[7]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[7]);
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[9]);
                        interp6(newPixels, newIndexWidth2 + 3, c[5], c[6],
                            c[9]);
                        interp8(newPixels, newIndexWidth3, c[5], c[7]);
                        interp1(newPixels, newIndexWidth3 + 1, c[5], c[7]);
                        interp1(newPixels, newIndexWidth3 + 2, c[5], c[9]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[9]);
                        break;
                    }
                    case 28:
                    {
                        interp8(newPixels, newIndex, c[5], c[1]);
                        interp6(newPixels, newIndex + 1, c[5], c[2], c[1]);
                        interp8(newPixels, newIndex + 2, c[5], c[2]);
                        interp8(newPixels, newIndex + 3, c[5], c[2]);
                        interp1(newPixels, newIndexWidth, c[5], c[1]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[1]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[2]);
                        interp3(newPixels, newIndexWidth + 3, c[5], c[2]);
                        interp1(newPixels, newIndexWidth2, c[5], c[7]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[7]);
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[9]);
                        interp1(newPixels, newIndexWidth2 + 3, c[5], c[9]);
                        interp8(newPixels, newIndexWidth3, c[5], c[7]);
                        interp6(newPixels, newIndexWidth3 + 1, c[5], c[8], c[7]);
                        interp6(newPixels, newIndexWidth3 + 2, c[5], c[8], c[9]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[9]);
                        break;
                    }
                    case 152:
                    {
                        interp8(newPixels, newIndex, c[5], c[1]);
                        interp6(newPixels, newIndex + 1, c[5], c[2], c[1]);
                        interp6(newPixels, newIndex + 2, c[5], c[2], c[3]);
                        interp8(newPixels, newIndex + 3, c[5], c[3]);
                        interp1(newPixels, newIndexWidth, c[5], c[1]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[1]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[3]);
                        interp1(newPixels, newIndexWidth + 3, c[5], c[3]);
                        interp1(newPixels, newIndexWidth2, c[5], c[7]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[7]);
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[8]);
                        interp3(newPixels, newIndexWidth2 + 3, c[5], c[8]);
                        interp8(newPixels, newIndexWidth3, c[5], c[7]);
                        interp6(newPixels, newIndexWidth3 + 1, c[5], c[8], c[7]);
                        interp8(newPixels, newIndexWidth3 + 2, c[5], c[8]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[8]);
                        break;
                    }
                    case 194:
                    {
                        interp8(newPixels, newIndex, c[5], c[1]);
                        interp1(newPixels, newIndex + 1, c[5], c[1]);
                        interp1(newPixels, newIndex + 2, c[5], c[3]);
                        interp8(newPixels, newIndex + 3, c[5], c[3]);
                        interp6(newPixels, newIndexWidth, c[5], c[4], c[1]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[1]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[3]);
                        interp6(newPixels, newIndexWidth + 3, c[5], c[6], c[3]);
                        interp6(newPixels, newIndexWidth2, c[5], c[4], c[7]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[7]);
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[6]);
                        interp8(newPixels, newIndexWidth2 + 3, c[5], c[6]);
                        interp8(newPixels, newIndexWidth3, c[5], c[7]);
                        interp1(newPixels, newIndexWidth3 + 1, c[5], c[7]);
                        interp3(newPixels, newIndexWidth3 + 2, c[5], c[6]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[6]);
                        break;
                    }
                    case 98:
                    {
                        interp8(newPixels, newIndex, c[5], c[1]);
                        interp1(newPixels, newIndex + 1, c[5], c[1]);
                        interp1(newPixels, newIndex + 2, c[5], c[3]);
                        interp8(newPixels, newIndex + 3, c[5], c[3]);
                        interp6(newPixels, newIndexWidth, c[5], c[4], c[1]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[1]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[3]);
                        interp6(newPixels, newIndexWidth + 3, c[5], c[6], c[3]);
                        interp8(newPixels, newIndexWidth2, c[5], c[4]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[4]);
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[9]);
                        interp6(newPixels, newIndexWidth2 + 3, c[5], c[6],
                            c[9]);
                        interp8(newPixels, newIndexWidth3, c[5], c[4]);
                        interp3(newPixels, newIndexWidth3 + 1, c[5], c[4]);
                        interp1(newPixels, newIndexWidth3 + 2, c[5], c[9]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[9]);
                        break;
                    }
                    case 56:
                    {
                        interp8(newPixels, newIndex, c[5], c[1]);
                        interp6(newPixels, newIndex + 1, c[5], c[2], c[1]);
                        interp6(newPixels, newIndex + 2, c[5], c[2], c[3]);
                        interp8(newPixels, newIndex + 3, c[5], c[3]);
                        interp1(newPixels, newIndexWidth, c[5], c[1]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[1]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[3]);
                        interp1(newPixels, newIndexWidth + 3, c[5], c[3]);
                        interp3(newPixels, newIndexWidth2, c[5], c[8]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[8]);
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[9]);
                        interp1(newPixels, newIndexWidth2 + 3, c[5], c[9]);
                        interp8(newPixels, newIndexWidth3, c[5], c[8]);
                        interp8(newPixels, newIndexWidth3 + 1, c[5], c[8]);
                        interp6(newPixels, newIndexWidth3 + 2, c[5], c[8], c[9]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[9]);
                        break;
                    }
                    case 25:
                    {
                        interp8(newPixels, newIndex, c[5], c[2]);
                        interp8(newPixels, newIndex + 1, c[5], c[2]);
                        interp6(newPixels, newIndex + 2, c[5], c[2], c[3]);
                        interp8(newPixels, newIndex + 3, c[5], c[3]);
                        interp3(newPixels, newIndexWidth, c[5], c[2]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[2]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[3]);
                        interp1(newPixels, newIndexWidth + 3, c[5], c[3]);
                        interp1(newPixels, newIndexWidth2, c[5], c[7]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[7]);
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[9]);
                        interp1(newPixels, newIndexWidth2 + 3, c[5], c[9]);
                        interp8(newPixels, newIndexWidth3, c[5], c[7]);
                        interp6(newPixels, newIndexWidth3 + 1, c[5], c[8], c[7]);
                        interp6(newPixels, newIndexWidth3 + 2, c[5], c[8], c[9]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[9]);
                        break;
                    }
                    case 26:
                    case 31:
                    {
                        if (diff(w[4], w[2]))
                        {
                            newPixels[newIndex] = c[5];
                            newPixels[newIndex + 1] = c[5];
                            newPixels[newIndexWidth] = c[5];
                        }
                        else
                        {
                            interp5(newPixels, newIndex, c[2], c[4]);
                            interp5(newPixels, newIndex + 1, c[2], c[5]);
                            interp5(newPixels, newIndexWidth, c[4], c[5]);
                        }
                        if (diff(w[2], w[6]))
                        {
                            newPixels[newIndex + 2] = c[5];
                            newPixels[newIndex + 3] = c[5];
                            newPixels[newIndexWidth + 3] = c[5];
                        }
                        else
                        {
                            interp5(newPixels, newIndex + 2, c[2], c[5]);
                            interp5(newPixels, newIndex + 3, c[2], c[6]);
                            interp5(newPixels, newIndexWidth + 3, c[6], c[5]);
                        }
                        newPixels[newIndexWidth + 1] = c[5];
                        newPixels[newIndexWidth + 2] = c[5];
                        interp1(newPixels, newIndexWidth2, c[5], c[7]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[7]);
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[9]);
                        interp1(newPixels, newIndexWidth2 + 3, c[5], c[9]);
                        interp8(newPixels, newIndexWidth3, c[5], c[7]);
                        interp6(newPixels, newIndexWidth3 + 1, c[5], c[8], c[7]);
                        interp6(newPixels, newIndexWidth3 + 2, c[5], c[8], c[9]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[9]);
                        break;
                    }
                    case 82:
                    case 214:
                    {
                        interp8(newPixels, newIndex, c[5], c[1]);
                        interp1(newPixels, newIndex + 1, c[5], c[1]);
                        if (diff(w[2], w[6]))
                        {
                            newPixels[newIndex + 2] = c[5];
                            newPixels[newIndex + 3] = c[5];
                            newPixels[newIndexWidth + 3] = c[5];
                        }
                        else
                        {
                            interp5(newPixels, newIndex + 2, c[2], c[5]);
                            interp5(newPixels, newIndex + 3, c[2], c[6]);
                            interp5(newPixels, newIndexWidth + 3, c[6], c[5]);
                        }
                        interp6(newPixels, newIndexWidth, c[5], c[4], c[1]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[1]);
                        newPixels[newIndexWidth + 2] = c[5];
                        interp6(newPixels, newIndexWidth2, c[5], c[4], c[7]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[7]);
                        newPixels[newIndexWidth2 + 2] = c[5];
                        if (diff(w[6], w[8]))
                        {
                            newPixels[newIndexWidth2 + 3] = c[5];
                            newPixels[newIndexWidth3 + 2] = c[5];
                            newPixels[newIndexWidth3 + 3] = c[5];
                        }
                        else
                        {
                            interp5(newPixels, newIndexWidth2 + 3, c[6], c[5]);
                            interp5(newPixels, newIndexWidth3 + 2, c[8], c[5]);
                            interp5(newPixels, newIndexWidth3 + 3, c[8], c[6]);
                        }
                        interp8(newPixels, newIndexWidth3, c[5], c[7]);
                        interp1(newPixels, newIndexWidth3 + 1, c[5], c[7]);
                        break;
                    }
                    case 88:
                    case 248:
                    {
                        interp8(newPixels, newIndex, c[5], c[1]);
                        interp6(newPixels, newIndex + 1, c[5], c[2], c[1]);
                        interp6(newPixels, newIndex + 2, c[5], c[2], c[3]);
                        interp8(newPixels, newIndex + 3, c[5], c[3]);
                        interp1(newPixels, newIndexWidth, c[5], c[1]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[1]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[3]);
                        interp1(newPixels, newIndexWidth + 3, c[5], c[3]);
                        if (diff(w[8], w[4]))
                        {
                            newPixels[newIndexWidth2] = c[5];
                            newPixels[newIndexWidth3] = c[5];
                            newPixels[newIndexWidth3 + 1] = c[5];
                        }
                        else
                        {
                            interp5(newPixels, newIndexWidth2, c[4], c[5]);
                            interp5(newPixels, newIndexWidth3, c[8], c[4]);
                            interp5(newPixels, newIndexWidth3 + 1, c[8], c[5]);
                        }
                        newPixels[newIndexWidth2 + 1] = c[5];
                        newPixels[newIndexWidth2 + 2] = c[5];
                        if (diff(w[6], w[8]))
                        {
                            newPixels[newIndexWidth2 + 3] = c[5];
                            newPixels[newIndexWidth3 + 2] = c[5];
                            newPixels[newIndexWidth3 + 3] = c[5];
                        }
                        else
                        {
                            interp5(newPixels, newIndexWidth2 + 3, c[6], c[5]);
                            interp5(newPixels, newIndexWidth3 + 2, c[8], c[5]);
                            interp5(newPixels, newIndexWidth3 + 3, c[8], c[6]);
                        }
                        break;
                    }
                    case 74:
                    case 107:
                    {
                        if (diff(w[4], w[2]))
                        {
                            newPixels[newIndex] = c[5];
                            newPixels[newIndex + 1] = c[5];
                            newPixels[newIndexWidth] = c[5];
                        }
                        else
                        {
                            interp5(newPixels, newIndex, c[2], c[4]);
                            interp5(newPixels, newIndex + 1, c[2], c[5]);
                            interp5(newPixels, newIndexWidth, c[4], c[5]);
                        }
                        interp1(newPixels, newIndex + 2, c[5], c[3]);
                        interp8(newPixels, newIndex + 3, c[5], c[3]);
                        newPixels[newIndexWidth + 1] = c[5];
                        interp3(newPixels, newIndexWidth + 2, c[5], c[3]);
                        interp6(newPixels, newIndexWidth + 3, c[5], c[6], c[3]);
                        if (diff(w[8], w[4]))
                        {
                            newPixels[newIndexWidth2] = c[5];
                            newPixels[newIndexWidth3] = c[5];
                            newPixels[newIndexWidth3 + 1] = c[5];
                        }
                        else
                        {
                            interp5(newPixels, newIndexWidth2, c[4], c[5]);
                            interp5(newPixels, newIndexWidth3, c[8], c[4]);
                            interp5(newPixels, newIndexWidth3 + 1, c[8], c[5]);
                        }
                        newPixels[newIndexWidth2 + 1] = c[5];
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[9]);
                        interp6(newPixels, newIndexWidth2 + 3, c[5], c[6],
                            c[9]);
                        interp1(newPixels, newIndexWidth3 + 2, c[5], c[9]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[9]);
                        break;
                    }
                    case 27:
                    {
                        if (diff(w[4], w[2]))
                        {
                            newPixels[newIndex] = c[5];
                            newPixels[newIndex + 1] = c[5];
                            newPixels[newIndexWidth] = c[5];
                        }
                        else
                        {
                            interp5(newPixels, newIndex, c[2], c[4]);
                            interp5(newPixels, newIndex + 1, c[2], c[5]);
                            interp5(newPixels, newIndexWidth, c[4], c[5]);
                        }
                        interp1(newPixels, newIndex + 2, c[5], c[3]);
                        interp8(newPixels, newIndex + 3, c[5], c[3]);
                        newPixels[newIndexWidth + 1] = c[5];
                        interp3(newPixels, newIndexWidth + 2, c[5], c[3]);
                        interp1(newPixels, newIndexWidth + 3, c[5], c[3]);
                        interp1(newPixels, newIndexWidth2, c[5], c[7]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[7]);
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[9]);
                        interp1(newPixels, newIndexWidth2 + 3, c[5], c[9]);
                        interp8(newPixels, newIndexWidth3, c[5], c[7]);
                        interp6(newPixels, newIndexWidth3 + 1, c[5], c[8], c[7]);
                        interp6(newPixels, newIndexWidth3 + 2, c[5], c[8], c[9]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[9]);
                        break;
                    }
                    case 86:
                    {
                        interp8(newPixels, newIndex, c[5], c[1]);
                        interp1(newPixels, newIndex + 1, c[5], c[1]);
                        if (diff(w[2], w[6]))
                        {
                            newPixels[newIndex + 2] = c[5];
                            newPixels[newIndex + 3] = c[5];
                            newPixels[newIndexWidth + 3] = c[5];
                        }
                        else
                        {
                            interp5(newPixels, newIndex + 2, c[2], c[5]);
                            interp5(newPixels, newIndex + 3, c[2], c[6]);
                            interp5(newPixels, newIndexWidth + 3, c[6], c[5]);
                        }
                        interp6(newPixels, newIndexWidth, c[5], c[4], c[1]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[1]);
                        newPixels[newIndexWidth + 2] = c[5];
                        interp6(newPixels, newIndexWidth2, c[5], c[4], c[7]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[7]);
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[9]);
                        interp1(newPixels, newIndexWidth2 + 3, c[5], c[9]);
                        interp8(newPixels, newIndexWidth3, c[5], c[7]);
                        interp1(newPixels, newIndexWidth3 + 1, c[5], c[7]);
                        interp1(newPixels, newIndexWidth3 + 2, c[5], c[9]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[9]);
                        break;
                    }
                    case 216:
                    {
                        interp8(newPixels, newIndex, c[5], c[1]);
                        interp6(newPixels, newIndex + 1, c[5], c[2], c[1]);
                        interp6(newPixels, newIndex + 2, c[5], c[2], c[3]);
                        interp8(newPixels, newIndex + 3, c[5], c[3]);
                        interp1(newPixels, newIndexWidth, c[5], c[1]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[1]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[3]);
                        interp1(newPixels, newIndexWidth + 3, c[5], c[3]);
                        interp1(newPixels, newIndexWidth2, c[5], c[7]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[7]);
                        newPixels[newIndexWidth2 + 2] = c[5];
                        if (diff(w[6], w[8]))
                        {
                            newPixels[newIndexWidth2 + 3] = c[5];
                            newPixels[newIndexWidth3 + 2] = c[5];
                            newPixels[newIndexWidth3 + 3] = c[5];
                        }
                        else
                        {
                            interp5(newPixels, newIndexWidth2 + 3, c[6], c[5]);
                            interp5(newPixels, newIndexWidth3 + 2, c[8], c[5]);
                            interp5(newPixels, newIndexWidth3 + 3, c[8], c[6]);
                        }
                        interp8(newPixels, newIndexWidth3, c[5], c[7]);
                        interp1(newPixels, newIndexWidth3 + 1, c[5], c[7]);
                        break;
                    }
                    case 106:
                    {
                        interp8(newPixels, newIndex, c[5], c[1]);
                        interp1(newPixels, newIndex + 1, c[5], c[1]);
                        interp1(newPixels, newIndex + 2, c[5], c[3]);
                        interp8(newPixels, newIndex + 3, c[5], c[3]);
                        interp1(newPixels, newIndexWidth, c[5], c[1]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[1]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[3]);
                        interp6(newPixels, newIndexWidth + 3, c[5], c[6], c[3]);
                        if (diff(w[8], w[4]))
                        {
                            newPixels[newIndexWidth2] = c[5];
                            newPixels[newIndexWidth3] = c[5];
                            newPixels[newIndexWidth3 + 1] = c[5];
                        }
                        else
                        {
                            interp5(newPixels, newIndexWidth2, c[4], c[5]);
                            interp5(newPixels, newIndexWidth3, c[8], c[4]);
                            interp5(newPixels, newIndexWidth3 + 1, c[8], c[5]);
                        }
                        newPixels[newIndexWidth2 + 1] = c[5];
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[9]);
                        interp6(newPixels, newIndexWidth2 + 3, c[5], c[6],
                            c[9]);
                        interp1(newPixels, newIndexWidth3 + 2, c[5], c[9]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[9]);
                        break;
                    }
                    case 30:
                    {
                        interp8(newPixels, newIndex, c[5], c[1]);
                        interp1(newPixels, newIndex + 1, c[5], c[1]);
                        if (diff(w[2], w[6]))
                        {
                            newPixels[newIndex + 2] = c[5];
                            newPixels[newIndex + 3] = c[5];
                            newPixels[newIndexWidth + 3] = c[5];
                        }
                        else
                        {
                            interp5(newPixels, newIndex + 2, c[2], c[5]);
                            interp5(newPixels, newIndex + 3, c[2], c[6]);
                            interp5(newPixels, newIndexWidth + 3, c[6], c[5]);
                        }
                        interp1(newPixels, newIndexWidth, c[5], c[1]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[1]);
                        newPixels[newIndexWidth + 2] = c[5];
                        interp1(newPixels, newIndexWidth2, c[5], c[7]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[7]);
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[9]);
                        interp1(newPixels, newIndexWidth2 + 3, c[5], c[9]);
                        interp8(newPixels, newIndexWidth3, c[5], c[7]);
                        interp6(newPixels, newIndexWidth3 + 1, c[5], c[8], c[7]);
                        interp6(newPixels, newIndexWidth3 + 2, c[5], c[8], c[9]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[9]);
                        break;
                    }
                    case 210:
                    {
                        interp8(newPixels, newIndex, c[5], c[1]);
                        interp1(newPixels, newIndex + 1, c[5], c[1]);
                        interp1(newPixels, newIndex + 2, c[5], c[3]);
                        interp8(newPixels, newIndex + 3, c[5], c[3]);
                        interp6(newPixels, newIndexWidth, c[5], c[4], c[1]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[1]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[3]);
                        interp1(newPixels, newIndexWidth + 3, c[5], c[3]);
                        interp6(newPixels, newIndexWidth2, c[5], c[4], c[7]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[7]);
                        newPixels[newIndexWidth2 + 2] = c[5];
                        if (diff(w[6], w[8]))
                        {
                            newPixels[newIndexWidth2 + 3] = c[5];
                            newPixels[newIndexWidth3 + 2] = c[5];
                            newPixels[newIndexWidth3 + 3] = c[5];
                        }
                        else
                        {
                            interp5(newPixels, newIndexWidth2 + 3, c[6], c[5]);
                            interp5(newPixels, newIndexWidth3 + 2, c[8], c[5]);
                            interp5(newPixels, newIndexWidth3 + 3, c[8], c[6]);
                        }
                        interp8(newPixels, newIndexWidth3, c[5], c[7]);
                        interp1(newPixels, newIndexWidth3 + 1, c[5], c[7]);
                        break;
                    }
                    case 120:
                    {
                        interp8(newPixels, newIndex, c[5], c[1]);
                        interp6(newPixels, newIndex + 1, c[5], c[2], c[1]);
                        interp6(newPixels, newIndex + 2, c[5], c[2], c[3]);
                        interp8(newPixels, newIndex + 3, c[5], c[3]);
                        interp1(newPixels, newIndexWidth, c[5], c[1]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[1]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[3]);
                        interp1(newPixels, newIndexWidth + 3, c[5], c[3]);
                        if (diff(w[8], w[4]))
                        {
                            newPixels[newIndexWidth2] = c[5];
                            newPixels[newIndexWidth3] = c[5];
                            newPixels[newIndexWidth3 + 1] = c[5];
                        }
                        else
                        {
                            interp5(newPixels, newIndexWidth2, c[4], c[5]);
                            interp5(newPixels, newIndexWidth3, c[8], c[4]);
                            interp5(newPixels, newIndexWidth3 + 1, c[8], c[5]);
                        }
                        newPixels[newIndexWidth2 + 1] = c[5];
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[9]);
                        interp1(newPixels, newIndexWidth2 + 3, c[5], c[9]);
                        interp1(newPixels, newIndexWidth3 + 2, c[5], c[9]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[9]);
                        break;
                    }
                    case 75:
                    {
                        if (diff(w[4], w[2]))
                        {
                            newPixels[newIndex] = c[5];
                            newPixels[newIndex + 1] = c[5];
                            newPixels[newIndexWidth] = c[5];
                        }
                        else
                        {
                            interp5(newPixels, newIndex, c[2], c[4]);
                            interp5(newPixels, newIndex + 1, c[2], c[5]);
                            interp5(newPixels, newIndexWidth, c[4], c[5]);
                        }
                        interp1(newPixels, newIndex + 2, c[5], c[3]);
                        interp8(newPixels, newIndex + 3, c[5], c[3]);
                        newPixels[newIndexWidth + 1] = c[5];
                        interp3(newPixels, newIndexWidth + 2, c[5], c[3]);
                        interp6(newPixels, newIndexWidth + 3, c[5], c[6], c[3]);
                        interp1(newPixels, newIndexWidth2, c[5], c[7]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[7]);
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[9]);
                        interp6(newPixels, newIndexWidth2 + 3, c[5], c[6],
                            c[9]);
                        interp8(newPixels, newIndexWidth3, c[5], c[7]);
                        interp1(newPixels, newIndexWidth3 + 1, c[5], c[7]);
                        interp1(newPixels, newIndexWidth3 + 2, c[5], c[9]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[9]);
                        break;
                    }
                    case 29:
                    {
                        interp8(newPixels, newIndex, c[5], c[2]);
                        interp8(newPixels, newIndex + 1, c[5], c[2]);
                        interp8(newPixels, newIndex + 2, c[5], c[2]);
                        interp8(newPixels, newIndex + 3, c[5], c[2]);
                        interp3(newPixels, newIndexWidth, c[5], c[2]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[2]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[2]);
                        interp3(newPixels, newIndexWidth + 3, c[5], c[2]);
                        interp1(newPixels, newIndexWidth2, c[5], c[7]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[7]);
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[9]);
                        interp1(newPixels, newIndexWidth2 + 3, c[5], c[9]);
                        interp8(newPixels, newIndexWidth3, c[5], c[7]);
                        interp6(newPixels, newIndexWidth3 + 1, c[5], c[8], c[7]);
                        interp6(newPixels, newIndexWidth3 + 2, c[5], c[8], c[9]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[9]);
                        break;
                    }
                    case 198:
                    {
                        interp8(newPixels, newIndex, c[5], c[1]);
                        interp1(newPixels, newIndex + 1, c[5], c[1]);
                        interp3(newPixels, newIndex + 2, c[5], c[6]);
                        interp8(newPixels, newIndex + 3, c[5], c[6]);
                        interp6(newPixels, newIndexWidth, c[5], c[4], c[1]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[1]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[6]);
                        interp8(newPixels, newIndexWidth + 3, c[5], c[6]);
                        interp6(newPixels, newIndexWidth2, c[5], c[4], c[7]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[7]);
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[6]);
                        interp8(newPixels, newIndexWidth2 + 3, c[5], c[6]);
                        interp8(newPixels, newIndexWidth3, c[5], c[7]);
                        interp1(newPixels, newIndexWidth3 + 1, c[5], c[7]);
                        interp3(newPixels, newIndexWidth3 + 2, c[5], c[6]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[6]);
                        break;
                    }
                    case 184:
                    {
                        interp8(newPixels, newIndex, c[5], c[1]);
                        interp6(newPixels, newIndex + 1, c[5], c[2], c[1]);
                        interp6(newPixels, newIndex + 2, c[5], c[2], c[3]);
                        interp8(newPixels, newIndex + 3, c[5], c[3]);
                        interp1(newPixels, newIndexWidth, c[5], c[1]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[1]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[3]);
                        interp1(newPixels, newIndexWidth + 3, c[5], c[3]);
                        interp3(newPixels, newIndexWidth2, c[5], c[8]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[8]);
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[8]);
                        interp3(newPixels, newIndexWidth2 + 3, c[5], c[8]);
                        interp8(newPixels, newIndexWidth3, c[5], c[8]);
                        interp8(newPixels, newIndexWidth3 + 1, c[5], c[8]);
                        interp8(newPixels, newIndexWidth3 + 2, c[5], c[8]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[8]);
                        break;
                    }
                    case 99:
                    {
                        interp8(newPixels, newIndex, c[5], c[4]);
                        interp3(newPixels, newIndex + 1, c[5], c[4]);
                        interp1(newPixels, newIndex + 2, c[5], c[3]);
                        interp8(newPixels, newIndex + 3, c[5], c[3]);
                        interp8(newPixels, newIndexWidth, c[5], c[4]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[4]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[3]);
                        interp6(newPixels, newIndexWidth + 3, c[5], c[6], c[3]);
                        interp8(newPixels, newIndexWidth2, c[5], c[4]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[4]);
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[9]);
                        interp6(newPixels, newIndexWidth2 + 3, c[5], c[6],
                            c[9]);
                        interp8(newPixels, newIndexWidth3, c[5], c[4]);
                        interp3(newPixels, newIndexWidth3 + 1, c[5], c[4]);
                        interp1(newPixels, newIndexWidth3 + 2, c[5], c[9]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[9]);
                        break;
                    }
                    case 57:
                    {
                        interp8(newPixels, newIndex, c[5], c[2]);
                        interp8(newPixels, newIndex + 1, c[5], c[2]);
                        interp6(newPixels, newIndex + 2, c[5], c[2], c[3]);
                        interp8(newPixels, newIndex + 3, c[5], c[3]);
                        interp3(newPixels, newIndexWidth, c[5], c[2]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[2]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[3]);
                        interp1(newPixels, newIndexWidth + 3, c[5], c[3]);
                        interp3(newPixels, newIndexWidth2, c[5], c[8]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[8]);
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[9]);
                        interp1(newPixels, newIndexWidth2 + 3, c[5], c[9]);
                        interp8(newPixels, newIndexWidth3, c[5], c[8]);
                        interp8(newPixels, newIndexWidth3 + 1, c[5], c[8]);
                        interp6(newPixels, newIndexWidth3 + 2, c[5], c[8], c[9]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[9]);
                        break;
                    }
                    case 71:
                    {
                        interp8(newPixels, newIndex, c[5], c[4]);
                        interp3(newPixels, newIndex + 1, c[5], c[4]);
                        interp3(newPixels, newIndex + 2, c[5], c[6]);
                        interp8(newPixels, newIndex + 3, c[5], c[6]);
                        interp8(newPixels, newIndexWidth, c[5], c[4]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[4]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[6]);
                        interp8(newPixels, newIndexWidth + 3, c[5], c[6]);
                        interp6(newPixels, newIndexWidth2, c[5], c[4], c[7]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[7]);
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[9]);
                        interp6(newPixels, newIndexWidth2 + 3, c[5], c[6],
                            c[9]);
                        interp8(newPixels, newIndexWidth3, c[5], c[7]);
                        interp1(newPixels, newIndexWidth3 + 1, c[5], c[7]);
                        interp1(newPixels, newIndexWidth3 + 2, c[5], c[9]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[9]);
                        break;
                    }
                    case 156:
                    {
                        interp8(newPixels, newIndex, c[5], c[1]);
                        interp6(newPixels, newIndex + 1, c[5], c[2], c[1]);
                        interp8(newPixels, newIndex + 2, c[5], c[2]);
                        interp8(newPixels, newIndex + 3, c[5], c[2]);
                        interp1(newPixels, newIndexWidth, c[5], c[1]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[1]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[2]);
                        interp3(newPixels, newIndexWidth + 3, c[5], c[2]);
                        interp1(newPixels, newIndexWidth2, c[5], c[7]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[7]);
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[8]);
                        interp3(newPixels, newIndexWidth2 + 3, c[5], c[8]);
                        interp8(newPixels, newIndexWidth3, c[5], c[7]);
                        interp6(newPixels, newIndexWidth3 + 1, c[5], c[8], c[7]);
                        interp8(newPixels, newIndexWidth3 + 2, c[5], c[8]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[8]);
                        break;
                    }
                    case 226:
                    {
                        interp8(newPixels, newIndex, c[5], c[1]);
                        interp1(newPixels, newIndex + 1, c[5], c[1]);
                        interp1(newPixels, newIndex + 2, c[5], c[3]);
                        interp8(newPixels, newIndex + 3, c[5], c[3]);
                        interp6(newPixels, newIndexWidth, c[5], c[4], c[1]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[1]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[3]);
                        interp6(newPixels, newIndexWidth + 3, c[5], c[6], c[3]);
                        interp8(newPixels, newIndexWidth2, c[5], c[4]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[4]);
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[6]);
                        interp8(newPixels, newIndexWidth2 + 3, c[5], c[6]);
                        interp8(newPixels, newIndexWidth3, c[5], c[4]);
                        interp3(newPixels, newIndexWidth3 + 1, c[5], c[4]);
                        interp3(newPixels, newIndexWidth3 + 2, c[5], c[6]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[6]);
                        break;
                    }
                    case 60:
                    {
                        interp8(newPixels, newIndex, c[5], c[1]);
                        interp6(newPixels, newIndex + 1, c[5], c[2], c[1]);
                        interp8(newPixels, newIndex + 2, c[5], c[2]);
                        interp8(newPixels, newIndex + 3, c[5], c[2]);
                        interp1(newPixels, newIndexWidth, c[5], c[1]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[1]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[2]);
                        interp3(newPixels, newIndexWidth + 3, c[5], c[2]);
                        interp3(newPixels, newIndexWidth2, c[5], c[8]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[8]);
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[9]);
                        interp1(newPixels, newIndexWidth2 + 3, c[5], c[9]);
                        interp8(newPixels, newIndexWidth3, c[5], c[8]);
                        interp8(newPixels, newIndexWidth3 + 1, c[5], c[8]);
                        interp6(newPixels, newIndexWidth3 + 2, c[5], c[8], c[9]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[9]);
                        break;
                    }
                    case 195:
                    {
                        interp8(newPixels, newIndex, c[5], c[4]);
                        interp3(newPixels, newIndex + 1, c[5], c[4]);
                        interp1(newPixels, newIndex + 2, c[5], c[3]);
                        interp8(newPixels, newIndex + 3, c[5], c[3]);
                        interp8(newPixels, newIndexWidth, c[5], c[4]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[4]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[3]);
                        interp6(newPixels, newIndexWidth + 3, c[5], c[6], c[3]);
                        interp6(newPixels, newIndexWidth2, c[5], c[4], c[7]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[7]);
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[6]);
                        interp8(newPixels, newIndexWidth2 + 3, c[5], c[6]);
                        interp8(newPixels, newIndexWidth3, c[5], c[7]);
                        interp1(newPixels, newIndexWidth3 + 1, c[5], c[7]);
                        interp3(newPixels, newIndexWidth3 + 2, c[5], c[6]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[6]);
                        break;
                    }
                    case 102:
                    {
                        interp8(newPixels, newIndex, c[5], c[1]);
                        interp1(newPixels, newIndex + 1, c[5], c[1]);
                        interp3(newPixels, newIndex + 2, c[5], c[6]);
                        interp8(newPixels, newIndex + 3, c[5], c[6]);
                        interp6(newPixels, newIndexWidth, c[5], c[4], c[1]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[1]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[6]);
                        interp8(newPixels, newIndexWidth + 3, c[5], c[6]);
                        interp8(newPixels, newIndexWidth2, c[5], c[4]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[4]);
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[9]);
                        interp6(newPixels, newIndexWidth2 + 3, c[5], c[6],
                            c[9]);
                        interp8(newPixels, newIndexWidth3, c[5], c[4]);
                        interp3(newPixels, newIndexWidth3 + 1, c[5], c[4]);
                        interp1(newPixels, newIndexWidth3 + 2, c[5], c[9]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[9]);
                        break;
                    }
                    case 153:
                    {
                        interp8(newPixels, newIndex, c[5], c[2]);
                        interp8(newPixels, newIndex + 1, c[5], c[2]);
                        interp6(newPixels, newIndex + 2, c[5], c[2], c[3]);
                        interp8(newPixels, newIndex + 3, c[5], c[3]);
                        interp3(newPixels, newIndexWidth, c[5], c[2]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[2]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[3]);
                        interp1(newPixels, newIndexWidth + 3, c[5], c[3]);
                        interp1(newPixels, newIndexWidth2, c[5], c[7]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[7]);
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[8]);
                        interp3(newPixels, newIndexWidth2 + 3, c[5], c[8]);
                        interp8(newPixels, newIndexWidth3, c[5], c[7]);
                        interp6(newPixels, newIndexWidth3 + 1, c[5], c[8], c[7]);
                        interp8(newPixels, newIndexWidth3 + 2, c[5], c[8]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[8]);
                        break;
                    }
                    case 58:
                    {
                        if (diff(w[4], w[2]))
                        {
                            interp8(newPixels, newIndex, c[5], c[1]);
                            interp1(newPixels, newIndex + 1, c[5], c[1]);
                            interp1(newPixels, newIndexWidth, c[5], c[1]);
                            interp3(newPixels, newIndexWidth + 1, c[5], c[1]);
                        }
                        else
                        {
                            interp2(newPixels, newIndex, c[5], c[2], c[4]);
                            interp1(newPixels, newIndex + 1, c[5], c[2]);
                            interp1(newPixels, newIndexWidth, c[5], c[4]);
                            newPixels[newIndexWidth + 1] = c[5];
                        }
                        if (diff(w[2], w[6]))
                        {
                            interp1(newPixels, newIndex + 2, c[5], c[3]);
                            interp8(newPixels, newIndex + 3, c[5], c[3]);
                            interp3(newPixels, newIndexWidth + 2, c[5], c[3]);
                            interp1(newPixels, newIndexWidth + 3, c[5], c[3]);
                        }
                        else
                        {
                            interp1(newPixels, newIndex + 2, c[5], c[2]);
                            interp2(newPixels, newIndex + 3, c[5], c[2], c[6]);
                            newPixels[newIndexWidth + 2] = c[5];
                            interp1(newPixels, newIndexWidth + 3, c[5], c[6]);
                        }
                        interp3(newPixels, newIndexWidth2, c[5], c[8]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[8]);
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[9]);
                        interp1(newPixels, newIndexWidth2 + 3, c[5], c[9]);
                        interp8(newPixels, newIndexWidth3, c[5], c[8]);
                        interp8(newPixels, newIndexWidth3 + 1, c[5], c[8]);
                        interp6(newPixels, newIndexWidth3 + 2, c[5], c[8], c[9]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[9]);
                        break;
                    }
                    case 83:
                    {
                        interp8(newPixels, newIndex, c[5], c[4]);
                        interp3(newPixels, newIndex + 1, c[5], c[4]);
                        if (diff(w[2], w[6]))
                        {
                            interp1(newPixels, newIndex + 2, c[5], c[3]);
                            interp8(newPixels, newIndex + 3, c[5], c[3]);
                            interp3(newPixels, newIndexWidth + 2, c[5], c[3]);
                            interp1(newPixels, newIndexWidth + 3, c[5], c[3]);
                        }
                        else
                        {
                            interp1(newPixels, newIndex + 2, c[5], c[2]);
                            interp2(newPixels, newIndex + 3, c[5], c[2], c[6]);
                            newPixels[newIndexWidth + 2] = c[5];
                            interp1(newPixels, newIndexWidth + 3, c[5], c[6]);
                        }
                        interp8(newPixels, newIndexWidth, c[5], c[4]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[4]);
                        interp6(newPixels, newIndexWidth2, c[5], c[4], c[7]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[7]);
                        if (diff(w[6], w[8]))
                        {
                            interp3(newPixels, newIndexWidth2 + 2, c[5], c[9]);
                            interp1(newPixels, newIndexWidth2 + 3, c[5], c[9]);
                            interp1(newPixels, newIndexWidth3 + 2, c[5], c[9]);
                            interp8(newPixels, newIndexWidth3 + 3, c[5], c[9]);
                        }
                        else
                        {
                            newPixels[newIndexWidth2 + 2] = c[5];
                            interp1(newPixels, newIndexWidth2 + 3, c[5], c[6]);
                            interp1(newPixels, newIndexWidth3 + 2, c[5], c[8]);
                            interp2(newPixels, newIndexWidth3 + 3, c[5], c[8],
                                c[6]);
                        }
                        interp8(newPixels, newIndexWidth3, c[5], c[7]);
                        interp1(newPixels, newIndexWidth3 + 1, c[5], c[7]);
                        break;
                    }
                    case 92:
                    {
                        interp8(newPixels, newIndex, c[5], c[1]);
                        interp6(newPixels, newIndex + 1, c[5], c[2], c[1]);
                        interp8(newPixels, newIndex + 2, c[5], c[2]);
                        interp8(newPixels, newIndex + 3, c[5], c[2]);
                        interp1(newPixels, newIndexWidth, c[5], c[1]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[1]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[2]);
                        interp3(newPixels, newIndexWidth + 3, c[5], c[2]);
                        if (diff(w[8], w[4]))
                        {
                            interp1(newPixels, newIndexWidth2, c[5], c[7]);
                            interp3(newPixels, newIndexWidth2 + 1, c[5], c[7]);
                            interp8(newPixels, newIndexWidth3, c[5], c[7]);
                            interp1(newPixels, newIndexWidth3 + 1, c[5], c[7]);
                        }
                        else
                        {
                            interp1(newPixels, newIndexWidth2, c[5], c[4]);
                            newPixels[newIndexWidth2 + 1] = c[5];
                            interp2(newPixels, newIndexWidth3, c[5], c[8], c[4]);
                            interp1(newPixels, newIndexWidth3 + 1, c[5], c[8]);
                        }
                        if (diff(w[6], w[8]))
                        {
                            interp3(newPixels, newIndexWidth2 + 2, c[5], c[9]);
                            interp1(newPixels, newIndexWidth2 + 3, c[5], c[9]);
                            interp1(newPixels, newIndexWidth3 + 2, c[5], c[9]);
                            interp8(newPixels, newIndexWidth3 + 3, c[5], c[9]);
                        }
                        else
                        {
                            newPixels[newIndexWidth2 + 2] = c[5];
                            interp1(newPixels, newIndexWidth2 + 3, c[5], c[6]);
                            interp1(newPixels, newIndexWidth3 + 2, c[5], c[8]);
                            interp2(newPixels, newIndexWidth3 + 3, c[5], c[8],
                                c[6]);
                        }
                        break;
                    }
                    case 202:
                    {
                        if (diff(w[4], w[2]))
                        {
                            interp8(newPixels, newIndex, c[5], c[1]);
                            interp1(newPixels, newIndex + 1, c[5], c[1]);
                            interp1(newPixels, newIndexWidth, c[5], c[1]);
                            interp3(newPixels, newIndexWidth + 1, c[5], c[1]);
                        }
                        else
                        {
                            interp2(newPixels, newIndex, c[5], c[2], c[4]);
                            interp1(newPixels, newIndex + 1, c[5], c[2]);
                            interp1(newPixels, newIndexWidth, c[5], c[4]);
                            newPixels[newIndexWidth + 1] = c[5];
                        }
                        interp1(newPixels, newIndex + 2, c[5], c[3]);
                        interp8(newPixels, newIndex + 3, c[5], c[3]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[3]);
                        interp6(newPixels, newIndexWidth + 3, c[5], c[6], c[3]);
                        if (diff(w[8], w[4]))
                        {
                            interp1(newPixels, newIndexWidth2, c[5], c[7]);
                            interp3(newPixels, newIndexWidth2 + 1, c[5], c[7]);
                            interp8(newPixels, newIndexWidth3, c[5], c[7]);
                            interp1(newPixels, newIndexWidth3 + 1, c[5], c[7]);
                        }
                        else
                        {
                            interp1(newPixels, newIndexWidth2, c[5], c[4]);
                            newPixels[newIndexWidth2 + 1] = c[5];
                            interp2(newPixels, newIndexWidth3, c[5], c[8], c[4]);
                            interp1(newPixels, newIndexWidth3 + 1, c[5], c[8]);
                        }
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[6]);
                        interp8(newPixels, newIndexWidth2 + 3, c[5], c[6]);
                        interp3(newPixels, newIndexWidth3 + 2, c[5], c[6]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[6]);
                        break;
                    }
                    case 78:
                    {
                        if (diff(w[4], w[2]))
                        {
                            interp8(newPixels, newIndex, c[5], c[1]);
                            interp1(newPixels, newIndex + 1, c[5], c[1]);
                            interp1(newPixels, newIndexWidth, c[5], c[1]);
                            interp3(newPixels, newIndexWidth + 1, c[5], c[1]);
                        }
                        else
                        {
                            interp2(newPixels, newIndex, c[5], c[2], c[4]);
                            interp1(newPixels, newIndex + 1, c[5], c[2]);
                            interp1(newPixels, newIndexWidth, c[5], c[4]);
                            newPixels[newIndexWidth + 1] = c[5];
                        }
                        interp3(newPixels, newIndex + 2, c[5], c[6]);
                        interp8(newPixels, newIndex + 3, c[5], c[6]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[6]);
                        interp8(newPixels, newIndexWidth + 3, c[5], c[6]);
                        if (diff(w[8], w[4]))
                        {
                            interp1(newPixels, newIndexWidth2, c[5], c[7]);
                            interp3(newPixels, newIndexWidth2 + 1, c[5], c[7]);
                            interp8(newPixels, newIndexWidth3, c[5], c[7]);
                            interp1(newPixels, newIndexWidth3 + 1, c[5], c[7]);
                        }
                        else
                        {
                            interp1(newPixels, newIndexWidth2, c[5], c[4]);
                            newPixels[newIndexWidth2 + 1] = c[5];
                            interp2(newPixels, newIndexWidth3, c[5], c[8], c[4]);
                            interp1(newPixels, newIndexWidth3 + 1, c[5], c[8]);
                        }
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[9]);
                        interp6(newPixels, newIndexWidth2 + 3, c[5], c[6],
                            c[9]);
                        interp1(newPixels, newIndexWidth3 + 2, c[5], c[9]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[9]);
                        break;
                    }
                    case 154:
                    {
                        if (diff(w[4], w[2]))
                        {
                            interp8(newPixels, newIndex, c[5], c[1]);
                            interp1(newPixels, newIndex + 1, c[5], c[1]);
                            interp1(newPixels, newIndexWidth, c[5], c[1]);
                            interp3(newPixels, newIndexWidth + 1, c[5], c[1]);
                        }
                        else
                        {
                            interp2(newPixels, newIndex, c[5], c[2], c[4]);
                            interp1(newPixels, newIndex + 1, c[5], c[2]);
                            interp1(newPixels, newIndexWidth, c[5], c[4]);
                            newPixels[newIndexWidth + 1] = c[5];
                        }
                        if (diff(w[2], w[6]))
                        {
                            interp1(newPixels, newIndex + 2, c[5], c[3]);
                            interp8(newPixels, newIndex + 3, c[5], c[3]);
                            interp3(newPixels, newIndexWidth + 2, c[5], c[3]);
                            interp1(newPixels, newIndexWidth + 3, c[5], c[3]);
                        }
                        else
                        {
                            interp1(newPixels, newIndex + 2, c[5], c[2]);
                            interp2(newPixels, newIndex + 3, c[5], c[2], c[6]);
                            newPixels[newIndexWidth + 2] = c[5];
                            interp1(newPixels, newIndexWidth + 3, c[5], c[6]);
                        }
                        interp1(newPixels, newIndexWidth2, c[5], c[7]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[7]);
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[8]);
                        interp3(newPixels, newIndexWidth2 + 3, c[5], c[8]);
                        interp8(newPixels, newIndexWidth3, c[5], c[7]);
                        interp6(newPixels, newIndexWidth3 + 1, c[5], c[8], c[7]);
                        interp8(newPixels, newIndexWidth3 + 2, c[5], c[8]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[8]);
                        break;
                    }
                    case 114:
                    {
                        interp8(newPixels, newIndex, c[5], c[1]);
                        interp1(newPixels, newIndex + 1, c[5], c[1]);
                        if (diff(w[2], w[6]))
                        {
                            interp1(newPixels, newIndex + 2, c[5], c[3]);
                            interp8(newPixels, newIndex + 3, c[5], c[3]);
                            interp3(newPixels, newIndexWidth + 2, c[5], c[3]);
                            interp1(newPixels, newIndexWidth + 3, c[5], c[3]);
                        }
                        else
                        {
                            interp1(newPixels, newIndex + 2, c[5], c[2]);
                            interp2(newPixels, newIndex + 3, c[5], c[2], c[6]);
                            newPixels[newIndexWidth + 2] = c[5];
                            interp1(newPixels, newIndexWidth + 3, c[5], c[6]);
                        }
                        interp6(newPixels, newIndexWidth, c[5], c[4], c[1]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[1]);
                        interp8(newPixels, newIndexWidth2, c[5], c[4]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[4]);
                        if (diff(w[6], w[8]))
                        {
                            interp3(newPixels, newIndexWidth2 + 2, c[5], c[9]);
                            interp1(newPixels, newIndexWidth2 + 3, c[5], c[9]);
                            interp1(newPixels, newIndexWidth3 + 2, c[5], c[9]);
                            interp8(newPixels, newIndexWidth3 + 3, c[5], c[9]);
                        }
                        else
                        {
                            newPixels[newIndexWidth2 + 2] = c[5];
                            interp1(newPixels, newIndexWidth2 + 3, c[5], c[6]);
                            interp1(newPixels, newIndexWidth3 + 2, c[5], c[8]);
                            interp2(newPixels, newIndexWidth3 + 3, c[5], c[8],
                                c[6]);
                        }
                        interp8(newPixels, newIndexWidth3, c[5], c[4]);
                        interp3(newPixels, newIndexWidth3 + 1, c[5], c[4]);
                        break;
                    }
                    case 89:
                    {
                        interp8(newPixels, newIndex, c[5], c[2]);
                        interp8(newPixels, newIndex + 1, c[5], c[2]);
                        interp6(newPixels, newIndex + 2, c[5], c[2], c[3]);
                        interp8(newPixels, newIndex + 3, c[5], c[3]);
                        interp3(newPixels, newIndexWidth, c[5], c[2]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[2]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[3]);
                        interp1(newPixels, newIndexWidth + 3, c[5], c[3]);
                        if (diff(w[8], w[4]))
                        {
                            interp1(newPixels, newIndexWidth2, c[5], c[7]);
                            interp3(newPixels, newIndexWidth2 + 1, c[5], c[7]);
                            interp8(newPixels, newIndexWidth3, c[5], c[7]);
                            interp1(newPixels, newIndexWidth3 + 1, c[5], c[7]);
                        }
                        else
                        {
                            interp1(newPixels, newIndexWidth2, c[5], c[4]);
                            newPixels[newIndexWidth2 + 1] = c[5];
                            interp2(newPixels, newIndexWidth3, c[5], c[8], c[4]);
                            interp1(newPixels, newIndexWidth3 + 1, c[5], c[8]);
                        }
                        if (diff(w[6], w[8]))
                        {
                            interp3(newPixels, newIndexWidth2 + 2, c[5], c[9]);
                            interp1(newPixels, newIndexWidth2 + 3, c[5], c[9]);
                            interp1(newPixels, newIndexWidth3 + 2, c[5], c[9]);
                            interp8(newPixels, newIndexWidth3 + 3, c[5], c[9]);
                        }
                        else
                        {
                            newPixels[newIndexWidth2 + 2] = c[5];
                            interp1(newPixels, newIndexWidth2 + 3, c[5], c[6]);
                            interp1(newPixels, newIndexWidth3 + 2, c[5], c[8]);
                            interp2(newPixels, newIndexWidth3 + 3, c[5], c[8],
                                c[6]);
                        }
                        break;
                    }
                    case 90:
                    {
                        if (diff(w[4], w[2]))
                        {
                            interp8(newPixels, newIndex, c[5], c[1]);
                            interp1(newPixels, newIndex + 1, c[5], c[1]);
                            interp1(newPixels, newIndexWidth, c[5], c[1]);
                            interp3(newPixels, newIndexWidth + 1, c[5], c[1]);
                        }
                        else
                        {
                            interp2(newPixels, newIndex, c[5], c[2], c[4]);
                            interp1(newPixels, newIndex + 1, c[5], c[2]);
                            interp1(newPixels, newIndexWidth, c[5], c[4]);
                            newPixels[newIndexWidth + 1] = c[5];
                        }
                        if (diff(w[2], w[6]))
                        {
                            interp1(newPixels, newIndex + 2, c[5], c[3]);
                            interp8(newPixels, newIndex + 3, c[5], c[3]);
                            interp3(newPixels, newIndexWidth + 2, c[5], c[3]);
                            interp1(newPixels, newIndexWidth + 3, c[5], c[3]);
                        }
                        else
                        {
                            interp1(newPixels, newIndex + 2, c[5], c[2]);
                            interp2(newPixels, newIndex + 3, c[5], c[2], c[6]);
                            newPixels[newIndexWidth + 2] = c[5];
                            interp1(newPixels, newIndexWidth + 3, c[5], c[6]);
                        }
                        if (diff(w[8], w[4]))
                        {
                            interp1(newPixels, newIndexWidth2, c[5], c[7]);
                            interp3(newPixels, newIndexWidth2 + 1, c[5], c[7]);
                            interp8(newPixels, newIndexWidth3, c[5], c[7]);
                            interp1(newPixels, newIndexWidth3 + 1, c[5], c[7]);
                        }
                        else
                        {
                            interp1(newPixels, newIndexWidth2, c[5], c[4]);
                            newPixels[newIndexWidth2 + 1] = c[5];
                            interp2(newPixels, newIndexWidth3, c[5], c[8], c[4]);
                            interp1(newPixels, newIndexWidth3 + 1, c[5], c[8]);
                        }
                        if (diff(w[6], w[8]))
                        {
                            interp3(newPixels, newIndexWidth2 + 2, c[5], c[9]);
                            interp1(newPixels, newIndexWidth2 + 3, c[5], c[9]);
                            interp1(newPixels, newIndexWidth3 + 2, c[5], c[9]);
                            interp8(newPixels, newIndexWidth3 + 3, c[5], c[9]);
                        }
                        else
                        {
                            newPixels[newIndexWidth2 + 2] = c[5];
                            interp1(newPixels, newIndexWidth2 + 3, c[5], c[6]);
                            interp1(newPixels, newIndexWidth3 + 2, c[5], c[8]);
                            interp2(newPixels, newIndexWidth3 + 3, c[5], c[8],
                                c[6]);
                        }
                        break;
                    }
                    case 55:
                    case 23:
                    {
                        if (diff(w[2], w[6]))
                        {
                            interp8(newPixels, newIndex, c[5], c[4]);
                            interp3(newPixels, newIndex + 1, c[5], c[4]);
                            newPixels[newIndex + 2] = c[5];
                            newPixels[newIndex + 3] = c[5];
                            newPixels[newIndexWidth + 2] = c[5];
                            newPixels[newIndexWidth + 3] = c[5];
                        }
                        else
                        {
                            interp1(newPixels, newIndex, c[5], c[2]);
                            interp1(newPixels, newIndex + 1, c[2], c[5]);
                            interp8(newPixels, newIndex + 2, c[2], c[6]);
                            interp5(newPixels, newIndex + 3, c[2], c[6]);
                            interp7(newPixels, newIndexWidth + 2, c[5], c[6],
                                c[2]);
                            interp2(newPixels, newIndexWidth + 3, c[6], c[5],
                                c[2]);
                        }
                        interp8(newPixels, newIndexWidth, c[5], c[4]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[4]);
                        interp6(newPixels, newIndexWidth2, c[5], c[4], c[8]);
                        interp7(newPixels, newIndexWidth2 + 1, c[5], c[4], c[8]);
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[9]);
                        interp1(newPixels, newIndexWidth2 + 3, c[5], c[9]);
                        interp2(newPixels, newIndexWidth3, c[5], c[8], c[4]);
                        interp6(newPixels, newIndexWidth3 + 1, c[5], c[8], c[4]);
                        interp6(newPixels, newIndexWidth3 + 2, c[5], c[8], c[9]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[9]);
                        break;
                    }
                    case 182:
                    case 150:
                    {
                        interp8(newPixels, newIndex, c[5], c[1]);
                        interp1(newPixels, newIndex + 1, c[5], c[1]);
                        if (diff(w[2], w[6]))
                        {
                            newPixels[newIndex + 2] = c[5];
                            newPixels[newIndex + 3] = c[5];
                            newPixels[newIndexWidth + 2] = c[5];
                            newPixels[newIndexWidth + 3] = c[5];
                            interp3(newPixels, newIndexWidth2 + 3, c[5], c[8]);
                            interp8(newPixels, newIndexWidth3 + 3, c[5], c[8]);
                        }
                        else
                        {
                            interp2(newPixels, newIndex + 2, c[2], c[5], c[6]);
                            interp5(newPixels, newIndex + 3, c[2], c[6]);
                            interp7(newPixels, newIndexWidth + 2, c[5], c[6],
                                c[2]);
                            interp8(newPixels, newIndexWidth + 3, c[6], c[2]);
                            interp1(newPixels, newIndexWidth2 + 3, c[6], c[5]);
                            interp1(newPixels, newIndexWidth3 + 3, c[5], c[6]);
                        }
                        interp6(newPixels, newIndexWidth, c[5], c[4], c[1]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[1]);
                        interp6(newPixels, newIndexWidth2, c[5], c[4], c[8]);
                        interp7(newPixels, newIndexWidth2 + 1, c[5], c[4], c[8]);
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[8]);
                        interp2(newPixels, newIndexWidth3, c[5], c[8], c[4]);
                        interp6(newPixels, newIndexWidth3 + 1, c[5], c[8], c[4]);
                        interp8(newPixels, newIndexWidth3 + 2, c[5], c[8]);
                        break;
                    }
                    case 213:
                    case 212:
                    {
                        interp2(newPixels, newIndex, c[5], c[2], c[4]);
                        interp6(newPixels, newIndex + 1, c[5], c[2], c[4]);
                        interp8(newPixels, newIndex + 2, c[5], c[2]);
                        if (diff(w[6], w[8]))
                        {
                            interp8(newPixels, newIndex + 3, c[5], c[2]);
                            interp3(newPixels, newIndexWidth + 3, c[5], c[2]);
                            newPixels[newIndexWidth2 + 2] = c[5];
                            newPixels[newIndexWidth2 + 3] = c[5];
                            newPixels[newIndexWidth3 + 2] = c[5];
                            newPixels[newIndexWidth3 + 3] = c[5];
                        }
                        else
                        {
                            interp1(newPixels, newIndex + 3, c[5], c[6]);
                            interp1(newPixels, newIndexWidth + 3, c[6], c[5]);
                            interp7(newPixels, newIndexWidth2 + 2, c[5], c[6],
                                c[8]);
                            interp8(newPixels, newIndexWidth2 + 3, c[6], c[8]);
                            interp2(newPixels, newIndexWidth3 + 2, c[8], c[5],
                                c[6]);
                            interp5(newPixels, newIndexWidth3 + 3, c[8], c[6]);
                        }
                        interp6(newPixels, newIndexWidth, c[5], c[4], c[2]);
                        interp7(newPixels, newIndexWidth + 1, c[5], c[4], c[2]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[2]);
                        interp6(newPixels, newIndexWidth2, c[5], c[4], c[7]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[7]);
                        interp8(newPixels, newIndexWidth3, c[5], c[7]);
                        interp1(newPixels, newIndexWidth3 + 1, c[5], c[7]);
                        break;
                    }
                    case 241:
                    case 240:
                    {
                        interp2(newPixels, newIndex, c[5], c[2], c[4]);
                        interp6(newPixels, newIndex + 1, c[5], c[2], c[4]);
                        interp6(newPixels, newIndex + 2, c[5], c[2], c[3]);
                        interp8(newPixels, newIndex + 3, c[5], c[3]);
                        interp6(newPixels, newIndexWidth, c[5], c[4], c[2]);
                        interp7(newPixels, newIndexWidth + 1, c[5], c[4], c[2]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[3]);
                        interp1(newPixels, newIndexWidth + 3, c[5], c[3]);
                        interp8(newPixels, newIndexWidth2, c[5], c[4]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[4]);
                        if (diff(w[6], w[8]))
                        {
                            newPixels[newIndexWidth2 + 2] = c[5];
                            newPixels[newIndexWidth2 + 3] = c[5];
                            interp8(newPixels, newIndexWidth3, c[5], c[4]);
                            interp3(newPixels, newIndexWidth3 + 1, c[5], c[4]);
                            newPixels[newIndexWidth3 + 2] = c[5];
                            newPixels[newIndexWidth3 + 3] = c[5];
                        }
                        else
                        {
                            interp7(newPixels, newIndexWidth2 + 2, c[5], c[6],
                                c[8]);
                            interp2(newPixels, newIndexWidth2 + 3, c[6], c[5],
                                c[8]);
                            interp1(newPixels, newIndexWidth3, c[5], c[8]);
                            interp1(newPixels, newIndexWidth3 + 1, c[8], c[5]);
                            interp8(newPixels, newIndexWidth3 + 2, c[8], c[6]);
                            interp5(newPixels, newIndexWidth3 + 3, c[8], c[6]);
                        }
                        break;
                    }
                    case 236:
                    case 232:
                    {
                        interp8(newPixels, newIndex, c[5], c[1]);
                        interp6(newPixels, newIndex + 1, c[5], c[2], c[1]);
                        interp6(newPixels, newIndex + 2, c[5], c[2], c[6]);
                        interp2(newPixels, newIndex + 3, c[5], c[2], c[6]);
                        interp1(newPixels, newIndexWidth, c[5], c[1]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[1]);
                        interp7(newPixels, newIndexWidth + 2, c[5], c[6], c[2]);
                        interp6(newPixels, newIndexWidth + 3, c[5], c[6], c[2]);
                        if (diff(w[8], w[4]))
                        {
                            newPixels[newIndexWidth2] = c[5];
                            newPixels[newIndexWidth2 + 1] = c[5];
                            newPixels[newIndexWidth3] = c[5];
                            newPixels[newIndexWidth3 + 1] = c[5];
                            interp3(newPixels, newIndexWidth3 + 2, c[5], c[6]);
                            interp8(newPixels, newIndexWidth3 + 3, c[5], c[6]);
                        }
                        else
                        {
                            interp2(newPixels, newIndexWidth2, c[4], c[5], c[8]);
                            interp7(newPixels, newIndexWidth2 + 1, c[5], c[4],
                                c[8]);
                            interp5(newPixels, newIndexWidth3, c[8], c[4]);
                            interp8(newPixels, newIndexWidth3 + 1, c[8], c[4]);
                            interp1(newPixels, newIndexWidth3 + 2, c[8], c[5]);
                            interp1(newPixels, newIndexWidth3 + 3, c[5], c[8]);
                        }
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[6]);
                        interp8(newPixels, newIndexWidth2 + 3, c[5], c[6]);
                        break;
                    }
                    case 109:
                    case 105:
                    {
                        if (diff(w[8], w[4]))
                        {
                            interp8(newPixels, newIndex, c[5], c[2]);
                            interp3(newPixels, newIndexWidth, c[5], c[2]);
                            newPixels[newIndexWidth2] = c[5];
                            newPixels[newIndexWidth2 + 1] = c[5];
                            newPixels[newIndexWidth3] = c[5];
                            newPixels[newIndexWidth3 + 1] = c[5];
                        }
                        else
                        {
                            interp1(newPixels, newIndex, c[5], c[4]);
                            interp1(newPixels, newIndexWidth, c[4], c[5]);
                            interp8(newPixels, newIndexWidth2, c[4], c[8]);
                            interp7(newPixels, newIndexWidth2 + 1, c[5], c[4],
                                c[8]);
                            interp5(newPixels, newIndexWidth3, c[8], c[4]);
                            interp2(newPixels, newIndexWidth3 + 1, c[8], c[5],
                                c[4]);
                        }
                        interp8(newPixels, newIndex + 1, c[5], c[2]);
                        interp6(newPixels, newIndex + 2, c[5], c[2], c[6]);
                        interp2(newPixels, newIndex + 3, c[5], c[2], c[6]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[2]);
                        interp7(newPixels, newIndexWidth + 2, c[5], c[6], c[2]);
                        interp6(newPixels, newIndexWidth + 3, c[5], c[6], c[2]);
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[9]);
                        interp6(newPixels, newIndexWidth2 + 3, c[5], c[6],
                            c[9]);
                        interp1(newPixels, newIndexWidth3 + 2, c[5], c[9]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[9]);
                        break;
                    }
                    case 171:
                    case 43:
                    {
                        if (diff(w[4], w[2]))
                        {
                            newPixels[newIndex] = c[5];
                            newPixels[newIndex + 1] = c[5];
                            newPixels[newIndexWidth] = c[5];
                            newPixels[newIndexWidth + 1] = c[5];
                            interp3(newPixels, newIndexWidth2, c[5], c[8]);
                            interp8(newPixels, newIndexWidth3, c[5], c[8]);
                        }
                        else
                        {
                            interp5(newPixels, newIndex, c[2], c[4]);
                            interp2(newPixels, newIndex + 1, c[2], c[5], c[4]);
                            interp8(newPixels, newIndexWidth, c[4], c[2]);
                            interp7(newPixels, newIndexWidth + 1, c[5], c[4],
                                c[2]);
                            interp1(newPixels, newIndexWidth2, c[4], c[5]);
                            interp1(newPixels, newIndexWidth3, c[5], c[4]);
                        }
                        interp1(newPixels, newIndex + 2, c[5], c[3]);
                        interp8(newPixels, newIndex + 3, c[5], c[3]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[3]);
                        interp6(newPixels, newIndexWidth + 3, c[5], c[6], c[3]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[8]);
                        interp7(newPixels, newIndexWidth2 + 2, c[5], c[6], c[8]);
                        interp6(newPixels, newIndexWidth2 + 3, c[5], c[6],
                            c[8]);
                        interp8(newPixels, newIndexWidth3 + 1, c[5], c[8]);
                        interp6(newPixels, newIndexWidth3 + 2, c[5], c[8], c[6]);
                        interp2(newPixels, newIndexWidth3 + 3, c[5], c[8],
                            c[6]);
                        break;
                    }
                    case 143:
                    case 15:
                    {
                        if (diff(w[4], w[2]))
                        {
                            newPixels[newIndex] = c[5];
                            newPixels[newIndex + 1] = c[5];
                            interp3(newPixels, newIndex + 2, c[5], c[6]);
                            interp8(newPixels, newIndex + 3, c[5], c[6]);
                            newPixels[newIndexWidth] = c[5];
                            newPixels[newIndexWidth + 1] = c[5];
                        }
                        else
                        {
                            interp5(newPixels, newIndex, c[2], c[4]);
                            interp8(newPixels, newIndex + 1, c[2], c[4]);
                            interp1(newPixels, newIndex + 2, c[2], c[5]);
                            interp1(newPixels, newIndex + 3, c[5], c[2]);
                            interp2(newPixels, newIndexWidth, c[4], c[5], c[2]);
                            interp7(newPixels, newIndexWidth + 1, c[5], c[4],
                                c[2]);
                        }
                        interp3(newPixels, newIndexWidth + 2, c[5], c[6]);
                        interp8(newPixels, newIndexWidth + 3, c[5], c[6]);
                        interp1(newPixels, newIndexWidth2, c[5], c[7]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[7]);
                        interp7(newPixels, newIndexWidth2 + 2, c[5], c[6], c[8]);
                        interp6(newPixels, newIndexWidth2 + 3, c[5], c[6],
                            c[8]);
                        interp8(newPixels, newIndexWidth3, c[5], c[7]);
                        interp6(newPixels, newIndexWidth3 + 1, c[5], c[8], c[7]);
                        interp6(newPixels, newIndexWidth3 + 2, c[5], c[8], c[6]);
                        interp2(newPixels, newIndexWidth3 + 3, c[5], c[8],
                            c[6]);
                        break;
                    }
                    case 124:
                    {
                        interp8(newPixels, newIndex, c[5], c[1]);
                        interp6(newPixels, newIndex + 1, c[5], c[2], c[1]);
                        interp8(newPixels, newIndex + 2, c[5], c[2]);
                        interp8(newPixels, newIndex + 3, c[5], c[2]);
                        interp1(newPixels, newIndexWidth, c[5], c[1]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[1]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[2]);
                        interp3(newPixels, newIndexWidth + 3, c[5], c[2]);
                        if (diff(w[8], w[4]))
                        {
                            newPixels[newIndexWidth2] = c[5];
                            newPixels[newIndexWidth3] = c[5];
                            newPixels[newIndexWidth3 + 1] = c[5];
                        }
                        else
                        {
                            interp5(newPixels, newIndexWidth2, c[4], c[5]);
                            interp5(newPixels, newIndexWidth3, c[8], c[4]);
                            interp5(newPixels, newIndexWidth3 + 1, c[8], c[5]);
                        }
                        newPixels[newIndexWidth2 + 1] = c[5];
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[9]);
                        interp1(newPixels, newIndexWidth2 + 3, c[5], c[9]);
                        interp1(newPixels, newIndexWidth3 + 2, c[5], c[9]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[9]);
                        break;
                    }
                    case 203:
                    {
                        if (diff(w[4], w[2]))
                        {
                            newPixels[newIndex] = c[5];
                            newPixels[newIndex + 1] = c[5];
                            newPixels[newIndexWidth] = c[5];
                        }
                        else
                        {
                            interp5(newPixels, newIndex, c[2], c[4]);
                            interp5(newPixels, newIndex + 1, c[2], c[5]);
                            interp5(newPixels, newIndexWidth, c[4], c[5]);
                        }
                        interp1(newPixels, newIndex + 2, c[5], c[3]);
                        interp8(newPixels, newIndex + 3, c[5], c[3]);
                        newPixels[newIndexWidth + 1] = c[5];
                        interp3(newPixels, newIndexWidth + 2, c[5], c[3]);
                        interp6(newPixels, newIndexWidth + 3, c[5], c[6], c[3]);
                        interp1(newPixels, newIndexWidth2, c[5], c[7]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[7]);
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[6]);
                        interp8(newPixels, newIndexWidth2 + 3, c[5], c[6]);
                        interp8(newPixels, newIndexWidth3, c[5], c[7]);
                        interp1(newPixels, newIndexWidth3 + 1, c[5], c[7]);
                        interp3(newPixels, newIndexWidth3 + 2, c[5], c[6]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[6]);
                        break;
                    }
                    case 62:
                    {
                        interp8(newPixels, newIndex, c[5], c[1]);
                        interp1(newPixels, newIndex + 1, c[5], c[1]);
                        if (diff(w[2], w[6]))
                        {
                            newPixels[newIndex + 2] = c[5];
                            newPixels[newIndex + 3] = c[5];
                            newPixels[newIndexWidth + 3] = c[5];
                        }
                        else
                        {
                            interp5(newPixels, newIndex + 2, c[2], c[5]);
                            interp5(newPixels, newIndex + 3, c[2], c[6]);
                            interp5(newPixels, newIndexWidth + 3, c[6], c[5]);
                        }
                        interp1(newPixels, newIndexWidth, c[5], c[1]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[1]);
                        newPixels[newIndexWidth + 2] = c[5];
                        interp3(newPixels, newIndexWidth2, c[5], c[8]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[8]);
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[9]);
                        interp1(newPixels, newIndexWidth2 + 3, c[5], c[9]);
                        interp8(newPixels, newIndexWidth3, c[5], c[8]);
                        interp8(newPixels, newIndexWidth3 + 1, c[5], c[8]);
                        interp6(newPixels, newIndexWidth3 + 2, c[5], c[8], c[9]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[9]);
                        break;
                    }
                    case 211:
                    {
                        interp8(newPixels, newIndex, c[5], c[4]);
                        interp3(newPixels, newIndex + 1, c[5], c[4]);
                        interp1(newPixels, newIndex + 2, c[5], c[3]);
                        interp8(newPixels, newIndex + 3, c[5], c[3]);
                        interp8(newPixels, newIndexWidth, c[5], c[4]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[4]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[3]);
                        interp1(newPixels, newIndexWidth + 3, c[5], c[3]);
                        interp6(newPixels, newIndexWidth2, c[5], c[4], c[7]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[7]);
                        newPixels[newIndexWidth2 + 2] = c[5];
                        if (diff(w[6], w[8]))
                        {
                            newPixels[newIndexWidth2 + 3] = c[5];
                            newPixels[newIndexWidth3 + 2] = c[5];
                            newPixels[newIndexWidth3 + 3] = c[5];
                        }
                        else
                        {
                            interp5(newPixels, newIndexWidth2 + 3, c[6], c[5]);
                            interp5(newPixels, newIndexWidth3 + 2, c[8], c[5]);
                            interp5(newPixels, newIndexWidth3 + 3, c[8], c[6]);
                        }
                        interp8(newPixels, newIndexWidth3, c[5], c[7]);
                        interp1(newPixels, newIndexWidth3 + 1, c[5], c[7]);
                        break;
                    }
                    case 118:
                    {
                        interp8(newPixels, newIndex, c[5], c[1]);
                        interp1(newPixels, newIndex + 1, c[5], c[1]);
                        if (diff(w[2], w[6]))
                        {
                            newPixels[newIndex + 2] = c[5];
                            newPixels[newIndex + 3] = c[5];
                            newPixels[newIndexWidth + 3] = c[5];
                        }
                        else
                        {
                            interp5(newPixels, newIndex + 2, c[2], c[5]);
                            interp5(newPixels, newIndex + 3, c[2], c[6]);
                            interp5(newPixels, newIndexWidth + 3, c[6], c[5]);
                        }
                        interp6(newPixels, newIndexWidth, c[5], c[4], c[1]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[1]);
                        newPixels[newIndexWidth + 2] = c[5];
                        interp8(newPixels, newIndexWidth2, c[5], c[4]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[4]);
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[9]);
                        interp1(newPixels, newIndexWidth2 + 3, c[5], c[9]);
                        interp8(newPixels, newIndexWidth3, c[5], c[4]);
                        interp3(newPixels, newIndexWidth3 + 1, c[5], c[4]);
                        interp1(newPixels, newIndexWidth3 + 2, c[5], c[9]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[9]);
                        break;
                    }
                    case 217:
                    {
                        interp8(newPixels, newIndex, c[5], c[2]);
                        interp8(newPixels, newIndex + 1, c[5], c[2]);
                        interp6(newPixels, newIndex + 2, c[5], c[2], c[3]);
                        interp8(newPixels, newIndex + 3, c[5], c[3]);
                        interp3(newPixels, newIndexWidth, c[5], c[2]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[2]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[3]);
                        interp1(newPixels, newIndexWidth + 3, c[5], c[3]);
                        interp1(newPixels, newIndexWidth2, c[5], c[7]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[7]);
                        newPixels[newIndexWidth2 + 2] = c[5];
                        if (diff(w[6], w[8]))
                        {
                            newPixels[newIndexWidth2 + 3] = c[5];
                            newPixels[newIndexWidth3 + 2] = c[5];
                            newPixels[newIndexWidth3 + 3] = c[5];
                        }
                        else
                        {
                            interp5(newPixels, newIndexWidth2 + 3, c[6], c[5]);
                            interp5(newPixels, newIndexWidth3 + 2, c[8], c[5]);
                            interp5(newPixels, newIndexWidth3 + 3, c[8], c[6]);
                        }
                        interp8(newPixels, newIndexWidth3, c[5], c[7]);
                        interp1(newPixels, newIndexWidth3 + 1, c[5], c[7]);
                        break;
                    }
                    case 110:
                    {
                        interp8(newPixels, newIndex, c[5], c[1]);
                        interp1(newPixels, newIndex + 1, c[5], c[1]);
                        interp3(newPixels, newIndex + 2, c[5], c[6]);
                        interp8(newPixels, newIndex + 3, c[5], c[6]);
                        interp1(newPixels, newIndexWidth, c[5], c[1]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[1]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[6]);
                        interp8(newPixels, newIndexWidth + 3, c[5], c[6]);
                        if (diff(w[8], w[4]))
                        {
                            newPixels[newIndexWidth2] = c[5];
                            newPixels[newIndexWidth3] = c[5];
                            newPixels[newIndexWidth3 + 1] = c[5];
                        }
                        else
                        {
                            interp5(newPixels, newIndexWidth2, c[4], c[5]);
                            interp5(newPixels, newIndexWidth3, c[8], c[4]);
                            interp5(newPixels, newIndexWidth3 + 1, c[8], c[5]);
                        }
                        newPixels[newIndexWidth2 + 1] = c[5];
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[9]);
                        interp6(newPixels, newIndexWidth2 + 3, c[5], c[6],
                            c[9]);
                        interp1(newPixels, newIndexWidth3 + 2, c[5], c[9]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[9]);
                        break;
                    }
                    case 155:
                    {
                        if (diff(w[4], w[2]))
                        {
                            newPixels[newIndex] = c[5];
                            newPixels[newIndex + 1] = c[5];
                            newPixels[newIndexWidth] = c[5];
                        }
                        else
                        {
                            interp5(newPixels, newIndex, c[2], c[4]);
                            interp5(newPixels, newIndex + 1, c[2], c[5]);
                            interp5(newPixels, newIndexWidth, c[4], c[5]);
                        }
                        interp1(newPixels, newIndex + 2, c[5], c[3]);
                        interp8(newPixels, newIndex + 3, c[5], c[3]);
                        newPixels[newIndexWidth + 1] = c[5];
                        interp3(newPixels, newIndexWidth + 2, c[5], c[3]);
                        interp1(newPixels, newIndexWidth + 3, c[5], c[3]);
                        interp1(newPixels, newIndexWidth2, c[5], c[7]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[7]);
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[8]);
                        interp3(newPixels, newIndexWidth2 + 3, c[5], c[8]);
                        interp8(newPixels, newIndexWidth3, c[5], c[7]);
                        interp6(newPixels, newIndexWidth3 + 1, c[5], c[8], c[7]);
                        interp8(newPixels, newIndexWidth3 + 2, c[5], c[8]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[8]);
                        break;
                    }
                    case 188:
                    {
                        interp8(newPixels, newIndex, c[5], c[1]);
                        interp6(newPixels, newIndex + 1, c[5], c[2], c[1]);
                        interp8(newPixels, newIndex + 2, c[5], c[2]);
                        interp8(newPixels, newIndex + 3, c[5], c[2]);
                        interp1(newPixels, newIndexWidth, c[5], c[1]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[1]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[2]);
                        interp3(newPixels, newIndexWidth + 3, c[5], c[2]);
                        interp3(newPixels, newIndexWidth2, c[5], c[8]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[8]);
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[8]);
                        interp3(newPixels, newIndexWidth2 + 3, c[5], c[8]);
                        interp8(newPixels, newIndexWidth3, c[5], c[8]);
                        interp8(newPixels, newIndexWidth3 + 1, c[5], c[8]);
                        interp8(newPixels, newIndexWidth3 + 2, c[5], c[8]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[8]);
                        break;
                    }
                    case 185:
                    {
                        interp8(newPixels, newIndex, c[5], c[2]);
                        interp8(newPixels, newIndex + 1, c[5], c[2]);
                        interp6(newPixels, newIndex + 2, c[5], c[2], c[3]);
                        interp8(newPixels, newIndex + 3, c[5], c[3]);
                        interp3(newPixels, newIndexWidth, c[5], c[2]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[2]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[3]);
                        interp1(newPixels, newIndexWidth + 3, c[5], c[3]);
                        interp3(newPixels, newIndexWidth2, c[5], c[8]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[8]);
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[8]);
                        interp3(newPixels, newIndexWidth2 + 3, c[5], c[8]);
                        interp8(newPixels, newIndexWidth3, c[5], c[8]);
                        interp8(newPixels, newIndexWidth3 + 1, c[5], c[8]);
                        interp8(newPixels, newIndexWidth3 + 2, c[5], c[8]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[8]);
                        break;
                    }
                    case 61:
                    {
                        interp8(newPixels, newIndex, c[5], c[2]);
                        interp8(newPixels, newIndex + 1, c[5], c[2]);
                        interp8(newPixels, newIndex + 2, c[5], c[2]);
                        interp8(newPixels, newIndex + 3, c[5], c[2]);
                        interp3(newPixels, newIndexWidth, c[5], c[2]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[2]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[2]);
                        interp3(newPixels, newIndexWidth + 3, c[5], c[2]);
                        interp3(newPixels, newIndexWidth2, c[5], c[8]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[8]);
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[9]);
                        interp1(newPixels, newIndexWidth2 + 3, c[5], c[9]);
                        interp8(newPixels, newIndexWidth3, c[5], c[8]);
                        interp8(newPixels, newIndexWidth3 + 1, c[5], c[8]);
                        interp6(newPixels, newIndexWidth3 + 2, c[5], c[8], c[9]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[9]);
                        break;
                    }
                    case 157:
                    {
                        interp8(newPixels, newIndex, c[5], c[2]);
                        interp8(newPixels, newIndex + 1, c[5], c[2]);
                        interp8(newPixels, newIndex + 2, c[5], c[2]);
                        interp8(newPixels, newIndex + 3, c[5], c[2]);
                        interp3(newPixels, newIndexWidth, c[5], c[2]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[2]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[2]);
                        interp3(newPixels, newIndexWidth + 3, c[5], c[2]);
                        interp1(newPixels, newIndexWidth2, c[5], c[7]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[7]);
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[8]);
                        interp3(newPixels, newIndexWidth2 + 3, c[5], c[8]);
                        interp8(newPixels, newIndexWidth3, c[5], c[7]);
                        interp6(newPixels, newIndexWidth3 + 1, c[5], c[8], c[7]);
                        interp8(newPixels, newIndexWidth3 + 2, c[5], c[8]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[8]);
                        break;
                    }
                    case 103:
                    {
                        interp8(newPixels, newIndex, c[5], c[4]);
                        interp3(newPixels, newIndex + 1, c[5], c[4]);
                        interp3(newPixels, newIndex + 2, c[5], c[6]);
                        interp8(newPixels, newIndex + 3, c[5], c[6]);
                        interp8(newPixels, newIndexWidth, c[5], c[4]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[4]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[6]);
                        interp8(newPixels, newIndexWidth + 3, c[5], c[6]);
                        interp8(newPixels, newIndexWidth2, c[5], c[4]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[4]);
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[9]);
                        interp6(newPixels, newIndexWidth2 + 3, c[5], c[6],
                            c[9]);
                        interp8(newPixels, newIndexWidth3, c[5], c[4]);
                        interp3(newPixels, newIndexWidth3 + 1, c[5], c[4]);
                        interp1(newPixels, newIndexWidth3 + 2, c[5], c[9]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[9]);
                        break;
                    }
                    case 227:
                    {
                        interp8(newPixels, newIndex, c[5], c[4]);
                        interp3(newPixels, newIndex + 1, c[5], c[4]);
                        interp1(newPixels, newIndex + 2, c[5], c[3]);
                        interp8(newPixels, newIndex + 3, c[5], c[3]);
                        interp8(newPixels, newIndexWidth, c[5], c[4]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[4]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[3]);
                        interp6(newPixels, newIndexWidth + 3, c[5], c[6], c[3]);
                        interp8(newPixels, newIndexWidth2, c[5], c[4]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[4]);
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[6]);
                        interp8(newPixels, newIndexWidth2 + 3, c[5], c[6]);
                        interp8(newPixels, newIndexWidth3, c[5], c[4]);
                        interp3(newPixels, newIndexWidth3 + 1, c[5], c[4]);
                        interp3(newPixels, newIndexWidth3 + 2, c[5], c[6]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[6]);
                        break;
                    }
                    case 230:
                    {
                        interp8(newPixels, newIndex, c[5], c[1]);
                        interp1(newPixels, newIndex + 1, c[5], c[1]);
                        interp3(newPixels, newIndex + 2, c[5], c[6]);
                        interp8(newPixels, newIndex + 3, c[5], c[6]);
                        interp6(newPixels, newIndexWidth, c[5], c[4], c[1]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[1]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[6]);
                        interp8(newPixels, newIndexWidth + 3, c[5], c[6]);
                        interp8(newPixels, newIndexWidth2, c[5], c[4]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[4]);
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[6]);
                        interp8(newPixels, newIndexWidth2 + 3, c[5], c[6]);
                        interp8(newPixels, newIndexWidth3, c[5], c[4]);
                        interp3(newPixels, newIndexWidth3 + 1, c[5], c[4]);
                        interp3(newPixels, newIndexWidth3 + 2, c[5], c[6]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[6]);
                        break;
                    }
                    case 199:
                    {
                        interp8(newPixels, newIndex, c[5], c[4]);
                        interp3(newPixels, newIndex + 1, c[5], c[4]);
                        interp3(newPixels, newIndex + 2, c[5], c[6]);
                        interp8(newPixels, newIndex + 3, c[5], c[6]);
                        interp8(newPixels, newIndexWidth, c[5], c[4]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[4]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[6]);
                        interp8(newPixels, newIndexWidth + 3, c[5], c[6]);
                        interp6(newPixels, newIndexWidth2, c[5], c[4], c[7]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[7]);
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[6]);
                        interp8(newPixels, newIndexWidth2 + 3, c[5], c[6]);
                        interp8(newPixels, newIndexWidth3, c[5], c[7]);
                        interp1(newPixels, newIndexWidth3 + 1, c[5], c[7]);
                        interp3(newPixels, newIndexWidth3 + 2, c[5], c[6]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[6]);
                        break;
                    }
                    case 220:
                    {
                        interp8(newPixels, newIndex, c[5], c[1]);
                        interp6(newPixels, newIndex + 1, c[5], c[2], c[1]);
                        interp8(newPixels, newIndex + 2, c[5], c[2]);
                        interp8(newPixels, newIndex + 3, c[5], c[2]);
                        interp1(newPixels, newIndexWidth, c[5], c[1]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[1]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[2]);
                        interp3(newPixels, newIndexWidth + 3, c[5], c[2]);
                        if (diff(w[8], w[4]))
                        {
                            interp1(newPixels, newIndexWidth2, c[5], c[7]);
                            interp3(newPixels, newIndexWidth2 + 1, c[5], c[7]);
                            interp8(newPixels, newIndexWidth3, c[5], c[7]);
                            interp1(newPixels, newIndexWidth3 + 1, c[5], c[7]);
                        }
                        else
                        {
                            interp1(newPixels, newIndexWidth2, c[5], c[4]);
                            newPixels[newIndexWidth2 + 1] = c[5];
                            interp2(newPixels, newIndexWidth3, c[5], c[8], c[4]);
                            interp1(newPixels, newIndexWidth3 + 1, c[5], c[8]);
                        }
                        newPixels[newIndexWidth2 + 2] = c[5];
                        if (diff(w[6], w[8]))
                        {
                            newPixels[newIndexWidth2 + 3] = c[5];
                            newPixels[newIndexWidth3 + 2] = c[5];
                            newPixels[newIndexWidth3 + 3] = c[5];
                        }
                        else
                        {
                            interp5(newPixels, newIndexWidth2 + 3, c[6], c[5]);
                            interp5(newPixels, newIndexWidth3 + 2, c[8], c[5]);
                            interp5(newPixels, newIndexWidth3 + 3, c[8], c[6]);
                        }
                        break;
                    }
                    case 158:
                    {
                        if (diff(w[4], w[2]))
                        {
                            interp8(newPixels, newIndex, c[5], c[1]);
                            interp1(newPixels, newIndex + 1, c[5], c[1]);
                            interp1(newPixels, newIndexWidth, c[5], c[1]);
                            interp3(newPixels, newIndexWidth + 1, c[5], c[1]);
                        }
                        else
                        {
                            interp2(newPixels, newIndex, c[5], c[2], c[4]);
                            interp1(newPixels, newIndex + 1, c[5], c[2]);
                            interp1(newPixels, newIndexWidth, c[5], c[4]);
                            newPixels[newIndexWidth + 1] = c[5];
                        }
                        if (diff(w[2], w[6]))
                        {
                            newPixels[newIndex + 2] = c[5];
                            newPixels[newIndex + 3] = c[5];
                            newPixels[newIndexWidth + 3] = c[5];
                        }
                        else
                        {
                            interp5(newPixels, newIndex + 2, c[2], c[5]);
                            interp5(newPixels, newIndex + 3, c[2], c[6]);
                            interp5(newPixels, newIndexWidth + 3, c[6], c[5]);
                        }
                        newPixels[newIndexWidth + 2] = c[5];
                        interp1(newPixels, newIndexWidth2, c[5], c[7]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[7]);
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[8]);
                        interp3(newPixels, newIndexWidth2 + 3, c[5], c[8]);
                        interp8(newPixels, newIndexWidth3, c[5], c[7]);
                        interp6(newPixels, newIndexWidth3 + 1, c[5], c[8], c[7]);
                        interp8(newPixels, newIndexWidth3 + 2, c[5], c[8]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[8]);
                        break;
                    }
                    case 234:
                    {
                        if (diff(w[4], w[2]))
                        {
                            interp8(newPixels, newIndex, c[5], c[1]);
                            interp1(newPixels, newIndex + 1, c[5], c[1]);
                            interp1(newPixels, newIndexWidth, c[5], c[1]);
                            interp3(newPixels, newIndexWidth + 1, c[5], c[1]);
                        }
                        else
                        {
                            interp2(newPixels, newIndex, c[5], c[2], c[4]);
                            interp1(newPixels, newIndex + 1, c[5], c[2]);
                            interp1(newPixels, newIndexWidth, c[5], c[4]);
                            newPixels[newIndexWidth + 1] = c[5];
                        }
                        interp1(newPixels, newIndex + 2, c[5], c[3]);
                        interp8(newPixels, newIndex + 3, c[5], c[3]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[3]);
                        interp6(newPixels, newIndexWidth + 3, c[5], c[6], c[3]);
                        if (diff(w[8], w[4]))
                        {
                            newPixels[newIndexWidth2] = c[5];
                            newPixels[newIndexWidth3] = c[5];
                            newPixels[newIndexWidth3 + 1] = c[5];
                        }
                        else
                        {
                            interp5(newPixels, newIndexWidth2, c[4], c[5]);
                            interp5(newPixels, newIndexWidth3, c[8], c[4]);
                            interp5(newPixels, newIndexWidth3 + 1, c[8], c[5]);
                        }
                        newPixels[newIndexWidth2 + 1] = c[5];
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[6]);
                        interp8(newPixels, newIndexWidth2 + 3, c[5], c[6]);
                        interp3(newPixels, newIndexWidth3 + 2, c[5], c[6]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[6]);
                        break;
                    }
                    case 242:
                    {
                        interp8(newPixels, newIndex, c[5], c[1]);
                        interp1(newPixels, newIndex + 1, c[5], c[1]);
                        if (diff(w[2], w[6]))
                        {
                            interp1(newPixels, newIndex + 2, c[5], c[3]);
                            interp8(newPixels, newIndex + 3, c[5], c[3]);
                            interp3(newPixels, newIndexWidth + 2, c[5], c[3]);
                            interp1(newPixels, newIndexWidth + 3, c[5], c[3]);
                        }
                        else
                        {
                            interp1(newPixels, newIndex + 2, c[5], c[2]);
                            interp2(newPixels, newIndex + 3, c[5], c[2], c[6]);
                            newPixels[newIndexWidth + 2] = c[5];
                            interp1(newPixels, newIndexWidth + 3, c[5], c[6]);
                        }
                        interp6(newPixels, newIndexWidth, c[5], c[4], c[1]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[1]);
                        interp8(newPixels, newIndexWidth2, c[5], c[4]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[4]);
                        newPixels[newIndexWidth2 + 2] = c[5];
                        if (diff(w[6], w[8]))
                        {
                            newPixels[newIndexWidth2 + 3] = c[5];
                            newPixels[newIndexWidth3 + 2] = c[5];
                            newPixels[newIndexWidth3 + 3] = c[5];
                        }
                        else
                        {
                            interp5(newPixels, newIndexWidth2 + 3, c[6], c[5]);
                            interp5(newPixels, newIndexWidth3 + 2, c[8], c[5]);
                            interp5(newPixels, newIndexWidth3 + 3, c[8], c[6]);
                        }
                        interp8(newPixels, newIndexWidth3, c[5], c[4]);
                        interp3(newPixels, newIndexWidth3 + 1, c[5], c[4]);
                        break;
                    }
                    case 59:
                    {
                        if (diff(w[4], w[2]))
                        {
                            newPixels[newIndex] = c[5];
                            newPixels[newIndex + 1] = c[5];
                            newPixels[newIndexWidth] = c[5];
                        }
                        else
                        {
                            interp5(newPixels, newIndex, c[2], c[4]);
                            interp5(newPixels, newIndex + 1, c[2], c[5]);
                            interp5(newPixels, newIndexWidth, c[4], c[5]);
                        }
                        if (diff(w[2], w[6]))
                        {
                            interp1(newPixels, newIndex + 2, c[5], c[3]);
                            interp8(newPixels, newIndex + 3, c[5], c[3]);
                            interp3(newPixels, newIndexWidth + 2, c[5], c[3]);
                            interp1(newPixels, newIndexWidth + 3, c[5], c[3]);
                        }
                        else
                        {
                            interp1(newPixels, newIndex + 2, c[5], c[2]);
                            interp2(newPixels, newIndex + 3, c[5], c[2], c[6]);
                            newPixels[newIndexWidth + 2] = c[5];
                            interp1(newPixels, newIndexWidth + 3, c[5], c[6]);
                        }
                        newPixels[newIndexWidth + 1] = c[5];
                        interp3(newPixels, newIndexWidth2, c[5], c[8]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[8]);
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[9]);
                        interp1(newPixels, newIndexWidth2 + 3, c[5], c[9]);
                        interp8(newPixels, newIndexWidth3, c[5], c[8]);
                        interp8(newPixels, newIndexWidth3 + 1, c[5], c[8]);
                        interp6(newPixels, newIndexWidth3 + 2, c[5], c[8], c[9]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[9]);
                        break;
                    }
                    case 121:
                    {
                        interp8(newPixels, newIndex, c[5], c[2]);
                        interp8(newPixels, newIndex + 1, c[5], c[2]);
                        interp6(newPixels, newIndex + 2, c[5], c[2], c[3]);
                        interp8(newPixels, newIndex + 3, c[5], c[3]);
                        interp3(newPixels, newIndexWidth, c[5], c[2]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[2]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[3]);
                        interp1(newPixels, newIndexWidth + 3, c[5], c[3]);
                        if (diff(w[8], w[4]))
                        {
                            newPixels[newIndexWidth2] = c[5];
                            newPixels[newIndexWidth3] = c[5];
                            newPixels[newIndexWidth3 + 1] = c[5];
                        }
                        else
                        {
                            interp5(newPixels, newIndexWidth2, c[4], c[5]);
                            interp5(newPixels, newIndexWidth3, c[8], c[4]);
                            interp5(newPixels, newIndexWidth3 + 1, c[8], c[5]);
                        }
                        newPixels[newIndexWidth2 + 1] = c[5];
                        if (diff(w[6], w[8]))
                        {
                            interp3(newPixels, newIndexWidth2 + 2, c[5], c[9]);
                            interp1(newPixels, newIndexWidth2 + 3, c[5], c[9]);
                            interp1(newPixels, newIndexWidth3 + 2, c[5], c[9]);
                            interp8(newPixels, newIndexWidth3 + 3, c[5], c[9]);
                        }
                        else
                        {
                            newPixels[newIndexWidth2 + 2] = c[5];
                            interp1(newPixels, newIndexWidth2 + 3, c[5], c[6]);
                            interp1(newPixels, newIndexWidth3 + 2, c[5], c[8]);
                            interp2(newPixels, newIndexWidth3 + 3, c[5], c[8],
                                c[6]);
                        }
                        break;
                    }
                    case 87:
                    {
                        interp8(newPixels, newIndex, c[5], c[4]);
                        interp3(newPixels, newIndex + 1, c[5], c[4]);
                        if (diff(w[2], w[6]))
                        {
                            newPixels[newIndex + 2] = c[5];
                            newPixels[newIndex + 3] = c[5];
                            newPixels[newIndexWidth + 3] = c[5];
                        }
                        else
                        {
                            interp5(newPixels, newIndex + 2, c[2], c[5]);
                            interp5(newPixels, newIndex + 3, c[2], c[6]);
                            interp5(newPixels, newIndexWidth + 3, c[6], c[5]);
                        }
                        interp8(newPixels, newIndexWidth, c[5], c[4]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[4]);
                        newPixels[newIndexWidth + 2] = c[5];
                        interp6(newPixels, newIndexWidth2, c[5], c[4], c[7]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[7]);
                        if (diff(w[6], w[8]))
                        {
                            interp3(newPixels, newIndexWidth2 + 2, c[5], c[9]);
                            interp1(newPixels, newIndexWidth2 + 3, c[5], c[9]);
                            interp1(newPixels, newIndexWidth3 + 2, c[5], c[9]);
                            interp8(newPixels, newIndexWidth3 + 3, c[5], c[9]);
                        }
                        else
                        {
                            newPixels[newIndexWidth2 + 2] = c[5];
                            interp1(newPixels, newIndexWidth2 + 3, c[5], c[6]);
                            interp1(newPixels, newIndexWidth3 + 2, c[5], c[8]);
                            interp2(newPixels, newIndexWidth3 + 3, c[5], c[8],
                                c[6]);
                        }
                        interp8(newPixels, newIndexWidth3, c[5], c[7]);
                        interp1(newPixels, newIndexWidth3 + 1, c[5], c[7]);
                        break;
                    }
                    case 79:
                    {
                        if (diff(w[4], w[2]))
                        {
                            newPixels[newIndex] = c[5];
                            newPixels[newIndex + 1] = c[5];
                            newPixels[newIndexWidth] = c[5];
                        }
                        else
                        {
                            interp5(newPixels, newIndex, c[2], c[4]);
                            interp5(newPixels, newIndex + 1, c[2], c[5]);
                            interp5(newPixels, newIndexWidth, c[4], c[5]);
                        }
                        interp3(newPixels, newIndex + 2, c[5], c[6]);
                        interp8(newPixels, newIndex + 3, c[5], c[6]);
                        newPixels[newIndexWidth + 1] = c[5];
                        interp3(newPixels, newIndexWidth + 2, c[5], c[6]);
                        interp8(newPixels, newIndexWidth + 3, c[5], c[6]);
                        if (diff(w[8], w[4]))
                        {
                            interp1(newPixels, newIndexWidth2, c[5], c[7]);
                            interp3(newPixels, newIndexWidth2 + 1, c[5], c[7]);
                            interp8(newPixels, newIndexWidth3, c[5], c[7]);
                            interp1(newPixels, newIndexWidth3 + 1, c[5], c[7]);
                        }
                        else
                        {
                            interp1(newPixels, newIndexWidth2, c[5], c[4]);
                            newPixels[newIndexWidth2 + 1] = c[5];
                            interp2(newPixels, newIndexWidth3, c[5], c[8], c[4]);
                            interp1(newPixels, newIndexWidth3 + 1, c[5], c[8]);
                        }
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[9]);
                        interp6(newPixels, newIndexWidth2 + 3, c[5], c[6],
                            c[9]);
                        interp1(newPixels, newIndexWidth3 + 2, c[5], c[9]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[9]);
                        break;
                    }
                    case 122:
                    {
                        if (diff(w[4], w[2]))
                        {
                            interp8(newPixels, newIndex, c[5], c[1]);
                            interp1(newPixels, newIndex + 1, c[5], c[1]);
                            interp1(newPixels, newIndexWidth, c[5], c[1]);
                            interp3(newPixels, newIndexWidth + 1, c[5], c[1]);
                        }
                        else
                        {
                            interp2(newPixels, newIndex, c[5], c[2], c[4]);
                            interp1(newPixels, newIndex + 1, c[5], c[2]);
                            interp1(newPixels, newIndexWidth, c[5], c[4]);
                            newPixels[newIndexWidth + 1] = c[5];
                        }
                        if (diff(w[2], w[6]))
                        {
                            interp1(newPixels, newIndex + 2, c[5], c[3]);
                            interp8(newPixels, newIndex + 3, c[5], c[3]);
                            interp3(newPixels, newIndexWidth + 2, c[5], c[3]);
                            interp1(newPixels, newIndexWidth + 3, c[5], c[3]);
                        }
                        else
                        {
                            interp1(newPixels, newIndex + 2, c[5], c[2]);
                            interp2(newPixels, newIndex + 3, c[5], c[2], c[6]);
                            newPixels[newIndexWidth + 2] = c[5];
                            interp1(newPixels, newIndexWidth + 3, c[5], c[6]);
                        }
                        if (diff(w[8], w[4]))
                        {
                            newPixels[newIndexWidth2] = c[5];
                            newPixels[newIndexWidth3] = c[5];
                            newPixels[newIndexWidth3 + 1] = c[5];
                        }
                        else
                        {
                            interp5(newPixels, newIndexWidth2, c[4], c[5]);
                            interp5(newPixels, newIndexWidth3, c[8], c[4]);
                            interp5(newPixels, newIndexWidth3 + 1, c[8], c[5]);
                        }
                        newPixels[newIndexWidth2 + 1] = c[5];
                        if (diff(w[6], w[8]))
                        {
                            interp3(newPixels, newIndexWidth2 + 2, c[5], c[9]);
                            interp1(newPixels, newIndexWidth2 + 3, c[5], c[9]);
                            interp1(newPixels, newIndexWidth3 + 2, c[5], c[9]);
                            interp8(newPixels, newIndexWidth3 + 3, c[5], c[9]);
                        }
                        else
                        {
                            newPixels[newIndexWidth2 + 2] = c[5];
                            interp1(newPixels, newIndexWidth2 + 3, c[5], c[6]);
                            interp1(newPixels, newIndexWidth3 + 2, c[5], c[8]);
                            interp2(newPixels, newIndexWidth3 + 3, c[5], c[8],
                                c[6]);
                        }
                        break;
                    }
                    case 94:
                    {
                        if (diff(w[4], w[2]))
                        {
                            interp8(newPixels, newIndex, c[5], c[1]);
                            interp1(newPixels, newIndex + 1, c[5], c[1]);
                            interp1(newPixels, newIndexWidth, c[5], c[1]);
                            interp3(newPixels, newIndexWidth + 1, c[5], c[1]);
                        }
                        else
                        {
                            interp2(newPixels, newIndex, c[5], c[2], c[4]);
                            interp1(newPixels, newIndex + 1, c[5], c[2]);
                            interp1(newPixels, newIndexWidth, c[5], c[4]);
                            newPixels[newIndexWidth + 1] = c[5];
                        }
                        if (diff(w[2], w[6]))
                        {
                            newPixels[newIndex + 2] = c[5];
                            newPixels[newIndex + 3] = c[5];
                            newPixels[newIndexWidth + 3] = c[5];
                        }
                        else
                        {
                            interp5(newPixels, newIndex + 2, c[2], c[5]);
                            interp5(newPixels, newIndex + 3, c[2], c[6]);
                            interp5(newPixels, newIndexWidth + 3, c[6], c[5]);
                        }
                        newPixels[newIndexWidth + 2] = c[5];
                        if (diff(w[8], w[4]))
                        {
                            interp1(newPixels, newIndexWidth2, c[5], c[7]);
                            interp3(newPixels, newIndexWidth2 + 1, c[5], c[7]);
                            interp8(newPixels, newIndexWidth3, c[5], c[7]);
                            interp1(newPixels, newIndexWidth3 + 1, c[5], c[7]);
                        }
                        else
                        {
                            interp1(newPixels, newIndexWidth2, c[5], c[4]);
                            newPixels[newIndexWidth2 + 1] = c[5];
                            interp2(newPixels, newIndexWidth3, c[5], c[8], c[4]);
                            interp1(newPixels, newIndexWidth3 + 1, c[5], c[8]);
                        }
                        if (diff(w[6], w[8]))
                        {
                            interp3(newPixels, newIndexWidth2 + 2, c[5], c[9]);
                            interp1(newPixels, newIndexWidth2 + 3, c[5], c[9]);
                            interp1(newPixels, newIndexWidth3 + 2, c[5], c[9]);
                            interp8(newPixels, newIndexWidth3 + 3, c[5], c[9]);
                        }
                        else
                        {
                            newPixels[newIndexWidth2 + 2] = c[5];
                            interp1(newPixels, newIndexWidth2 + 3, c[5], c[6]);
                            interp1(newPixels, newIndexWidth3 + 2, c[5], c[8]);
                            interp2(newPixels, newIndexWidth3 + 3, c[5], c[8],
                                c[6]);
                        }
                        break;
                    }
                    case 218:
                    {
                        if (diff(w[4], w[2]))
                        {
                            interp8(newPixels, newIndex, c[5], c[1]);
                            interp1(newPixels, newIndex + 1, c[5], c[1]);
                            interp1(newPixels, newIndexWidth, c[5], c[1]);
                            interp3(newPixels, newIndexWidth + 1, c[5], c[1]);
                        }
                        else
                        {
                            interp2(newPixels, newIndex, c[5], c[2], c[4]);
                            interp1(newPixels, newIndex + 1, c[5], c[2]);
                            interp1(newPixels, newIndexWidth, c[5], c[4]);
                            newPixels[newIndexWidth + 1] = c[5];
                        }
                        if (diff(w[2], w[6]))
                        {
                            interp1(newPixels, newIndex + 2, c[5], c[3]);
                            interp8(newPixels, newIndex + 3, c[5], c[3]);
                            interp3(newPixels, newIndexWidth + 2, c[5], c[3]);
                            interp1(newPixels, newIndexWidth + 3, c[5], c[3]);
                        }
                        else
                        {
                            interp1(newPixels, newIndex + 2, c[5], c[2]);
                            interp2(newPixels, newIndex + 3, c[5], c[2], c[6]);
                            newPixels[newIndexWidth + 2] = c[5];
                            interp1(newPixels, newIndexWidth + 3, c[5], c[6]);
                        }
                        if (diff(w[8], w[4]))
                        {
                            interp1(newPixels, newIndexWidth2, c[5], c[7]);
                            interp3(newPixels, newIndexWidth2 + 1, c[5], c[7]);
                            interp8(newPixels, newIndexWidth3, c[5], c[7]);
                            interp1(newPixels, newIndexWidth3 + 1, c[5], c[7]);
                        }
                        else
                        {
                            interp1(newPixels, newIndexWidth2, c[5], c[4]);
                            newPixels[newIndexWidth2 + 1] = c[5];
                            interp2(newPixels, newIndexWidth3, c[5], c[8], c[4]);
                            interp1(newPixels, newIndexWidth3 + 1, c[5], c[8]);
                        }
                        newPixels[newIndexWidth2 + 2] = c[5];
                        if (diff(w[6], w[8]))
                        {
                            newPixels[newIndexWidth2 + 3] = c[5];
                            newPixels[newIndexWidth3 + 2] = c[5];
                            newPixels[newIndexWidth3 + 3] = c[5];
                        }
                        else
                        {
                            interp5(newPixels, newIndexWidth2 + 3, c[6], c[5]);
                            interp5(newPixels, newIndexWidth3 + 2, c[8], c[5]);
                            interp5(newPixels, newIndexWidth3 + 3, c[8], c[6]);
                        }
                        break;
                    }
                    case 91:
                    {
                        if (diff(w[4], w[2]))
                        {
                            newPixels[newIndex] = c[5];
                            newPixels[newIndex + 1] = c[5];
                            newPixels[newIndexWidth] = c[5];
                        }
                        else
                        {
                            interp5(newPixels, newIndex, c[2], c[4]);
                            interp5(newPixels, newIndex + 1, c[2], c[5]);
                            interp5(newPixels, newIndexWidth, c[4], c[5]);
                        }
                        if (diff(w[2], w[6]))
                        {
                            interp1(newPixels, newIndex + 2, c[5], c[3]);
                            interp8(newPixels, newIndex + 3, c[5], c[3]);
                            interp3(newPixels, newIndexWidth + 2, c[5], c[3]);
                            interp1(newPixels, newIndexWidth + 3, c[5], c[3]);
                        }
                        else
                        {
                            interp1(newPixels, newIndex + 2, c[5], c[2]);
                            interp2(newPixels, newIndex + 3, c[5], c[2], c[6]);
                            newPixels[newIndexWidth + 2] = c[5];
                            interp1(newPixels, newIndexWidth + 3, c[5], c[6]);
                        }
                        newPixels[newIndexWidth + 1] = c[5];
                        if (diff(w[8], w[4]))
                        {
                            interp1(newPixels, newIndexWidth2, c[5], c[7]);
                            interp3(newPixels, newIndexWidth2 + 1, c[5], c[7]);
                            interp8(newPixels, newIndexWidth3, c[5], c[7]);
                            interp1(newPixels, newIndexWidth3 + 1, c[5], c[7]);
                        }
                        else
                        {
                            interp1(newPixels, newIndexWidth2, c[5], c[4]);
                            newPixels[newIndexWidth2 + 1] = c[5];
                            interp2(newPixels, newIndexWidth3, c[5], c[8], c[4]);
                            interp1(newPixels, newIndexWidth3 + 1, c[5], c[8]);
                        }
                        if (diff(w[6], w[8]))
                        {
                            interp3(newPixels, newIndexWidth2 + 2, c[5], c[9]);
                            interp1(newPixels, newIndexWidth2 + 3, c[5], c[9]);
                            interp1(newPixels, newIndexWidth3 + 2, c[5], c[9]);
                            interp8(newPixels, newIndexWidth3 + 3, c[5], c[9]);
                        }
                        else
                        {
                            newPixels[newIndexWidth2 + 2] = c[5];
                            interp1(newPixels, newIndexWidth2 + 3, c[5], c[6]);
                            interp1(newPixels, newIndexWidth3 + 2, c[5], c[8]);
                            interp2(newPixels, newIndexWidth3 + 3, c[5], c[8],
                                c[6]);
                        }
                        break;
                    }
                    case 229:
                    {
                        interp2(newPixels, newIndex, c[5], c[2], c[4]);
                        interp6(newPixels, newIndex + 1, c[5], c[2], c[4]);
                        interp6(newPixels, newIndex + 2, c[5], c[2], c[6]);
                        interp2(newPixels, newIndex + 3, c[5], c[2], c[6]);
                        interp6(newPixels, newIndexWidth, c[5], c[4], c[2]);
                        interp7(newPixels, newIndexWidth + 1, c[5], c[4], c[2]);
                        interp7(newPixels, newIndexWidth + 2, c[5], c[6], c[2]);
                        interp6(newPixels, newIndexWidth + 3, c[5], c[6], c[2]);
                        interp8(newPixels, newIndexWidth2, c[5], c[4]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[4]);
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[6]);
                        interp8(newPixels, newIndexWidth2 + 3, c[5], c[6]);
                        interp8(newPixels, newIndexWidth3, c[5], c[4]);
                        interp3(newPixels, newIndexWidth3 + 1, c[5], c[4]);
                        interp3(newPixels, newIndexWidth3 + 2, c[5], c[6]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[6]);
                        break;
                    }
                    case 167:
                    {
                        interp8(newPixels, newIndex, c[5], c[4]);
                        interp3(newPixels, newIndex + 1, c[5], c[4]);
                        interp3(newPixels, newIndex + 2, c[5], c[6]);
                        interp8(newPixels, newIndex + 3, c[5], c[6]);
                        interp8(newPixels, newIndexWidth, c[5], c[4]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[4]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[6]);
                        interp8(newPixels, newIndexWidth + 3, c[5], c[6]);
                        interp6(newPixels, newIndexWidth2, c[5], c[4], c[8]);
                        interp7(newPixels, newIndexWidth2 + 1, c[5], c[4], c[8]);
                        interp7(newPixels, newIndexWidth2 + 2, c[5], c[6], c[8]);
                        interp6(newPixels, newIndexWidth2 + 3, c[5], c[6],
                            c[8]);
                        interp2(newPixels, newIndexWidth3, c[5], c[8], c[4]);
                        interp6(newPixels, newIndexWidth3 + 1, c[5], c[8], c[4]);
                        interp6(newPixels, newIndexWidth3 + 2, c[5], c[8], c[6]);
                        interp2(newPixels, newIndexWidth3 + 3, c[5], c[8],
                            c[6]);
                        break;
                    }
                    case 173:
                    {
                        interp8(newPixels, newIndex, c[5], c[2]);
                        interp8(newPixels, newIndex + 1, c[5], c[2]);
                        interp6(newPixels, newIndex + 2, c[5], c[2], c[6]);
                        interp2(newPixels, newIndex + 3, c[5], c[2], c[6]);
                        interp3(newPixels, newIndexWidth, c[5], c[2]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[2]);
                        interp7(newPixels, newIndexWidth + 2, c[5], c[6], c[2]);
                        interp6(newPixels, newIndexWidth + 3, c[5], c[6], c[2]);
                        interp3(newPixels, newIndexWidth2, c[5], c[8]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[8]);
                        interp7(newPixels, newIndexWidth2 + 2, c[5], c[6], c[8]);
                        interp6(newPixels, newIndexWidth2 + 3, c[5], c[6],
                            c[8]);
                        interp8(newPixels, newIndexWidth3, c[5], c[8]);
                        interp8(newPixels, newIndexWidth3 + 1, c[5], c[8]);
                        interp6(newPixels, newIndexWidth3 + 2, c[5], c[8], c[6]);
                        interp2(newPixels, newIndexWidth3 + 3, c[5], c[8],
                            c[6]);
                        break;
                    }
                    case 181:
                    {
                        interp2(newPixels, newIndex, c[5], c[2], c[4]);
                        interp6(newPixels, newIndex + 1, c[5], c[2], c[4]);
                        interp8(newPixels, newIndex + 2, c[5], c[2]);
                        interp8(newPixels, newIndex + 3, c[5], c[2]);
                        interp6(newPixels, newIndexWidth, c[5], c[4], c[2]);
                        interp7(newPixels, newIndexWidth + 1, c[5], c[4], c[2]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[2]);
                        interp3(newPixels, newIndexWidth + 3, c[5], c[2]);
                        interp6(newPixels, newIndexWidth2, c[5], c[4], c[8]);
                        interp7(newPixels, newIndexWidth2 + 1, c[5], c[4], c[8]);
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[8]);
                        interp3(newPixels, newIndexWidth2 + 3, c[5], c[8]);
                        interp2(newPixels, newIndexWidth3, c[5], c[8], c[4]);
                        interp6(newPixels, newIndexWidth3 + 1, c[5], c[8], c[4]);
                        interp8(newPixels, newIndexWidth3 + 2, c[5], c[8]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[8]);
                        break;
                    }
                    case 186:
                    {
                        if (diff(w[4], w[2]))
                        {
                            interp8(newPixels, newIndex, c[5], c[1]);
                            interp1(newPixels, newIndex + 1, c[5], c[1]);
                            interp1(newPixels, newIndexWidth, c[5], c[1]);
                            interp3(newPixels, newIndexWidth + 1, c[5], c[1]);
                        }
                        else
                        {
                            interp2(newPixels, newIndex, c[5], c[2], c[4]);
                            interp1(newPixels, newIndex + 1, c[5], c[2]);
                            interp1(newPixels, newIndexWidth, c[5], c[4]);
                            newPixels[newIndexWidth + 1] = c[5];
                        }
                        if (diff(w[2], w[6]))
                        {
                            interp1(newPixels, newIndex + 2, c[5], c[3]);
                            interp8(newPixels, newIndex + 3, c[5], c[3]);
                            interp3(newPixels, newIndexWidth + 2, c[5], c[3]);
                            interp1(newPixels, newIndexWidth + 3, c[5], c[3]);
                        }
                        else
                        {
                            interp1(newPixels, newIndex + 2, c[5], c[2]);
                            interp2(newPixels, newIndex + 3, c[5], c[2], c[6]);
                            newPixels[newIndexWidth + 2] = c[5];
                            interp1(newPixels, newIndexWidth + 3, c[5], c[6]);
                        }
                        interp3(newPixels, newIndexWidth2, c[5], c[8]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[8]);
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[8]);
                        interp3(newPixels, newIndexWidth2 + 3, c[5], c[8]);
                        interp8(newPixels, newIndexWidth3, c[5], c[8]);
                        interp8(newPixels, newIndexWidth3 + 1, c[5], c[8]);
                        interp8(newPixels, newIndexWidth3 + 2, c[5], c[8]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[8]);
                        break;
                    }
                    case 115:
                    {
                        interp8(newPixels, newIndex, c[5], c[4]);
                        interp3(newPixels, newIndex + 1, c[5], c[4]);
                        if (diff(w[2], w[6]))
                        {
                            interp1(newPixels, newIndex + 2, c[5], c[3]);
                            interp8(newPixels, newIndex + 3, c[5], c[3]);
                            interp3(newPixels, newIndexWidth + 2, c[5], c[3]);
                            interp1(newPixels, newIndexWidth + 3, c[5], c[3]);
                        }
                        else
                        {
                            interp1(newPixels, newIndex + 2, c[5], c[2]);
                            interp2(newPixels, newIndex + 3, c[5], c[2], c[6]);
                            newPixels[newIndexWidth + 2] = c[5];
                            interp1(newPixels, newIndexWidth + 3, c[5], c[6]);
                        }
                        interp8(newPixels, newIndexWidth, c[5], c[4]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[4]);
                        interp8(newPixels, newIndexWidth2, c[5], c[4]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[4]);
                        if (diff(w[6], w[8]))
                        {
                            interp3(newPixels, newIndexWidth2 + 2, c[5], c[9]);
                            interp1(newPixels, newIndexWidth2 + 3, c[5], c[9]);
                            interp1(newPixels, newIndexWidth3 + 2, c[5], c[9]);
                            interp8(newPixels, newIndexWidth3 + 3, c[5], c[9]);
                        }
                        else
                        {
                            newPixels[newIndexWidth2 + 2] = c[5];
                            interp1(newPixels, newIndexWidth2 + 3, c[5], c[6]);
                            interp1(newPixels, newIndexWidth3 + 2, c[5], c[8]);
                            interp2(newPixels, newIndexWidth3 + 3, c[5], c[8],
                                c[6]);
                        }
                        interp8(newPixels, newIndexWidth3, c[5], c[4]);
                        interp3(newPixels, newIndexWidth3 + 1, c[5], c[4]);
                        break;
                    }
                    case 93:
                    {
                        interp8(newPixels, newIndex, c[5], c[2]);
                        interp8(newPixels, newIndex + 1, c[5], c[2]);
                        interp8(newPixels, newIndex + 2, c[5], c[2]);
                        interp8(newPixels, newIndex + 3, c[5], c[2]);
                        interp3(newPixels, newIndexWidth, c[5], c[2]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[2]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[2]);
                        interp3(newPixels, newIndexWidth + 3, c[5], c[2]);
                        if (diff(w[8], w[4]))
                        {
                            interp1(newPixels, newIndexWidth2, c[5], c[7]);
                            interp3(newPixels, newIndexWidth2 + 1, c[5], c[7]);
                            interp8(newPixels, newIndexWidth3, c[5], c[7]);
                            interp1(newPixels, newIndexWidth3 + 1, c[5], c[7]);
                        }
                        else
                        {
                            interp1(newPixels, newIndexWidth2, c[5], c[4]);
                            newPixels[newIndexWidth2 + 1] = c[5];
                            interp2(newPixels, newIndexWidth3, c[5], c[8], c[4]);
                            interp1(newPixels, newIndexWidth3 + 1, c[5], c[8]);
                        }
                        if (diff(w[6], w[8]))
                        {
                            interp3(newPixels, newIndexWidth2 + 2, c[5], c[9]);
                            interp1(newPixels, newIndexWidth2 + 3, c[5], c[9]);
                            interp1(newPixels, newIndexWidth3 + 2, c[5], c[9]);
                            interp8(newPixels, newIndexWidth3 + 3, c[5], c[9]);
                        }
                        else
                        {
                            newPixels[newIndexWidth2 + 2] = c[5];
                            interp1(newPixels, newIndexWidth2 + 3, c[5], c[6]);
                            interp1(newPixels, newIndexWidth3 + 2, c[5], c[8]);
                            interp2(newPixels, newIndexWidth3 + 3, c[5], c[8],
                                c[6]);
                        }
                        break;
                    }
                    case 206:
                    {
                        if (diff(w[4], w[2]))
                        {
                            interp8(newPixels, newIndex, c[5], c[1]);
                            interp1(newPixels, newIndex + 1, c[5], c[1]);
                            interp1(newPixels, newIndexWidth, c[5], c[1]);
                            interp3(newPixels, newIndexWidth + 1, c[5], c[1]);
                        }
                        else
                        {
                            interp2(newPixels, newIndex, c[5], c[2], c[4]);
                            interp1(newPixels, newIndex + 1, c[5], c[2]);
                            interp1(newPixels, newIndexWidth, c[5], c[4]);
                            newPixels[newIndexWidth + 1] = c[5];
                        }
                        interp3(newPixels, newIndex + 2, c[5], c[6]);
                        interp8(newPixels, newIndex + 3, c[5], c[6]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[6]);
                        interp8(newPixels, newIndexWidth + 3, c[5], c[6]);
                        if (diff(w[8], w[4]))
                        {
                            interp1(newPixels, newIndexWidth2, c[5], c[7]);
                            interp3(newPixels, newIndexWidth2 + 1, c[5], c[7]);
                            interp8(newPixels, newIndexWidth3, c[5], c[7]);
                            interp1(newPixels, newIndexWidth3 + 1, c[5], c[7]);
                        }
                        else
                        {
                            interp1(newPixels, newIndexWidth2, c[5], c[4]);
                            newPixels[newIndexWidth2 + 1] = c[5];
                            interp2(newPixels, newIndexWidth3, c[5], c[8], c[4]);
                            interp1(newPixels, newIndexWidth3 + 1, c[5], c[8]);
                        }
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[6]);
                        interp8(newPixels, newIndexWidth2 + 3, c[5], c[6]);
                        interp3(newPixels, newIndexWidth3 + 2, c[5], c[6]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[6]);
                        break;
                    }
                    case 205:
                    case 201:
                    {
                        interp8(newPixels, newIndex, c[5], c[2]);
                        interp8(newPixels, newIndex + 1, c[5], c[2]);
                        interp6(newPixels, newIndex + 2, c[5], c[2], c[6]);
                        interp2(newPixels, newIndex + 3, c[5], c[2], c[6]);
                        interp3(newPixels, newIndexWidth, c[5], c[2]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[2]);
                        interp7(newPixels, newIndexWidth + 2, c[5], c[6], c[2]);
                        interp6(newPixels, newIndexWidth + 3, c[5], c[6], c[2]);
                        if (diff(w[8], w[4]))
                        {
                            interp1(newPixels, newIndexWidth2, c[5], c[7]);
                            interp3(newPixels, newIndexWidth2 + 1, c[5], c[7]);
                            interp8(newPixels, newIndexWidth3, c[5], c[7]);
                            interp1(newPixels, newIndexWidth3 + 1, c[5], c[7]);
                        }
                        else
                        {
                            interp1(newPixels, newIndexWidth2, c[5], c[4]);
                            newPixels[newIndexWidth2 + 1] = c[5];
                            interp2(newPixels, newIndexWidth3, c[5], c[8], c[4]);
                            interp1(newPixels, newIndexWidth3 + 1, c[5], c[8]);
                        }
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[6]);
                        interp8(newPixels, newIndexWidth2 + 3, c[5], c[6]);
                        interp3(newPixels, newIndexWidth3 + 2, c[5], c[6]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[6]);
                        break;
                    }
                    case 174:
                    case 46:
                    {
                        if (diff(w[4], w[2]))
                        {
                            interp8(newPixels, newIndex, c[5], c[1]);
                            interp1(newPixels, newIndex + 1, c[5], c[1]);
                            interp1(newPixels, newIndexWidth, c[5], c[1]);
                            interp3(newPixels, newIndexWidth + 1, c[5], c[1]);
                        }
                        else
                        {
                            interp2(newPixels, newIndex, c[5], c[2], c[4]);
                            interp1(newPixels, newIndex + 1, c[5], c[2]);
                            interp1(newPixels, newIndexWidth, c[5], c[4]);
                            newPixels[newIndexWidth + 1] = c[5];
                        }
                        interp3(newPixels, newIndex + 2, c[5], c[6]);
                        interp8(newPixels, newIndex + 3, c[5], c[6]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[6]);
                        interp8(newPixels, newIndexWidth + 3, c[5], c[6]);
                        interp3(newPixels, newIndexWidth2, c[5], c[8]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[8]);
                        interp7(newPixels, newIndexWidth2 + 2, c[5], c[6], c[8]);
                        interp6(newPixels, newIndexWidth2 + 3, c[5], c[6],
                            c[8]);
                        interp8(newPixels, newIndexWidth3, c[5], c[8]);
                        interp8(newPixels, newIndexWidth3 + 1, c[5], c[8]);
                        interp6(newPixels, newIndexWidth3 + 2, c[5], c[8], c[6]);
                        interp2(newPixels, newIndexWidth3 + 3, c[5], c[8],
                            c[6]);
                        break;
                    }
                    case 179:
                    case 147:
                    {
                        interp8(newPixels, newIndex, c[5], c[4]);
                        interp3(newPixels, newIndex + 1, c[5], c[4]);
                        if (diff(w[2], w[6]))
                        {
                            interp1(newPixels, newIndex + 2, c[5], c[3]);
                            interp8(newPixels, newIndex + 3, c[5], c[3]);
                            interp3(newPixels, newIndexWidth + 2, c[5], c[3]);
                            interp1(newPixels, newIndexWidth + 3, c[5], c[3]);
                        }
                        else
                        {
                            interp1(newPixels, newIndex + 2, c[5], c[2]);
                            interp2(newPixels, newIndex + 3, c[5], c[2], c[6]);
                            newPixels[newIndexWidth + 2] = c[5];
                            interp1(newPixels, newIndexWidth + 3, c[5], c[6]);
                        }
                        interp8(newPixels, newIndexWidth, c[5], c[4]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[4]);
                        interp6(newPixels, newIndexWidth2, c[5], c[4], c[8]);
                        interp7(newPixels, newIndexWidth2 + 1, c[5], c[4], c[8]);
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[8]);
                        interp3(newPixels, newIndexWidth2 + 3, c[5], c[8]);
                        interp2(newPixels, newIndexWidth3, c[5], c[8], c[4]);
                        interp6(newPixels, newIndexWidth3 + 1, c[5], c[8], c[4]);
                        interp8(newPixels, newIndexWidth3 + 2, c[5], c[8]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[8]);
                        break;
                    }
                    case 117:
                    case 116:
                    {
                        interp2(newPixels, newIndex, c[5], c[2], c[4]);
                        interp6(newPixels, newIndex + 1, c[5], c[2], c[4]);
                        interp8(newPixels, newIndex + 2, c[5], c[2]);
                        interp8(newPixels, newIndex + 3, c[5], c[2]);
                        interp6(newPixels, newIndexWidth, c[5], c[4], c[2]);
                        interp7(newPixels, newIndexWidth + 1, c[5], c[4], c[2]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[2]);
                        interp3(newPixels, newIndexWidth + 3, c[5], c[2]);
                        interp8(newPixels, newIndexWidth2, c[5], c[4]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[4]);
                        if (diff(w[6], w[8]))
                        {
                            interp3(newPixels, newIndexWidth2 + 2, c[5], c[9]);
                            interp1(newPixels, newIndexWidth2 + 3, c[5], c[9]);
                            interp1(newPixels, newIndexWidth3 + 2, c[5], c[9]);
                            interp8(newPixels, newIndexWidth3 + 3, c[5], c[9]);
                        }
                        else
                        {
                            newPixels[newIndexWidth2 + 2] = c[5];
                            interp1(newPixels, newIndexWidth2 + 3, c[5], c[6]);
                            interp1(newPixels, newIndexWidth3 + 2, c[5], c[8]);
                            interp2(newPixels, newIndexWidth3 + 3, c[5], c[8],
                                c[6]);
                        }
                        interp8(newPixels, newIndexWidth3, c[5], c[4]);
                        interp3(newPixels, newIndexWidth3 + 1, c[5], c[4]);
                        break;
                    }
                    case 189:
                    {
                        interp8(newPixels, newIndex, c[5], c[2]);
                        interp8(newPixels, newIndex + 1, c[5], c[2]);
                        interp8(newPixels, newIndex + 2, c[5], c[2]);
                        interp8(newPixels, newIndex + 3, c[5], c[2]);
                        interp3(newPixels, newIndexWidth, c[5], c[2]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[2]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[2]);
                        interp3(newPixels, newIndexWidth + 3, c[5], c[2]);
                        interp3(newPixels, newIndexWidth2, c[5], c[8]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[8]);
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[8]);
                        interp3(newPixels, newIndexWidth2 + 3, c[5], c[8]);
                        interp8(newPixels, newIndexWidth3, c[5], c[8]);
                        interp8(newPixels, newIndexWidth3 + 1, c[5], c[8]);
                        interp8(newPixels, newIndexWidth3 + 2, c[5], c[8]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[8]);
                        break;
                    }
                    case 231:
                    {
                        interp8(newPixels, newIndex, c[5], c[4]);
                        interp3(newPixels, newIndex + 1, c[5], c[4]);
                        interp3(newPixels, newIndex + 2, c[5], c[6]);
                        interp8(newPixels, newIndex + 3, c[5], c[6]);
                        interp8(newPixels, newIndexWidth, c[5], c[4]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[4]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[6]);
                        interp8(newPixels, newIndexWidth + 3, c[5], c[6]);
                        interp8(newPixels, newIndexWidth2, c[5], c[4]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[4]);
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[6]);
                        interp8(newPixels, newIndexWidth2 + 3, c[5], c[6]);
                        interp8(newPixels, newIndexWidth3, c[5], c[4]);
                        interp3(newPixels, newIndexWidth3 + 1, c[5], c[4]);
                        interp3(newPixels, newIndexWidth3 + 2, c[5], c[6]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[6]);
                        break;
                    }
                    case 126:
                    {
                        interp8(newPixels, newIndex, c[5], c[1]);
                        interp1(newPixels, newIndex + 1, c[5], c[1]);
                        if (diff(w[2], w[6]))
                        {
                            newPixels[newIndex + 2] = c[5];
                            newPixels[newIndex + 3] = c[5];
                            newPixels[newIndexWidth + 3] = c[5];
                        }
                        else
                        {
                            interp5(newPixels, newIndex + 2, c[2], c[5]);
                            interp5(newPixels, newIndex + 3, c[2], c[6]);
                            interp5(newPixels, newIndexWidth + 3, c[6], c[5]);
                        }
                        interp1(newPixels, newIndexWidth, c[5], c[1]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[1]);
                        newPixels[newIndexWidth + 2] = c[5];
                        if (diff(w[8], w[4]))
                        {
                            newPixels[newIndexWidth2] = c[5];
                            newPixels[newIndexWidth3] = c[5];
                            newPixels[newIndexWidth3 + 1] = c[5];
                        }
                        else
                        {
                            interp5(newPixels, newIndexWidth2, c[4], c[5]);
                            interp5(newPixels, newIndexWidth3, c[8], c[4]);
                            interp5(newPixels, newIndexWidth3 + 1, c[8], c[5]);
                        }
                        newPixels[newIndexWidth2 + 1] = c[5];
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[9]);
                        interp1(newPixels, newIndexWidth2 + 3, c[5], c[9]);
                        interp1(newPixels, newIndexWidth3 + 2, c[5], c[9]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[9]);
                        break;
                    }
                    case 219:
                    {
                        if (diff(w[4], w[2]))
                        {
                            newPixels[newIndex] = c[5];
                            newPixels[newIndex + 1] = c[5];
                            newPixels[newIndexWidth] = c[5];
                        }
                        else
                        {
                            interp5(newPixels, newIndex, c[2], c[4]);
                            interp5(newPixels, newIndex + 1, c[2], c[5]);
                            interp5(newPixels, newIndexWidth, c[4], c[5]);
                        }
                        interp1(newPixels, newIndex + 2, c[5], c[3]);
                        interp8(newPixels, newIndex + 3, c[5], c[3]);
                        newPixels[newIndexWidth + 1] = c[5];
                        interp3(newPixels, newIndexWidth + 2, c[5], c[3]);
                        interp1(newPixels, newIndexWidth + 3, c[5], c[3]);
                        interp1(newPixels, newIndexWidth2, c[5], c[7]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[7]);
                        newPixels[newIndexWidth2 + 2] = c[5];
                        if (diff(w[6], w[8]))
                        {
                            newPixels[newIndexWidth2 + 3] = c[5];
                            newPixels[newIndexWidth3 + 2] = c[5];
                            newPixels[newIndexWidth3 + 3] = c[5];
                        }
                        else
                        {
                            interp5(newPixels, newIndexWidth2 + 3, c[6], c[5]);
                            interp5(newPixels, newIndexWidth3 + 2, c[8], c[5]);
                            interp5(newPixels, newIndexWidth3 + 3, c[8], c[6]);
                        }
                        interp8(newPixels, newIndexWidth3, c[5], c[7]);
                        interp1(newPixels, newIndexWidth3 + 1, c[5], c[7]);
                        break;
                    }
                    case 125:
                    {
                        if (diff(w[8], w[4]))
                        {
                            interp8(newPixels, newIndex, c[5], c[2]);
                            interp3(newPixels, newIndexWidth, c[5], c[2]);
                            newPixels[newIndexWidth2] = c[5];
                            newPixels[newIndexWidth2 + 1] = c[5];
                            newPixels[newIndexWidth3] = c[5];
                            newPixels[newIndexWidth3 + 1] = c[5];
                        }
                        else
                        {
                            interp1(newPixels, newIndex, c[5], c[4]);
                            interp1(newPixels, newIndexWidth, c[4], c[5]);
                            interp8(newPixels, newIndexWidth2, c[4], c[8]);
                            interp7(newPixels, newIndexWidth2 + 1, c[5], c[4],
                                c[8]);
                            interp5(newPixels, newIndexWidth3, c[8], c[4]);
                            interp2(newPixels, newIndexWidth3 + 1, c[8], c[5],
                                c[4]);
                        }
                        interp8(newPixels, newIndex + 1, c[5], c[2]);
                        interp8(newPixels, newIndex + 2, c[5], c[2]);
                        interp8(newPixels, newIndex + 3, c[5], c[2]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[2]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[2]);
                        interp3(newPixels, newIndexWidth + 3, c[5], c[2]);
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[9]);
                        interp1(newPixels, newIndexWidth2 + 3, c[5], c[9]);
                        interp1(newPixels, newIndexWidth3 + 2, c[5], c[9]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[9]);
                        break;
                    }
                    case 221:
                    {
                        interp8(newPixels, newIndex, c[5], c[2]);
                        interp8(newPixels, newIndex + 1, c[5], c[2]);
                        interp8(newPixels, newIndex + 2, c[5], c[2]);
                        if (diff(w[6], w[8]))
                        {
                            interp8(newPixels, newIndex + 3, c[5], c[2]);
                            interp3(newPixels, newIndexWidth + 3, c[5], c[2]);
                            newPixels[newIndexWidth2 + 2] = c[5];
                            newPixels[newIndexWidth2 + 3] = c[5];
                            newPixels[newIndexWidth3 + 2] = c[5];
                            newPixels[newIndexWidth3 + 3] = c[5];
                        }
                        else
                        {
                            interp1(newPixels, newIndex + 3, c[5], c[6]);
                            interp1(newPixels, newIndexWidth + 3, c[6], c[5]);
                            interp7(newPixels, newIndexWidth2 + 2, c[5], c[6],
                                c[8]);
                            interp8(newPixels, newIndexWidth2 + 3, c[6], c[8]);
                            interp2(newPixels, newIndexWidth3 + 2, c[8], c[5],
                                c[6]);
                            interp5(newPixels, newIndexWidth3 + 3, c[8], c[6]);
                        }
                        interp3(newPixels, newIndexWidth, c[5], c[2]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[2]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[2]);
                        interp1(newPixels, newIndexWidth2, c[5], c[7]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[7]);
                        interp8(newPixels, newIndexWidth3, c[5], c[7]);
                        interp1(newPixels, newIndexWidth3 + 1, c[5], c[7]);
                        break;
                    }
                    case 207:
                    {
                        if (diff(w[4], w[2]))
                        {
                            newPixels[newIndex] = c[5];
                            newPixels[newIndex + 1] = c[5];
                            interp3(newPixels, newIndex + 2, c[5], c[6]);
                            interp8(newPixels, newIndex + 3, c[5], c[6]);
                            newPixels[newIndexWidth] = c[5];
                            newPixels[newIndexWidth + 1] = c[5];
                        }
                        else
                        {
                            interp5(newPixels, newIndex, c[2], c[4]);
                            interp8(newPixels, newIndex + 1, c[2], c[4]);
                            interp1(newPixels, newIndex + 2, c[2], c[5]);
                            interp1(newPixels, newIndex + 3, c[5], c[2]);
                            interp2(newPixels, newIndexWidth, c[4], c[5], c[2]);
                            interp7(newPixels, newIndexWidth + 1, c[5], c[4],
                                c[2]);
                        }
                        interp3(newPixels, newIndexWidth + 2, c[5], c[6]);
                        interp8(newPixels, newIndexWidth + 3, c[5], c[6]);
                        interp1(newPixels, newIndexWidth2, c[5], c[7]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[7]);
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[6]);
                        interp8(newPixels, newIndexWidth2 + 3, c[5], c[6]);
                        interp8(newPixels, newIndexWidth3, c[5], c[7]);
                        interp1(newPixels, newIndexWidth3 + 1, c[5], c[7]);
                        interp3(newPixels, newIndexWidth3 + 2, c[5], c[6]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[6]);
                        break;
                    }
                    case 238:
                    {
                        interp8(newPixels, newIndex, c[5], c[1]);
                        interp1(newPixels, newIndex + 1, c[5], c[1]);
                        interp3(newPixels, newIndex + 2, c[5], c[6]);
                        interp8(newPixels, newIndex + 3, c[5], c[6]);
                        interp1(newPixels, newIndexWidth, c[5], c[1]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[1]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[6]);
                        interp8(newPixels, newIndexWidth + 3, c[5], c[6]);
                        if (diff(w[8], w[4]))
                        {
                            newPixels[newIndexWidth2] = c[5];
                            newPixels[newIndexWidth2 + 1] = c[5];
                            newPixels[newIndexWidth3] = c[5];
                            newPixels[newIndexWidth3 + 1] = c[5];
                            interp3(newPixels, newIndexWidth3 + 2, c[5], c[6]);
                            interp8(newPixels, newIndexWidth3 + 3, c[5], c[6]);
                        }
                        else
                        {
                            interp2(newPixels, newIndexWidth2, c[4], c[5], c[8]);
                            interp7(newPixels, newIndexWidth2 + 1, c[5], c[4],
                                c[8]);
                            interp5(newPixels, newIndexWidth3, c[8], c[4]);
                            interp8(newPixels, newIndexWidth3 + 1, c[8], c[4]);
                            interp1(newPixels, newIndexWidth3 + 2, c[8], c[5]);
                            interp1(newPixels, newIndexWidth3 + 3, c[5], c[8]);
                        }
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[6]);
                        interp8(newPixels, newIndexWidth2 + 3, c[5], c[6]);
                        break;
                    }
                    case 190:
                    {
                        interp8(newPixels, newIndex, c[5], c[1]);
                        interp1(newPixels, newIndex + 1, c[5], c[1]);
                        if (diff(w[2], w[6]))
                        {
                            newPixels[newIndex + 2] = c[5];
                            newPixels[newIndex + 3] = c[5];
                            newPixels[newIndexWidth + 2] = c[5];
                            newPixels[newIndexWidth + 3] = c[5];
                            interp3(newPixels, newIndexWidth2 + 3, c[5], c[8]);
                            interp8(newPixels, newIndexWidth3 + 3, c[5], c[8]);
                        }
                        else
                        {
                            interp2(newPixels, newIndex + 2, c[2], c[5], c[6]);
                            interp5(newPixels, newIndex + 3, c[2], c[6]);
                            interp7(newPixels, newIndexWidth + 2, c[5], c[6],
                                c[2]);
                            interp8(newPixels, newIndexWidth + 3, c[6], c[2]);
                            interp1(newPixels, newIndexWidth2 + 3, c[6], c[5]);
                            interp1(newPixels, newIndexWidth3 + 3, c[5], c[6]);
                        }
                        interp1(newPixels, newIndexWidth, c[5], c[1]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[1]);
                        interp3(newPixels, newIndexWidth2, c[5], c[8]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[8]);
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[8]);
                        interp8(newPixels, newIndexWidth3, c[5], c[8]);
                        interp8(newPixels, newIndexWidth3 + 1, c[5], c[8]);
                        interp8(newPixels, newIndexWidth3 + 2, c[5], c[8]);
                        break;
                    }
                    case 187:
                    {
                        if (diff(w[4], w[2]))
                        {
                            newPixels[newIndex] = c[5];
                            newPixels[newIndex + 1] = c[5];
                            newPixels[newIndexWidth] = c[5];
                            newPixels[newIndexWidth + 1] = c[5];
                            interp3(newPixels, newIndexWidth2, c[5], c[8]);
                            interp8(newPixels, newIndexWidth3, c[5], c[8]);
                        }
                        else
                        {
                            interp5(newPixels, newIndex, c[2], c[4]);
                            interp2(newPixels, newIndex + 1, c[2], c[5], c[4]);
                            interp8(newPixels, newIndexWidth, c[4], c[2]);
                            interp7(newPixels, newIndexWidth + 1, c[5], c[4],
                                c[2]);
                            interp1(newPixels, newIndexWidth2, c[4], c[5]);
                            interp1(newPixels, newIndexWidth3, c[5], c[4]);
                        }
                        interp1(newPixels, newIndex + 2, c[5], c[3]);
                        interp8(newPixels, newIndex + 3, c[5], c[3]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[3]);
                        interp1(newPixels, newIndexWidth + 3, c[5], c[3]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[8]);
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[8]);
                        interp3(newPixels, newIndexWidth2 + 3, c[5], c[8]);
                        interp8(newPixels, newIndexWidth3 + 1, c[5], c[8]);
                        interp8(newPixels, newIndexWidth3 + 2, c[5], c[8]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[8]);
                        break;
                    }
                    case 243:
                    {
                        interp8(newPixels, newIndex, c[5], c[4]);
                        interp3(newPixels, newIndex + 1, c[5], c[4]);
                        interp1(newPixels, newIndex + 2, c[5], c[3]);
                        interp8(newPixels, newIndex + 3, c[5], c[3]);
                        interp8(newPixels, newIndexWidth, c[5], c[4]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[4]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[3]);
                        interp1(newPixels, newIndexWidth + 3, c[5], c[3]);
                        interp8(newPixels, newIndexWidth2, c[5], c[4]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[4]);
                        if (diff(w[6], w[8]))
                        {
                            newPixels[newIndexWidth2 + 2] = c[5];
                            newPixels[newIndexWidth2 + 3] = c[5];
                            interp8(newPixels, newIndexWidth3, c[5], c[4]);
                            interp3(newPixels, newIndexWidth3 + 1, c[5], c[4]);
                            newPixels[newIndexWidth3 + 2] = c[5];
                            newPixels[newIndexWidth3 + 3] = c[5];
                        }
                        else
                        {
                            interp7(newPixels, newIndexWidth2 + 2, c[5], c[6],
                                c[8]);
                            interp2(newPixels, newIndexWidth2 + 3, c[6], c[5],
                                c[8]);
                            interp1(newPixels, newIndexWidth3, c[5], c[8]);
                            interp1(newPixels, newIndexWidth3 + 1, c[8], c[5]);
                            interp8(newPixels, newIndexWidth3 + 2, c[8], c[6]);
                            interp5(newPixels, newIndexWidth3 + 3, c[8], c[6]);
                        }
                        break;
                    }
                    case 119:
                    {
                        if (diff(w[2], w[6]))
                        {
                            interp8(newPixels, newIndex, c[5], c[4]);
                            interp3(newPixels, newIndex + 1, c[5], c[4]);
                            newPixels[newIndex + 2] = c[5];
                            newPixels[newIndex + 3] = c[5];
                            newPixels[newIndexWidth + 2] = c[5];
                            newPixels[newIndexWidth + 3] = c[5];
                        }
                        else
                        {
                            interp1(newPixels, newIndex, c[5], c[2]);
                            interp1(newPixels, newIndex + 1, c[2], c[5]);
                            interp8(newPixels, newIndex + 2, c[2], c[6]);
                            interp5(newPixels, newIndex + 3, c[2], c[6]);
                            interp7(newPixels, newIndexWidth + 2, c[5], c[6],
                                c[2]);
                            interp2(newPixels, newIndexWidth + 3, c[6], c[5],
                                c[2]);
                        }
                        interp8(newPixels, newIndexWidth, c[5], c[4]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[4]);
                        interp8(newPixels, newIndexWidth2, c[5], c[4]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[4]);
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[9]);
                        interp1(newPixels, newIndexWidth2 + 3, c[5], c[9]);
                        interp8(newPixels, newIndexWidth3, c[5], c[4]);
                        interp3(newPixels, newIndexWidth3 + 1, c[5], c[4]);
                        interp1(newPixels, newIndexWidth3 + 2, c[5], c[9]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[9]);
                        break;
                    }
                    case 237:
                    case 233:
                    {
                        interp8(newPixels, newIndex, c[5], c[2]);
                        interp8(newPixels, newIndex + 1, c[5], c[2]);
                        interp6(newPixels, newIndex + 2, c[5], c[2], c[6]);
                        interp2(newPixels, newIndex + 3, c[5], c[2], c[6]);
                        interp3(newPixels, newIndexWidth, c[5], c[2]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[2]);
                        interp7(newPixels, newIndexWidth + 2, c[5], c[6], c[2]);
                        interp6(newPixels, newIndexWidth + 3, c[5], c[6], c[2]);
                        newPixels[newIndexWidth2] = c[5];
                        newPixels[newIndexWidth2 + 1] = c[5];
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[6]);
                        interp8(newPixels, newIndexWidth2 + 3, c[5], c[6]);
                        if (diff(w[8], w[4]))
                        {
                            newPixels[newIndexWidth3] = c[5];
                        }
                        else
                        {
                            interp2(newPixels, newIndexWidth3, c[5], c[8], c[4]);
                        }
                        newPixels[newIndexWidth3 + 1] = c[5];
                        interp3(newPixels, newIndexWidth3 + 2, c[5], c[6]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[6]);
                        break;
                    }
                    case 175:
                    case 47:
                    {
                        if (diff(w[4], w[2]))
                        {
                            newPixels[newIndex] = c[5];
                        }
                        else
                        {
                            interp2(newPixels, newIndex, c[5], c[2], c[4]);
                        }
                        newPixels[newIndex + 1] = c[5];
                        interp3(newPixels, newIndex + 2, c[5], c[6]);
                        interp8(newPixels, newIndex + 3, c[5], c[6]);
                        newPixels[newIndexWidth] = c[5];
                        newPixels[newIndexWidth + 1] = c[5];
                        interp3(newPixels, newIndexWidth + 2, c[5], c[6]);
                        interp8(newPixels, newIndexWidth + 3, c[5], c[6]);
                        interp3(newPixels, newIndexWidth2, c[5], c[8]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[8]);
                        interp7(newPixels, newIndexWidth2 + 2, c[5], c[6], c[8]);
                        interp6(newPixels, newIndexWidth2 + 3, c[5], c[6],
                            c[8]);
                        interp8(newPixels, newIndexWidth3, c[5], c[8]);
                        interp8(newPixels, newIndexWidth3 + 1, c[5], c[8]);
                        interp6(newPixels, newIndexWidth3 + 2, c[5], c[8], c[6]);
                        interp2(newPixels, newIndexWidth3 + 3, c[5], c[8],
                            c[6]);
                        break;
                    }
                    case 183:
                    case 151:
                    {
                        interp8(newPixels, newIndex, c[5], c[4]);
                        interp3(newPixels, newIndex + 1, c[5], c[4]);
                        newPixels[newIndex + 2] = c[5];
                        if (diff(w[2], w[6]))
                        {
                            newPixels[newIndex + 3] = c[5];
                        }
                        else
                        {
                            interp2(newPixels, newIndex + 3, c[5], c[2], c[6]);
                        }
                        interp8(newPixels, newIndexWidth, c[5], c[4]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[4]);
                        newPixels[newIndexWidth + 2] = c[5];
                        newPixels[newIndexWidth + 3] = c[5];
                        interp6(newPixels, newIndexWidth2, c[5], c[4], c[8]);
                        interp7(newPixels, newIndexWidth2 + 1, c[5], c[4], c[8]);
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[8]);
                        interp3(newPixels, newIndexWidth2 + 3, c[5], c[8]);
                        interp2(newPixels, newIndexWidth3, c[5], c[8], c[4]);
                        interp6(newPixels, newIndexWidth3 + 1, c[5], c[8], c[4]);
                        interp8(newPixels, newIndexWidth3 + 2, c[5], c[8]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[8]);
                        break;
                    }
                    case 245:
                    case 244:
                    {
                        interp2(newPixels, newIndex, c[5], c[2], c[4]);
                        interp6(newPixels, newIndex + 1, c[5], c[2], c[4]);
                        interp8(newPixels, newIndex + 2, c[5], c[2]);
                        interp8(newPixels, newIndex + 3, c[5], c[2]);
                        interp6(newPixels, newIndexWidth, c[5], c[4], c[2]);
                        interp7(newPixels, newIndexWidth + 1, c[5], c[4], c[2]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[2]);
                        interp3(newPixels, newIndexWidth + 3, c[5], c[2]);
                        interp8(newPixels, newIndexWidth2, c[5], c[4]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[4]);
                        newPixels[newIndexWidth2 + 2] = c[5];
                        newPixels[newIndexWidth2 + 3] = c[5];
                        interp8(newPixels, newIndexWidth3, c[5], c[4]);
                        interp3(newPixels, newIndexWidth3 + 1, c[5], c[4]);
                        newPixels[newIndexWidth3 + 2] = c[5];
                        if (diff(w[6], w[8]))
                        {
                            newPixels[newIndexWidth3 + 3] = c[5];
                        }
                        else
                        {
                            interp2(newPixels, newIndexWidth3 + 3, c[5], c[8],
                                c[6]);
                        }
                        break;
                    }
                    case 250:
                    {
                        interp8(newPixels, newIndex, c[5], c[1]);
                        interp1(newPixels, newIndex + 1, c[5], c[1]);
                        interp1(newPixels, newIndex + 2, c[5], c[3]);
                        interp8(newPixels, newIndex + 3, c[5], c[3]);
                        interp1(newPixels, newIndexWidth, c[5], c[1]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[1]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[3]);
                        interp1(newPixels, newIndexWidth + 3, c[5], c[3]);
                        if (diff(w[8], w[4]))
                        {
                            newPixels[newIndexWidth2] = c[5];
                            newPixels[newIndexWidth3] = c[5];
                            newPixels[newIndexWidth3 + 1] = c[5];
                        }
                        else
                        {
                            interp5(newPixels, newIndexWidth2, c[4], c[5]);
                            interp5(newPixels, newIndexWidth3, c[8], c[4]);
                            interp5(newPixels, newIndexWidth3 + 1, c[8], c[5]);
                        }
                        newPixels[newIndexWidth2 + 1] = c[5];
                        newPixels[newIndexWidth2 + 2] = c[5];
                        if (diff(w[6], w[8]))
                        {
                            newPixels[newIndexWidth2 + 3] = c[5];
                            newPixels[newIndexWidth3 + 2] = c[5];
                            newPixels[newIndexWidth3 + 3] = c[5];
                        }
                        else
                        {
                            interp5(newPixels, newIndexWidth2 + 3, c[6], c[5]);
                            interp5(newPixels, newIndexWidth3 + 2, c[8], c[5]);
                            interp5(newPixels, newIndexWidth3 + 3, c[8], c[6]);
                        }
                        break;
                    }
                    case 123:
                    {
                        if (diff(w[4], w[2]))
                        {
                            newPixels[newIndex] = c[5];
                            newPixels[newIndex + 1] = c[5];
                            newPixels[newIndexWidth] = c[5];
                        }
                        else
                        {
                            interp5(newPixels, newIndex, c[2], c[4]);
                            interp5(newPixels, newIndex + 1, c[2], c[5]);
                            interp5(newPixels, newIndexWidth, c[4], c[5]);
                        }
                        interp1(newPixels, newIndex + 2, c[5], c[3]);
                        interp8(newPixels, newIndex + 3, c[5], c[3]);
                        newPixels[newIndexWidth + 1] = c[5];
                        interp3(newPixels, newIndexWidth + 2, c[5], c[3]);
                        interp1(newPixels, newIndexWidth + 3, c[5], c[3]);
                        if (diff(w[8], w[4]))
                        {
                            newPixels[newIndexWidth2] = c[5];
                            newPixels[newIndexWidth3] = c[5];
                            newPixels[newIndexWidth3 + 1] = c[5];
                        }
                        else
                        {
                            interp5(newPixels, newIndexWidth2, c[4], c[5]);
                            interp5(newPixels, newIndexWidth3, c[8], c[4]);
                            interp5(newPixels, newIndexWidth3 + 1, c[8], c[5]);
                        }
                        newPixels[newIndexWidth2 + 1] = c[5];
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[9]);
                        interp1(newPixels, newIndexWidth2 + 3, c[5], c[9]);
                        interp1(newPixels, newIndexWidth3 + 2, c[5], c[9]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[9]);
                        break;
                    }
                    case 95:
                    {
                        if (diff(w[4], w[2]))
                        {
                            newPixels[newIndex] = c[5];
                            newPixels[newIndex + 1] = c[5];
                            newPixels[newIndexWidth] = c[5];
                        }
                        else
                        {
                            interp5(newPixels, newIndex, c[2], c[4]);
                            interp5(newPixels, newIndex + 1, c[2], c[5]);
                            interp5(newPixels, newIndexWidth, c[4], c[5]);
                        }
                        if (diff(w[2], w[6]))
                        {
                            newPixels[newIndex + 2] = c[5];
                            newPixels[newIndex + 3] = c[5];
                            newPixels[newIndexWidth + 3] = c[5];
                        }
                        else
                        {
                            interp5(newPixels, newIndex + 2, c[2], c[5]);
                            interp5(newPixels, newIndex + 3, c[2], c[6]);
                            interp5(newPixels, newIndexWidth + 3, c[6], c[5]);
                        }
                        newPixels[newIndexWidth + 1] = c[5];
                        newPixels[newIndexWidth + 2] = c[5];
                        interp1(newPixels, newIndexWidth2, c[5], c[7]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[7]);
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[9]);
                        interp1(newPixels, newIndexWidth2 + 3, c[5], c[9]);
                        interp8(newPixels, newIndexWidth3, c[5], c[7]);
                        interp1(newPixels, newIndexWidth3 + 1, c[5], c[7]);
                        interp1(newPixels, newIndexWidth3 + 2, c[5], c[9]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[9]);
                        break;
                    }
                    case 222:
                    {
                        interp8(newPixels, newIndex, c[5], c[1]);
                        interp1(newPixels, newIndex + 1, c[5], c[1]);
                        if (diff(w[2], w[6]))
                        {
                            newPixels[newIndex + 2] = c[5];
                            newPixels[newIndex + 3] = c[5];
                            newPixels[newIndexWidth + 3] = c[5];
                        }
                        else
                        {
                            interp5(newPixels, newIndex + 2, c[2], c[5]);
                            interp5(newPixels, newIndex + 3, c[2], c[6]);
                            interp5(newPixels, newIndexWidth + 3, c[6], c[5]);
                        }
                        interp1(newPixels, newIndexWidth, c[5], c[1]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[1]);
                        newPixels[newIndexWidth + 2] = c[5];
                        interp1(newPixels, newIndexWidth2, c[5], c[7]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[7]);
                        newPixels[newIndexWidth2 + 2] = c[5];
                        if (diff(w[6], w[8]))
                        {
                            newPixels[newIndexWidth2 + 3] = c[5];
                            newPixels[newIndexWidth3 + 2] = c[5];
                            newPixels[newIndexWidth3 + 3] = c[5];
                        }
                        else
                        {
                            interp5(newPixels, newIndexWidth2 + 3, c[6], c[5]);
                            interp5(newPixels, newIndexWidth3 + 2, c[8], c[5]);
                            interp5(newPixels, newIndexWidth3 + 3, c[8], c[6]);
                        }
                        interp8(newPixels, newIndexWidth3, c[5], c[7]);
                        interp1(newPixels, newIndexWidth3 + 1, c[5], c[7]);
                        break;
                    }
                    case 252:
                    {
                        interp8(newPixels, newIndex, c[5], c[1]);
                        interp6(newPixels, newIndex + 1, c[5], c[2], c[1]);
                        interp8(newPixels, newIndex + 2, c[5], c[2]);
                        interp8(newPixels, newIndex + 3, c[5], c[2]);
                        interp1(newPixels, newIndexWidth, c[5], c[1]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[1]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[2]);
                        interp3(newPixels, newIndexWidth + 3, c[5], c[2]);
                        if (diff(w[8], w[4]))
                        {
                            newPixels[newIndexWidth2] = c[5];
                            newPixels[newIndexWidth3] = c[5];
                            newPixels[newIndexWidth3 + 1] = c[5];
                        }
                        else
                        {
                            interp5(newPixels, newIndexWidth2, c[4], c[5]);
                            interp5(newPixels, newIndexWidth3, c[8], c[4]);
                            interp5(newPixels, newIndexWidth3 + 1, c[8], c[5]);
                        }
                        newPixels[newIndexWidth2 + 1] = c[5];
                        newPixels[newIndexWidth2 + 2] = c[5];
                        newPixels[newIndexWidth2 + 3] = c[5];
                        newPixels[newIndexWidth3 + 2] = c[5];
                        if (diff(w[6], w[8]))
                        {
                            newPixels[newIndexWidth3 + 3] = c[5];
                        }
                        else
                        {
                            interp2(newPixels, newIndexWidth3 + 3, c[5], c[8],
                                c[6]);
                        }
                        break;
                    }
                    case 249:
                    {
                        interp8(newPixels, newIndex, c[5], c[2]);
                        interp8(newPixels, newIndex + 1, c[5], c[2]);
                        interp6(newPixels, newIndex + 2, c[5], c[2], c[3]);
                        interp8(newPixels, newIndex + 3, c[5], c[3]);
                        interp3(newPixels, newIndexWidth, c[5], c[2]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[2]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[3]);
                        interp1(newPixels, newIndexWidth + 3, c[5], c[3]);
                        newPixels[newIndexWidth2] = c[5];
                        newPixels[newIndexWidth2 + 1] = c[5];
                        newPixels[newIndexWidth2 + 2] = c[5];
                        if (diff(w[6], w[8]))
                        {
                            newPixels[newIndexWidth2 + 3] = c[5];
                            newPixels[newIndexWidth3 + 2] = c[5];
                            newPixels[newIndexWidth3 + 3] = c[5];
                        }
                        else
                        {
                            interp5(newPixels, newIndexWidth2 + 3, c[6], c[5]);
                            interp5(newPixels, newIndexWidth3 + 2, c[8], c[5]);
                            interp5(newPixels, newIndexWidth3 + 3, c[8], c[6]);
                        }
                        if (diff(w[8], w[4]))
                        {
                            newPixels[newIndexWidth3] = c[5];
                        }
                        else
                        {
                            interp2(newPixels, newIndexWidth3, c[5], c[8], c[4]);
                        }
                        newPixels[newIndexWidth3 + 1] = c[5];
                        break;
                    }
                    case 235:
                    {
                        if (diff(w[4], w[2]))
                        {
                            newPixels[newIndex] = c[5];
                            newPixels[newIndex + 1] = c[5];
                            newPixels[newIndexWidth] = c[5];
                        }
                        else
                        {
                            interp5(newPixels, newIndex, c[2], c[4]);
                            interp5(newPixels, newIndex + 1, c[2], c[5]);
                            interp5(newPixels, newIndexWidth, c[4], c[5]);
                        }
                        interp1(newPixels, newIndex + 2, c[5], c[3]);
                        interp8(newPixels, newIndex + 3, c[5], c[3]);
                        newPixels[newIndexWidth + 1] = c[5];
                        interp3(newPixels, newIndexWidth + 2, c[5], c[3]);
                        interp6(newPixels, newIndexWidth + 3, c[5], c[6], c[3]);
                        newPixels[newIndexWidth2] = c[5];
                        newPixels[newIndexWidth2 + 1] = c[5];
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[6]);
                        interp8(newPixels, newIndexWidth2 + 3, c[5], c[6]);
                        if (diff(w[8], w[4]))
                        {
                            newPixels[newIndexWidth3] = c[5];
                        }
                        else
                        {
                            interp2(newPixels, newIndexWidth3, c[5], c[8], c[4]);
                        }
                        newPixels[newIndexWidth3 + 1] = c[5];
                        interp3(newPixels, newIndexWidth3 + 2, c[5], c[6]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[6]);
                        break;
                    }
                    case 111:
                    {
                        if (diff(w[4], w[2]))
                        {
                            newPixels[newIndex] = c[5];
                        }
                        else
                        {
                            interp2(newPixels, newIndex, c[5], c[2], c[4]);
                        }
                        newPixels[newIndex + 1] = c[5];
                        interp3(newPixels, newIndex + 2, c[5], c[6]);
                        interp8(newPixels, newIndex + 3, c[5], c[6]);
                        newPixels[newIndexWidth] = c[5];
                        newPixels[newIndexWidth + 1] = c[5];
                        interp3(newPixels, newIndexWidth + 2, c[5], c[6]);
                        interp8(newPixels, newIndexWidth + 3, c[5], c[6]);
                        if (diff(w[8], w[4]))
                        {
                            newPixels[newIndexWidth2] = c[5];
                            newPixels[newIndexWidth3] = c[5];
                            newPixels[newIndexWidth3 + 1] = c[5];
                        }
                        else
                        {
                            interp5(newPixels, newIndexWidth2, c[4], c[5]);
                            interp5(newPixels, newIndexWidth3, c[8], c[4]);
                            interp5(newPixels, newIndexWidth3 + 1, c[8], c[5]);
                        }
                        newPixels[newIndexWidth2 + 1] = c[5];
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[9]);
                        interp6(newPixels, newIndexWidth2 + 3, c[5], c[6],
                            c[9]);
                        interp1(newPixels, newIndexWidth3 + 2, c[5], c[9]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[9]);
                        break;
                    }
                    case 63:
                    {
                        if (diff(w[4], w[2]))
                        {
                            newPixels[newIndex] = c[5];
                        }
                        else
                        {
                            interp2(newPixels, newIndex, c[5], c[2], c[4]);
                        }
                        newPixels[newIndex + 1] = c[5];
                        if (diff(w[2], w[6]))
                        {
                            newPixels[newIndex + 2] = c[5];
                            newPixels[newIndex + 3] = c[5];
                            newPixels[newIndexWidth + 3] = c[5];
                        }
                        else
                        {
                            interp5(newPixels, newIndex + 2, c[2], c[5]);
                            interp5(newPixels, newIndex + 3, c[2], c[6]);
                            interp5(newPixels, newIndexWidth + 3, c[6], c[5]);
                        }
                        newPixels[newIndexWidth] = c[5];
                        newPixels[newIndexWidth + 1] = c[5];
                        newPixels[newIndexWidth + 2] = c[5];
                        interp3(newPixels, newIndexWidth2, c[5], c[8]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[8]);
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[9]);
                        interp1(newPixels, newIndexWidth2 + 3, c[5], c[9]);
                        interp8(newPixels, newIndexWidth3, c[5], c[8]);
                        interp8(newPixels, newIndexWidth3 + 1, c[5], c[8]);
                        interp6(newPixels, newIndexWidth3 + 2, c[5], c[8], c[9]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[9]);
                        break;
                    }
                    case 159:
                    {
                        if (diff(w[4], w[2]))
                        {
                            newPixels[newIndex] = c[5];
                            newPixels[newIndex + 1] = c[5];
                            newPixels[newIndexWidth] = c[5];
                        }
                        else
                        {
                            interp5(newPixels, newIndex, c[2], c[4]);
                            interp5(newPixels, newIndex + 1, c[2], c[5]);
                            interp5(newPixels, newIndexWidth, c[4], c[5]);
                        }
                        newPixels[newIndex + 2] = c[5];
                        if (diff(w[2], w[6]))
                        {
                            newPixels[newIndex + 3] = c[5];
                        }
                        else
                        {
                            interp2(newPixels, newIndex + 3, c[5], c[2], c[6]);
                        }
                        newPixels[newIndexWidth + 1] = c[5];
                        newPixels[newIndexWidth + 2] = c[5];
                        newPixels[newIndexWidth + 3] = c[5];
                        interp1(newPixels, newIndexWidth2, c[5], c[7]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[7]);
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[8]);
                        interp3(newPixels, newIndexWidth2 + 3, c[5], c[8]);
                        interp8(newPixels, newIndexWidth3, c[5], c[7]);
                        interp6(newPixels, newIndexWidth3 + 1, c[5], c[8], c[7]);
                        interp8(newPixels, newIndexWidth3 + 2, c[5], c[8]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[8]);
                        break;
                    }
                    case 215:
                    {
                        interp8(newPixels, newIndex, c[5], c[4]);
                        interp3(newPixels, newIndex + 1, c[5], c[4]);
                        newPixels[newIndex + 2] = c[5];
                        if (diff(w[2], w[6]))
                        {
                            newPixels[newIndex + 3] = c[5];
                        }
                        else
                        {
                            interp2(newPixels, newIndex + 3, c[5], c[2], c[6]);
                        }
                        interp8(newPixels, newIndexWidth, c[5], c[4]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[4]);
                        newPixels[newIndexWidth + 2] = c[5];
                        newPixels[newIndexWidth + 3] = c[5];
                        interp6(newPixels, newIndexWidth2, c[5], c[4], c[7]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[7]);
                        newPixels[newIndexWidth2 + 2] = c[5];
                        if (diff(w[6], w[8]))
                        {
                            newPixels[newIndexWidth2 + 3] = c[5];
                            newPixels[newIndexWidth3 + 2] = c[5];
                            newPixels[newIndexWidth3 + 3] = c[5];
                        }
                        else
                        {
                            interp5(newPixels, newIndexWidth2 + 3, c[6], c[5]);
                            interp5(newPixels, newIndexWidth3 + 2, c[8], c[5]);
                            interp5(newPixels, newIndexWidth3 + 3, c[8], c[6]);
                        }
                        interp8(newPixels, newIndexWidth3, c[5], c[7]);
                        interp1(newPixels, newIndexWidth3 + 1, c[5], c[7]);
                        break;
                    }
                    case 246:
                    {
                        interp8(newPixels, newIndex, c[5], c[1]);
                        interp1(newPixels, newIndex + 1, c[5], c[1]);
                        if (diff(w[2], w[6]))
                        {
                            newPixels[newIndex + 2] = c[5];
                            newPixels[newIndex + 3] = c[5];
                            newPixels[newIndexWidth + 3] = c[5];
                        }
                        else
                        {
                            interp5(newPixels, newIndex + 2, c[2], c[5]);
                            interp5(newPixels, newIndex + 3, c[2], c[6]);
                            interp5(newPixels, newIndexWidth + 3, c[6], c[5]);
                        }
                        interp6(newPixels, newIndexWidth, c[5], c[4], c[1]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[1]);
                        newPixels[newIndexWidth + 2] = c[5];
                        interp8(newPixels, newIndexWidth2, c[5], c[4]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[4]);
                        newPixels[newIndexWidth2 + 2] = c[5];
                        newPixels[newIndexWidth2 + 3] = c[5];
                        interp8(newPixels, newIndexWidth3, c[5], c[4]);
                        interp3(newPixels, newIndexWidth3 + 1, c[5], c[4]);
                        newPixels[newIndexWidth3 + 2] = c[5];
                        if (diff(w[6], w[8]))
                        {
                            newPixels[newIndexWidth3 + 3] = c[5];
                        }
                        else
                        {
                            interp2(newPixels, newIndexWidth3 + 3, c[5], c[8],
                                c[6]);
                        }
                        break;
                    }
                    case 254:
                    {
                        interp8(newPixels, newIndex, c[5], c[1]);
                        interp1(newPixels, newIndex + 1, c[5], c[1]);
                        if (diff(w[2], w[6]))
                        {
                            newPixels[newIndex + 2] = c[5];
                            newPixels[newIndex + 3] = c[5];
                            newPixels[newIndexWidth + 3] = c[5];
                        }
                        else
                        {
                            interp5(newPixels, newIndex + 2, c[2], c[5]);
                            interp5(newPixels, newIndex + 3, c[2], c[6]);
                            interp5(newPixels, newIndexWidth + 3, c[6], c[5]);
                        }
                        interp1(newPixels, newIndexWidth, c[5], c[1]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[1]);
                        newPixels[newIndexWidth + 2] = c[5];
                        if (diff(w[8], w[4]))
                        {
                            newPixels[newIndexWidth2] = c[5];
                            newPixels[newIndexWidth3] = c[5];
                            newPixels[newIndexWidth3 + 1] = c[5];
                        }
                        else
                        {
                            interp5(newPixels, newIndexWidth2, c[4], c[5]);
                            interp5(newPixels, newIndexWidth3, c[8], c[4]);
                            interp5(newPixels, newIndexWidth3 + 1, c[8], c[5]);
                        }
                        newPixels[newIndexWidth2 + 1] = c[5];
                        newPixels[newIndexWidth2 + 2] = c[5];
                        newPixels[newIndexWidth2 + 3] = c[5];
                        newPixels[newIndexWidth3 + 2] = c[5];
                        if (diff(w[6], w[8]))
                        {
                            newPixels[newIndexWidth3 + 3] = c[5];
                        }
                        else
                        {
                            interp2(newPixels, newIndexWidth3 + 3, c[5], c[8],
                                c[6]);
                        }
                        break;
                    }
                    case 253:
                    {
                        interp8(newPixels, newIndex, c[5], c[2]);
                        interp8(newPixels, newIndex + 1, c[5], c[2]);
                        interp8(newPixels, newIndex + 2, c[5], c[2]);
                        interp8(newPixels, newIndex + 3, c[5], c[2]);
                        interp3(newPixels, newIndexWidth, c[5], c[2]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[2]);
                        interp3(newPixels, newIndexWidth + 2, c[5], c[2]);
                        interp3(newPixels, newIndexWidth + 3, c[5], c[2]);
                        newPixels[newIndexWidth2] = c[5];
                        newPixels[newIndexWidth2 + 1] = c[5];
                        newPixels[newIndexWidth2 + 2] = c[5];
                        newPixels[newIndexWidth2 + 3] = c[5];
                        if (diff(w[8], w[4]))
                        {
                            newPixels[newIndexWidth3] = c[5];
                        }
                        else
                        {
                            interp2(newPixels, newIndexWidth3, c[5], c[8], c[4]);
                        }
                        newPixels[newIndexWidth3 + 1] = c[5];
                        newPixels[newIndexWidth3 + 2] = c[5];
                        if (diff(w[6], w[8]))
                        {
                            newPixels[newIndexWidth3 + 3] = c[5];
                        }
                        else
                        {
                            interp2(newPixels, newIndexWidth3 + 3, c[5], c[8],
                                c[6]);
                        }
                        break;
                    }
                    case 251:
                    {
                        if (diff(w[4], w[2]))
                        {
                            newPixels[newIndex] = c[5];
                            newPixels[newIndex + 1] = c[5];
                            newPixels[newIndexWidth] = c[5];
                        }
                        else
                        {
                            interp5(newPixels, newIndex, c[2], c[4]);
                            interp5(newPixels, newIndex + 1, c[2], c[5]);
                            interp5(newPixels, newIndexWidth, c[4], c[5]);
                        }
                        interp1(newPixels, newIndex + 2, c[5], c[3]);
                        interp8(newPixels, newIndex + 3, c[5], c[3]);
                        newPixels[newIndexWidth + 1] = c[5];
                        interp3(newPixels, newIndexWidth + 2, c[5], c[3]);
                        interp1(newPixels, newIndexWidth + 3, c[5], c[3]);
                        newPixels[newIndexWidth2] = c[5];
                        newPixels[newIndexWidth2 + 1] = c[5];
                        newPixels[newIndexWidth2 + 2] = c[5];
                        if (diff(w[6], w[8]))
                        {
                            newPixels[newIndexWidth2 + 3] = c[5];
                            newPixels[newIndexWidth3 + 2] = c[5];
                            newPixels[newIndexWidth3 + 3] = c[5];
                        }
                        else
                        {
                            interp5(newPixels, newIndexWidth2 + 3, c[6], c[5]);
                            interp5(newPixels, newIndexWidth3 + 2, c[8], c[5]);
                            interp5(newPixels, newIndexWidth3 + 3, c[8], c[6]);
                        }
                        if (diff(w[8], w[4]))
                        {
                            newPixels[newIndexWidth3] = c[5];
                        }
                        else
                        {
                            interp2(newPixels, newIndexWidth3, c[5], c[8], c[4]);
                        }
                        newPixels[newIndexWidth3 + 1] = c[5];
                        break;
                    }
                    case 239:
                    {
                        if (diff(w[4], w[2]))
                        {
                            newPixels[newIndex] = c[5];
                        }
                        else
                        {
                            interp2(newPixels, newIndex, c[5], c[2], c[4]);
                        }
                        newPixels[newIndex + 1] = c[5];
                        interp3(newPixels, newIndex + 2, c[5], c[6]);
                        interp8(newPixels, newIndex + 3, c[5], c[6]);
                        newPixels[newIndexWidth] = c[5];
                        newPixels[newIndexWidth + 1] = c[5];
                        interp3(newPixels, newIndexWidth + 2, c[5], c[6]);
                        interp8(newPixels, newIndexWidth + 3, c[5], c[6]);
                        newPixels[newIndexWidth2] = c[5];
                        newPixels[newIndexWidth2 + 1] = c[5];
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[6]);
                        interp8(newPixels, newIndexWidth2 + 3, c[5], c[6]);
                        if (diff(w[8], w[4]))
                        {
                            newPixels[newIndexWidth3] = c[5];
                        }
                        else
                        {
                            interp2(newPixels, newIndexWidth3, c[5], c[8], c[4]);
                        }
                        newPixels[newIndexWidth3 + 1] = c[5];
                        interp3(newPixels, newIndexWidth3 + 2, c[5], c[6]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[6]);
                        break;
                    }
                    case 127:
                    {
                        if (diff(w[4], w[2]))
                        {
                            newPixels[newIndex] = c[5];
                        }
                        else
                        {
                            interp2(newPixels, newIndex, c[5], c[2], c[4]);
                        }
                        newPixels[newIndex + 1] = c[5];
                        if (diff(w[2], w[6]))
                        {
                            newPixels[newIndex + 2] = c[5];
                            newPixels[newIndex + 3] = c[5];
                            newPixels[newIndexWidth + 3] = c[5];
                        }
                        else
                        {
                            interp5(newPixels, newIndex + 2, c[2], c[5]);
                            interp5(newPixels, newIndex + 3, c[2], c[6]);
                            interp5(newPixels, newIndexWidth + 3, c[6], c[5]);
                        }
                        newPixels[newIndexWidth] = c[5];
                        newPixels[newIndexWidth + 1] = c[5];
                        newPixels[newIndexWidth + 2] = c[5];
                        if (diff(w[8], w[4]))
                        {
                            newPixels[newIndexWidth2] = c[5];
                            newPixels[newIndexWidth3] = c[5];
                            newPixels[newIndexWidth3 + 1] = c[5];
                        }
                        else
                        {
                            interp5(newPixels, newIndexWidth2, c[4], c[5]);
                            interp5(newPixels, newIndexWidth3, c[8], c[4]);
                            interp5(newPixels, newIndexWidth3 + 1, c[8], c[5]);
                        }
                        newPixels[newIndexWidth2 + 1] = c[5];
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[9]);
                        interp1(newPixels, newIndexWidth2 + 3, c[5], c[9]);
                        interp1(newPixels, newIndexWidth3 + 2, c[5], c[9]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[9]);
                        break;
                    }
                    case 191:
                    {
                        if (diff(w[4], w[2]))
                        {
                            newPixels[newIndex] = c[5];
                        }
                        else
                        {
                            interp2(newPixels, newIndex, c[5], c[2], c[4]);
                        }
                        newPixels[newIndex + 1] = c[5];
                        newPixels[newIndex + 2] = c[5];
                        if (diff(w[2], w[6]))
                        {
                            newPixels[newIndex + 3] = c[5];
                        }
                        else
                        {
                            interp2(newPixels, newIndex + 3, c[5], c[2], c[6]);
                        }
                        newPixels[newIndexWidth] = c[5];
                        newPixels[newIndexWidth + 1] = c[5];
                        newPixels[newIndexWidth + 2] = c[5];
                        newPixels[newIndexWidth + 3] = c[5];
                        interp3(newPixels, newIndexWidth2, c[5], c[8]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[8]);
                        interp3(newPixels, newIndexWidth2 + 2, c[5], c[8]);
                        interp3(newPixels, newIndexWidth2 + 3, c[5], c[8]);
                        interp8(newPixels, newIndexWidth3, c[5], c[8]);
                        interp8(newPixels, newIndexWidth3 + 1, c[5], c[8]);
                        interp8(newPixels, newIndexWidth3 + 2, c[5], c[8]);
                        interp8(newPixels, newIndexWidth3 + 3, c[5], c[8]);
                        break;
                    }
                    case 223:
                    {
                        if (diff(w[4], w[2]))
                        {
                            newPixels[newIndex] = c[5];
                            newPixels[newIndex + 1] = c[5];
                            newPixels[newIndexWidth] = c[5];
                        }
                        else
                        {
                            interp5(newPixels, newIndex, c[2], c[4]);
                            interp5(newPixels, newIndex + 1, c[2], c[5]);
                            interp5(newPixels, newIndexWidth, c[4], c[5]);
                        }
                        newPixels[newIndex + 2] = c[5];
                        if (diff(w[2], w[6]))
                        {
                            newPixels[newIndex + 3] = c[5];
                        }
                        else
                        {
                            interp2(newPixels, newIndex + 3, c[5], c[2], c[6]);
                        }
                        newPixels[newIndexWidth + 1] = c[5];
                        newPixels[newIndexWidth + 2] = c[5];
                        newPixels[newIndexWidth + 3] = c[5];
                        interp1(newPixels, newIndexWidth2, c[5], c[7]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[7]);
                        newPixels[newIndexWidth2 + 2] = c[5];
                        if (diff(w[6], w[8]))
                        {
                            newPixels[newIndexWidth2 + 3] = c[5];
                            newPixels[newIndexWidth3 + 2] = c[5];
                            newPixels[newIndexWidth3 + 3] = c[5];
                        }
                        else
                        {
                            interp5(newPixels, newIndexWidth2 + 3, c[6], c[5]);
                            interp5(newPixels, newIndexWidth3 + 2, c[8], c[5]);
                            interp5(newPixels, newIndexWidth3 + 3, c[8], c[6]);
                        }
                        interp8(newPixels, newIndexWidth3, c[5], c[7]);
                        interp1(newPixels, newIndexWidth3 + 1, c[5], c[7]);
                        break;
                    }
                    case 247:
                    {
                        interp8(newPixels, newIndex, c[5], c[4]);
                        interp3(newPixels, newIndex + 1, c[5], c[4]);
                        newPixels[newIndex + 2] = c[5];
                        if (diff(w[2], w[6]))
                        {
                            newPixels[newIndex + 3] = c[5];
                        }
                        else
                        {
                            interp2(newPixels, newIndex + 3, c[5], c[2], c[6]);
                        }
                        interp8(newPixels, newIndexWidth, c[5], c[4]);
                        interp3(newPixels, newIndexWidth + 1, c[5], c[4]);
                        newPixels[newIndexWidth + 2] = c[5];
                        newPixels[newIndexWidth + 3] = c[5];
                        interp8(newPixels, newIndexWidth2, c[5], c[4]);
                        interp3(newPixels, newIndexWidth2 + 1, c[5], c[4]);
                        newPixels[newIndexWidth2 + 2] = c[5];
                        newPixels[newIndexWidth2 + 3] = c[5];
                        interp8(newPixels, newIndexWidth3, c[5], c[4]);
                        interp3(newPixels, newIndexWidth3 + 1, c[5], c[4]);
                        newPixels[newIndexWidth3 + 2] = c[5];
                        if (diff(w[6], w[8]))
                        {
                            newPixels[newIndexWidth3 + 3] = c[5];
                        }
                        else
                        {
                            interp2(newPixels, newIndexWidth3 + 3, c[5], c[8],
                                c[6]);
                        }
                        break;
                    }
                    case 255:
                    {
                        if (diff(w[4], w[2]))
                        {
                            newPixels[newIndex] = c[5];
                        }
                        else
                        {
                            interp2(newPixels, newIndex, c[5], c[2], c[4]);
                        }
                        newPixels[newIndex + 1] = c[5];
                        newPixels[newIndex + 2] = c[5];
                        if (diff(w[2], w[6]))
                        {
                            newPixels[newIndex + 3] = c[5];
                        }
                        else
                        {
                            interp2(newPixels, newIndex + 3, c[5], c[2], c[6]);
                        }
                        newPixels[newIndexWidth] = c[5];
                        newPixels[newIndexWidth + 1] = c[5];
                        newPixels[newIndexWidth + 2] = c[5];
                        newPixels[newIndexWidth + 3] = c[5];
                        newPixels[newIndexWidth2] = c[5];
                        newPixels[newIndexWidth2 + 1] = c[5];
                        newPixels[newIndexWidth2 + 2] = c[5];
                        newPixels[newIndexWidth2 + 3] = c[5];
                        if (diff(w[8], w[4]))
                        {
                            newPixels[newIndexWidth3] = c[5];
                        }
                        else
                        {
                            interp2(newPixels, newIndexWidth3, c[5], c[8], c[4]);
                        }
                        newPixels[newIndexWidth3 + 1] = c[5];
                        newPixels[newIndexWidth3 + 2] = c[5];
                        if (diff(w[6], w[8]))
                        {
                            newPixels[newIndexWidth3 + 3] = c[5];
                        }
                        else
                        {
                            interp2(newPixels, newIndexWidth3 + 3, c[5], c[8],
                                c[6]);
                        }
                        break;
                    }

                }

                index++;
                newIndex += 4;
            }
            newIndex += newWidth * 3;
        }

        return newPixels;
    }


    /**
     * @see _ImageFilter#getImageType()
     */

    @Override
    public int getImageType()
    {
        return BufferedImage.TYPE_INT_RGB;
    }
}
