package com.imageeditor.command;

import com.imageeditor.model.Layer;
import com.imageeditor.operations.ResizeOperation;

import java.awt.image.BufferedImage;

public class LayerResizeCommand implements Command {

    private final Layer targetLayer;
    private final int newWidth;
    private final int newHeight;
    private final BufferedImage originalImage;
    private BufferedImage resizedImage;

    public LayerResizeCommand(Layer targetLayer, int newWidth, int newHeight) {
        if (targetLayer == null) {
            throw new IllegalArgumentException("Target layer cannot be null.");
        }
        if (targetLayer.getImage() == null) {
            throw new IllegalStateException("Selected layer has no image to resize.");
        }
        ResizeOperation.validateDimensions(newWidth, newHeight);

        this.targetLayer = targetLayer;
        this.newWidth = newWidth;
        this.newHeight = newHeight;
        this.originalImage = targetLayer.getImage();
    }

    @Override
    public void execute() {
        if (resizedImage == null) {
            ResizeOperation resizeOperation = new ResizeOperation(newWidth, newHeight);
            resizedImage = resizeOperation.apply(originalImage);
        }
        targetLayer.setImage(resizedImage);
    }

    @Override
    public void undo() {
        targetLayer.setImage(originalImage);
    }

    public void redo() {
        execute();
    }

    public Layer getTargetLayer() {
        return targetLayer;
    }

    public BufferedImage getOriginalImage() {
        return originalImage;
    }

    public BufferedImage getResizedImage() {
        return resizedImage;
    }

    public int getNewWidth() {
        return newWidth;
    }

    public int getNewHeight() {
        return newHeight;
    }
}

