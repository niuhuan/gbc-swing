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

import java.awt.image.BufferedImage;


/**
 * The interface for all scale filters.
 * 
 * @author niuhuan (niuhuancn@outlook.com)
 */

public interface _ImageFilter
{
    /**
     * Returns the scale factor of the scale filters (For example 2, 3 or 4).
     * 
     * @return The scale factor.
     */

    int getScaleFactor();


    /**
     * Returns the image type of the result this scale filters will produce. It
     * may return -1 to indiciate that the filters is using the input image type
     * as output image type (The nearest neighbor and normal filters will do this
     * for example because the first one is not adding additional colors (so the
     * image type is not important) and the second one does not change anything
     * at all).
     * 
     * @return The image type of the scaled result or -1 if it is always the
     *         input image type
     */

    int getImageType();


    /**
     * Scales the specified image and returns the new scaled image.
     * 
     * @param image
     *            The original image to scale
     * @return The scaled image
     */

    BufferedImage scale(BufferedImage image);


    /**
     * Scales the specified pixel data (In RGB888 format, alpha values will be
     * lost). Returns pixel array will also be in RGB888 format.
     * 
     * @param pixels
     *            The pixel array
     * @param width
     *            The image width
     * @param height
     *            The image height
     * @return The scaled pixel data
     */

    int[] scale(int[] pixels, int width, int height);
}
