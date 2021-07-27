/*
 * Copyright (C) 2006-2012 Klaus Reimer <k@ailis.de>
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


/**
 * A simple nearest neighbour 4x algorithm. Because of the simplicity of
 * this algorithm it also supports alpha channels.
 *
 * @author niuhuan (niuhuancn@outlook.com)
 */

public class Nearest4xFilter extends _ImageFilterAbstract
{
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
    public int[] scale(final int[] pixels, final int width, final int height)
    {
        int newWidth, newWidth2, newWidth3, newHeight, index;
        int[] newPixels;
        int line;

        newWidth = width * 4;
        newHeight = height * 4;
        newWidth2 = newWidth * 2;
        newWidth3 = newWidth * 3;
        newPixels = new int[newWidth * newHeight];
        line = 0;
        index = 0;
        for (int y = 0; y < height; y++)
        {
            for (int x = 0; x < width; x++)
            {
                int b, tmp;

                b = pixels[line + x];
                newPixels[index] = b;
                newPixels[index + 1 ] = b;
                newPixels[index + 2 ] = b;
                newPixels[index + 3 ] = b;
                tmp = index + newWidth;
                newPixels[tmp] = b;
                newPixels[tmp + 1] = b;
                newPixels[tmp + 2] = b;
                newPixels[tmp + 3] = b;
                tmp = index + newWidth2;
                newPixels[tmp] = b;
                newPixels[tmp + 1] = b;
                newPixels[tmp + 2] = b;
                newPixels[tmp + 3] = b;
                tmp = index + newWidth3;
                newPixels[tmp] = b;
                newPixels[tmp + 1] = b;
                newPixels[tmp + 2] = b;
                newPixels[tmp + 3] = b;

                index += 4;
            }
            line += width;
            index += newWidth + newWidth + newWidth;
        }
        return newPixels;
    }


    /**
     * @see _ImageFilter#getImageType()
     */

    @Override
    public int getImageType()
    {
        return -1;
    }
}
