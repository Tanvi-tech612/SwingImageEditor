package com.imageeditor.command;

import com.imageeditor.model.Layer;

public class MoveLayerCommand implements Command {

    private final Layer targetLayer;
    private final int originalX;
    private final int originalY;
    private final int newX;
    private final int newY;

    public MoveLayerCommand(Layer targetLayer, int originalX, int originalY, int newX, int newY) {
        if (targetLayer == null) {
            throw new IllegalArgumentException("Target layer cannot be null.");
        }
        this.targetLayer = targetLayer;
        this.originalX = originalX;
        this.originalY = originalY;
        this.newX = newX;
        this.newY = newY;
    }

    public MoveLayerCommand(Layer targetLayer, int newX, int newY) {
        this(
                targetLayer,
                targetLayer != null ? targetLayer.getX() : 0,
                targetLayer != null ? targetLayer.getY() : 0,
                newX,
                newY
        );
    }

    @Override
    public void execute() {
        targetLayer.setPosition(newX, newY);
    }

    @Override
    public void undo() {
        targetLayer.setPosition(originalX, originalY);
    }

    public void redo() {
        execute();
    }

    public Layer getTargetLayer() {
        return targetLayer;
    }

    public int getOriginalX() {
        return originalX;
    }

    public int getOriginalY() {
        return originalY;
    }

    public int getNewX() {
        return newX;
    }

    public int getNewY() {
        return newY;
    }
}
