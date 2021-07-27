package gbc;

import java.awt.image.BufferedImage;

public interface ScreenListener {
    void onFrameReady(BufferedImage image, int skipFrame);
}
