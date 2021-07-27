package gbcswing.tools.filter;

public abstract class Hq0xFilter extends _ImageFilterAbstract{

    /**
     * Converts an RGB 888 color_style to RGB 565 color_style space. Alpha values will get
     * lost.
     *
     * @param rgb888
     *            The RGB 888 color_style value
     * @return The RGB 565 color_style
     */
    protected int convertRgb888To565(int rgb888)
    {
        return ((rgb888 & 0xf80000) >> 8) | ((rgb888 & 0xfc00) >> 5)
                | ((rgb888 & 0xf8) >> 3);
    }


    /**
     * Converts an RGB 888 color_style array to an RGB 565 color_style array. Alpha values
     * will get lost.
     *
     * @param rgb888
     *            The RGB 888 color_style array
     * @return The RGB 565 color_style array
     */
    protected int[] convertRgb888To565(int[] rgb888)
    {
        int max;
        int[] rgb565;
        max = rgb888.length;
        rgb565 = new int[max];
        for (int i = 0; i < max; i++)
        {
            rgb565[i] = convertRgb888To565(rgb888[i]);
        }
        return rgb565;
    }

}
