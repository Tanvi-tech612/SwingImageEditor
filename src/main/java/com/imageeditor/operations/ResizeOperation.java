package com.imageeditor.operations;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

public class ResizeOperation implements ImageOperation {

    public static final int MIN_DIMENSION = 1;
    public static final int MAX_DIMENSION = 10_000;
    public static final long MAX_TOTAL_PIXELS = 50_000_000L;

    private final int targetWidth;
    private final int targetHeight;

    public ResizeOperation(int targetWidth, int targetHeight) {
        validateDimensions(targetWidth, targetHeight);
        this.targetWidth = targetWidth;
        this.targetHeight = targetHeight;
    }

    public static void validateDimensions(int width, int height) {
        if (width < MIN_DIMENSION || height < MIN_DIMENSION) {
            throw new IllegalArgumentException(
                    "Width and height must be positive integers (at least " + MIN_DIMENSION + " px)."
            );
        }
        if (width > MAX_DIMENSION || height > MAX_DIMENSION) {
            throw new IllegalArgumentException(
                    "Width and height cannot exceed " + MAX_DIMENSION + " px."
            );
        }
        long totalPixels = (long) width * (long) height;
        if (totalPixels > MAX_TOTAL_PIXELS) {
            throw new IllegalArgumentException(
                    "Requested dimensions (" + width + " × " + height + ") exceed the safe maximum of "
                            + MAX_TOTAL_PIXELS + " pixels and could cause memory issues."
            );
        }
    }

    public int getTargetWidth() {
        return targetWidth;
    }

    public int getTargetHeight() {
        return targetHeight;
    }

    @Override
    public BufferedImage apply(BufferedImage image) {
        if (image == null) {
            throw new IllegalArgumentException("Image cannot be null.");
        }

        BufferedImage resized = new BufferedImage(
                targetWidth,
                targetHeight,
                BufferedImage.TYPE_INT_ARGB
        );

        Graphics2D g2d = resized.createGraphics();
        try {
            g2d.setComposite(AlphaComposite.Src);
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);

            g2d.drawImage(image, 0, 0, targetWidth, targetHeight, null);
        } finally {
            g2d.dispose();
        }

        return resized;
    }
}

