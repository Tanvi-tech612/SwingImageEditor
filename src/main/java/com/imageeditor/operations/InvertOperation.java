package com.imageeditor.operations;

import java.awt.image.BufferedImage;

public class InvertOperation implements ImageOperation {

    @Override
    public BufferedImage apply(BufferedImage image) {

        BufferedImage result = new BufferedImage(
                image.getWidth(),
                image.getHeight(),
                BufferedImage.TYPE_INT_ARGB
        );

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {

                int rgb = image.getRGB(x, y);

                int alpha = (rgb >> 24) & 255;
                int r = (rgb >> 16) & 255;
                int g = (rgb >> 8) & 255;
                int b = rgb & 255;

                r = 255 - r;
                g = 255 - g;
                b = 255 - b;

                int newRgb = (alpha << 24)
                        | (r << 16)
                        | (g << 8)
                        | b;

                result.setRGB(x, y, newRgb);
            }
        }

        return result;
    }
}