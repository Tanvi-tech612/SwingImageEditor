package com.imageeditor.model;

import java.awt.image.BufferedImage;
import java.util.UUID;

public class Layer {

    private final String id;
    private String name;
    private BufferedImage image;
    private boolean visible;
    private float opacity;

    public Layer(String name, BufferedImage image) {
        this(UUID.randomUUID().toString(), name, image, true, 1.0f);
    }

    public Layer(String id, String name, BufferedImage image, boolean visible, float opacity) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.name = name != null ? name : "Untitled Layer";
        this.image = image;
        this.visible = visible;
        this.opacity = clampOpacity(opacity);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BufferedImage getImage() {
        return image;
    }

    public void setImage(BufferedImage image) {
        this.image = image;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public float getOpacity() {
        return opacity;
    }

    public void setOpacity(float opacity) {
        this.opacity = clampOpacity(opacity);
    }

    public int getWidth() {
        return image != null ? image.getWidth() : 0;
    }

    public int getHeight() {
        return image != null ? image.getHeight() : 0;
    }

    private static float clampOpacity(float opacity) {
        return Math.max(0.0f, Math.min(1.0f, opacity));
    }
}