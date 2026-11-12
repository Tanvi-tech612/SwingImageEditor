package com.imageeditor.model;

import java.awt.image.BufferedImage;

public class Layer {

    private String name;
    private BufferedImage image;
    private boolean visible;

    public Layer(String name, BufferedImage image) {
        this.name = name;
        this.image = image;
        this.visible = true;
    }

    public String getName() {
        return name;
    }

    public BufferedImage getImage() {
        return image;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }
}