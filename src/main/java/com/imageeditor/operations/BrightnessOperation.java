package com.imageeditor.operations;

import java.awt.image.BufferedImage;

public class BrightnessOperation implements ImageOperation {

    private int amount;

    public BrightnessOperation(int amount) {
        this.amount = amount;
    }

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

                r = clamp(r + amount);
                g = clamp(g + amount);
                b = clamp(b + amount);

                int newRgb = (alpha << 24)
                        | (r << 16)
                        | (g << 8)
                        | b;

                result.setRGB(x, y, newRgb);
            }
        }

        return result;
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}