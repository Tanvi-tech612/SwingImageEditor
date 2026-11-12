package com.imageeditor.operations;

import java.awt.image.BufferedImage;

public class GrayscaleOperation implements ImageOperation {

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

                int r = (rgb >> 16) & 255;
                int g = (rgb >> 8) & 255;
                int b = rgb & 255;

                int gray = (r + g + b) / 3;

                int newRgb = (rgb & 0xFF000000)
                        | (gray << 16)
                        | (gray << 8)
                        | gray;

                result.setRGB(x, y, newRgb);
            }
        }

        return result;
    }
}