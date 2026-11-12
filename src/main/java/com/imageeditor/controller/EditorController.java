
package com.imageeditor.controller;

import com.imageeditor.model.ImageDocument;
import com.imageeditor.operations.ImageOperation;

import javax.imageio.ImageIO;
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
}
