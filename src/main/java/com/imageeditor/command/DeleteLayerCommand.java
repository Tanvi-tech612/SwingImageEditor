package com.imageeditor.command;

import com.imageeditor.model.ImageDocument;
import com.imageeditor.model.Layer;

public class DeleteLayerCommand implements Command {

    private final ImageDocument document;
    private final Layer layer;
    private final int index;

    public DeleteLayerCommand(ImageDocument document, Layer layer) {
        this.document = document;
        this.layer = layer;
        this.index = document != null && layer != null ? document.indexOf(layer) : -1;
    }

    public DeleteLayerCommand(ImageDocument document, Layer layer, int index) {
        this.document = document;
        this.layer = layer;
        this.index = index;
    }

    @Override
    public void execute() {
        if (document != null && layer != null) {
            document.removeLayer(layer);
        }
    }

    @Override
    public void undo() {
        if (document != null && layer != null && index >= 0) {
            document.addLayer(index, layer);
            document.setSelectedLayer(layer);
        }
    }

    public Layer getLayer() {
        return layer;
    }

    public int getIndex() {
        return index;
    }
}
