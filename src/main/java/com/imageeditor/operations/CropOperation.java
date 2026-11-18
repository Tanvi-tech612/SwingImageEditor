package com.imageeditor.operations;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

public class CropOperation implements ImageOperation {

    private final Rectangle cropArea;

    public CropOperation(Rectangle cropArea) {
        if (cropArea == null) {
            throw new IllegalArgumentException("Crop area cannot be null.");
        }
        if (cropArea.x < 0 || cropArea.y < 0 || cropArea.width <= 0 || cropArea.height <= 0) {
            throw new IllegalArgumentException("Invalid crop area: " + cropArea);
        }
        this.cropArea = new Rectangle(cropArea);
    }

    public Rectangle getCropArea() {
        return new Rectangle(cropArea);
    }

    @Override
    public BufferedImage apply(BufferedImage image) {
        if (image == null) {
            throw new IllegalArgumentException("Image cannot be null.");
        }

        if ((long) cropArea.x + cropArea.width > image.getWidth()
                || (long) cropArea.y + cropArea.height > image.getHeight()) {
            throw new IllegalArgumentException("Crop area extends outside image boundaries.");
        }

        return applyToLayer(image);
    }

    /**
     * Crops a layer image using document-space coordinates.
     * Always produces an ARGB image matching cropArea's exact width and height,
     * preserving transparency and relative alignment even if the layer is smaller
     * than the document canvas or does not intersect the crop rectangle.
     */
    public BufferedImage applyToLayer(BufferedImage image) {
        if (image == null) {
            return null;
        }

        BufferedImage cropped = new BufferedImage(
                cropArea.width,
                cropArea.height,
                BufferedImage.TYPE_INT_ARGB
        );

        int srcX1 = Math.max(0, cropArea.x);
        int srcY1 = Math.max(0, cropArea.y);
        int srcX2 = Math.min(image.getWidth(), cropArea.x + cropArea.width);
        int srcY2 = Math.min(image.getHeight(), cropArea.y + cropArea.height);

        if (srcX2 > srcX1 && srcY2 > srcY1) {
            int dstX1 = srcX1 - cropArea.x;
            int dstY1 = srcY1 - cropArea.y;
            int dstX2 = srcX2 - cropArea.x;
            int dstY2 = srcY2 - cropArea.y;

            Graphics2D g2d = cropped.createGraphics();
            try {
                g2d.setComposite(AlphaComposite.Src);
                g2d.drawImage(
                        image,
                        dstX1, dstY1, dstX2, dstY2,
                        srcX1, srcY1, srcX2, srcY2,
                        null
                );
            } finally {
                g2d.dispose();
            }
        }

        return cropped;
    }
}
