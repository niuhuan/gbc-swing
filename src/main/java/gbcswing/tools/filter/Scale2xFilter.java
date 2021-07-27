/*
 * Copyright (C) 2006-2012 Klaus Reimer <k@ailis.de>
 * Based on public domain code ImageScale2x and RawScale2x from Kevin Glass.
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
 * Java implementation of the scale2x magnification filters. Based on the Java
 * classes ImageScale2x and RawScale2x from Kevin Glass (which were released
 * into the public domain).
 *
 * @author niuhuan (niuhuancn@outlook.com)
 */

public class Scale2xFilter extends _ImageFilterAbstract
{
    /** The src image data */
    private int[] srcImage;

    /** The dest image data */
    private int[] dstImage;

    /** The width of the source image */
    private int width;

    /** The height of the source image */
    private int height;


    /**
     * @see _ImageFilter#getScaleFactor()
     */

    @Override
    public int getScaleFactor()
    {
        return 2;
    }


    /**
     * @see _ImageFilter#scale(int[], int, int)
     */

    @Override
    public int[] scale(final int[] pixels, final int width, final int height)
    {
        this.width = width;
        this.height = height;
        this.srcImage = pixels;
        this.dstImage = new int[pixels.length * 4];

        for (int x = 0; x < this.width; x++)
        {
            for (int y = 0; y < this.height; y++)
            {
                process(x, y);
            }
        }

        return this.dstImage;
    }


    /**
     * Check if two pixels are different. Place holder for maybe some clever
     * code about tolerance checking
     *
     * @param a
     *            The first pixel value
     * @param b
     *            The second pixel value
     * @return True if the pixels are different
     */

    private boolean different(final int a, final int b)
    {
        return a != b;
    }


    /**
     * Set a pixel in the destination image data
     *
     * @param x
     *            The x location of the pixel to set
     * @param y
     *            The y location of the pixel to set
     * @param p
     *            The value of the pixel to set
     */

    private void setDestPixel(final int x, final int y, final int p)
    {
        this.dstImage[x + (y * this.width * 2)] = p;
    }


    /**
     * Get a pixel from the source image. This handles bonds checks and resolves
     * to edge pixels
     *
     * @param x
     *            The x location of the pixel to retrieve
     * @param y
     *            The y location of the pixel to retrieve
     * @return The pixel value at the specified location
     */

    private int getSourcePixel(final int x, final int y)
    {
        int x2 = Math.max(0, x);
        x2 = Math.min(this.width - 1, x2);
        int y2 = Math.max(0, y);
        y2 = Math.min(this.height - 1, y2);

        return this.srcImage[x2 + (y2 * this.width)];
    }


    /**
     * Process a specific pixel. This will generate 4 pixels in the destination
     * image based on the scale2x algorithm
     *
     * @param x
     *            The x location in the source image of the pixel to process
     * @param y
     *            The y location in the source image of the pixel to process
     */

    private void process(final int x, final int y)
    {
        final int B = getSourcePixel(x, y - 1);
        final int D = getSourcePixel(x - 1, y);
        final int E = getSourcePixel(x, y);
        final int F = getSourcePixel(x + 1, y);
        final int H = getSourcePixel(x, y + 1);
        int E0 = E;
        int E1 = E;
        int E2 = E;
        int E3 = E;

        if (different(B, H) && different(D, F))
        {
            E0 = !different(D, B) ? D : E;
            E1 = !different(B, F) ? F : E;
            E2 = !different(D, H) ? D : E;
            E3 = !different(H, F) ? F : E;
        }

        setDestPixel(x * 2, y * 2, E0);
        setDestPixel((x * 2) + 1, y * 2, E1);
        setDestPixel((x * 2), (y * 2) + 1, E2);
        setDestPixel((x * 2) + 1, (y * 2) + 1, E3);
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
