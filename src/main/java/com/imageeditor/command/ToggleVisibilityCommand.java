package com.imageeditor.command;

import com.imageeditor.model.Layer;

public class ToggleVisibilityCommand implements Command {

    private final Layer layer;
    private final boolean previousVisibility;
    private final boolean newVisibility;

    public ToggleVisibilityCommand(Layer layer, boolean newVisibility) {
        this.layer = layer;
        this.previousVisibility = layer != null ? layer.isVisible() : false;
        this.newVisibility = newVisibility;
    }

    public ToggleVisibilityCommand(Layer layer) {
        this(layer, layer != null ? !layer.isVisible() : true);
    }

    @Override
    public void execute() {
        if (layer != null) {
            layer.setVisible(newVisibility);
        }
    }

    @Override
    public void undo() {
        if (layer != null) {
            layer.setVisible(previousVisibility);
        }
    }

    public Layer getLayer() {
        return layer;
    }

    public boolean getPreviousVisibility() {
        return previousVisibility;
    }

    public boolean getNewVisibility() {
        return newVisibility;
    }
}
