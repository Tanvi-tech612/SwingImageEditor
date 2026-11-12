package com.imageeditor.model;

import java.awt.image.BufferedImage;

public class ImageDocument {

    private BufferedImage image;

    public ImageDocument() {
        this.image = null;
    }

    public BufferedImage getImage() {
        return image;
    }

    public void setImage(BufferedImage image) {
        this.image = image;
    }

    public boolean hasImage() {
        return image != null;
    }
}