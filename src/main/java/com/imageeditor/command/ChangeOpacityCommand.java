package com.imageeditor.command;

import com.imageeditor.model.Layer;

public class ChangeOpacityCommand implements Command {

    private final Layer layer;
    private final float previousOpacity;
    private final float newOpacity;

    public ChangeOpacityCommand(Layer layer, float previousOpacity, float newOpacity) {
        this.layer = layer;
        this.previousOpacity = previousOpacity;
        this.newOpacity = newOpacity;
    }

    public ChangeOpacityCommand(Layer layer, float newOpacity) {
        this(layer, layer != null ? layer.getOpacity() : 1.0f, newOpacity);
    }

    @Override
    public void execute() {
        if (layer != null) {
            layer.setOpacity(newOpacity);
        }
    }

    @Override
    public void undo() {
        if (layer != null) {
            layer.setOpacity(previousOpacity);
        }
    }

    public Layer getLayer() {
        return layer;
    }

    public float getPreviousOpacity() {
        return previousOpacity;
    }

    public float getNewOpacity() {
        return newOpacity;
    }
}
