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
 * This is just a dummy because it simply does nothing. It returns the values
 * as they are, simulating a scale factor of x1.
 *
 * @author niuhuan (niuhuancn@outlook.com)
 */

public class NormalFilter implements _ImageFilter {

    /**
     * @see _ImageFilter#getScaleFactor()
     */
    @Override
    public int getScaleFactor() {
        return 1;
    }

    /**
     * @see _ImageFilter#scale(int[], int, int)
     */
    @Override
    public int[] scale(final int[] pixels, final int width, final int height) {
        throw new RuntimeException("Not yet implemented");
    }

    /**
     * @see _ImageFilter#scale(BufferedImage)
     */
    @Override
    public BufferedImage scale(final BufferedImage image) {
        BufferedImage bufferedImage = new BufferedImage(image.getWidth() * getScaleFactor(), image.getHeight() * getScaleFactor(), BufferedImage.TYPE_INT_RGB);
        bufferedImage.getGraphics().drawImage(image, 0, 0, image.getWidth() * getScaleFactor(), image.getHeight() * getScaleFactor(), null);
        return bufferedImage;
    }

    /**
     * @see _ImageFilter#getImageType()
     */
    @Override
    public int getImageType() {
        return BufferedImage.TYPE_INT_RGB;
    }
}
