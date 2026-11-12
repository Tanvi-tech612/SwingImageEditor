package com.imageeditor.operations;

import java.awt.image.BufferedImage;

public class RotateOperation implements ImageOperation {

    @Override
    public BufferedImage apply(BufferedImage image) {

        int width = image.getWidth();
        int height = image.getHeight();

        BufferedImage result = new BufferedImage(
                height,
                width,
                BufferedImage.TYPE_INT_ARGB
        );

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {

                result.setRGB(height - 1 - y, x, image.getRGB(x, y));
            }
        }

        return result;
    }
}