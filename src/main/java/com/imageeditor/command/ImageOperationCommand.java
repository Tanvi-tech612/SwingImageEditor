package com.imageeditor.command;

import com.imageeditor.model.ImageDocument;
import com.imageeditor.model.Layer;
import com.imageeditor.operations.ImageOperation;

import java.awt.image.BufferedImage;

public class ImageOperationCommand implements Command {

    private final ImageDocument document;
    private final Layer targetLayer;
    private final ImageOperation operation;
    private BufferedImage previousImage;
    private BufferedImage newImage;

    public ImageOperationCommand(
            ImageDocument document,
            ImageOperation operation) {
        this(document, document != null ? document.getSelectedLayer() : null, operation);
    }

    public ImageOperationCommand(
            ImageDocument document,
            Layer targetLayer,
            ImageOperation operation) {
        this.document = document;
        this.targetLayer = targetLayer != null ? targetLayer : (document != null ? document.getSelectedLayer() : null);
        this.operation = operation;
    }

    @Override
    public void execute() {
        if (targetLayer == null || operation == null) {
            return;
        }

        if (newImage == null) {
            previousImage = targetLayer.getImage();
            if (previousImage != null) {
                newImage = operation.apply(previousImage);
            }
        }

        if (newImage != null) {
            targetLayer.setImage(newImage);
        }
    }

    @Override
    public void undo() {
        if (targetLayer != null && previousImage != null) {
            targetLayer.setImage(previousImage);
        }
    }

    public Layer getTargetLayer() {
        return targetLayer;
    }
}
