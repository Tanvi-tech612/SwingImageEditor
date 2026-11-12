
package com.imageeditor.controller;

import com.imageeditor.model.ImageDocument;
import com.imageeditor.operations.ImageOperation;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class EditorController {

    private final ImageDocument document;

    public EditorController() {
        document = new ImageDocument();
    }

    public ImageDocument getDocument() {
        return document;
    }

    public void loadImage(File file) throws IOException {

        BufferedImage image = ImageIO.read(file);

        if (image == null) {
            throw new IOException("Unsupported image format.");
        }

        document.setImage(image);
    }

    public void applyOperation(ImageOperation operation) {

        if (!document.hasImage()) {
            throw new IllegalStateException("Please open an image first.");
        }

        BufferedImage currentImage = document.getImage();

        BufferedImage processedImage = operation.apply(currentImage);

        document.setImage(processedImage);
    }

    public void saveImage(File file, String format) throws IOException {

        if (!document.hasImage()) {
            throw new IllegalStateException("Please open an image first.");
        }

        BufferedImage currentImage = document.getImage();
        BufferedImage imageToSave = currentImage;

        if ("jpg".equalsIgnoreCase(format) || "jpeg".equalsIgnoreCase(format)) {
            BufferedImage rgbImage = new BufferedImage(
                    currentImage.getWidth(),
                    currentImage.getHeight(),
                    BufferedImage.TYPE_INT_RGB
            );
            Graphics2D g = rgbImage.createGraphics();
            g.drawImage(currentImage, 0, 0, Color.WHITE, null);
            g.dispose();
            imageToSave = rgbImage;
            format = "jpg";
        }

        boolean written = ImageIO.write(imageToSave, format, file);

        if (!written) {
            throw new IOException("Failed to save image in format: " + format);
        }
    }

    public void saveImage(File file) throws IOException {

        String name = file.getName();
        int dotIndex = name.lastIndexOf('.');

        if (dotIndex == -1 || dotIndex == name.length() - 1) {
            throw new IllegalArgumentException("File must have a valid extension (.png, .jpg, .jpeg).");
        }

        String ext = name.substring(dotIndex + 1).toLowerCase();

        if (!ext.equals("png") && !ext.equals("jpg") && !ext.equals("jpeg")) {
            throw new IllegalArgumentException("Unsupported image format: " + ext);
        }

        String format = ext.equals("jpeg") ? "jpg" : ext;
        saveImage(file, format);
    }
}
