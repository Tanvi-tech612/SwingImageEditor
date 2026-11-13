package com.imageeditor.command;

import com.imageeditor.model.ImageDocument;
import com.imageeditor.model.Layer;

public class AddLayerCommand implements Command {

    private final ImageDocument document;
    private final Layer layer;
    private final int index;

    public AddLayerCommand(ImageDocument document, Layer layer, int index) {
        this.document = document;
        this.layer = layer;
        this.index = index;
    }

    public AddLayerCommand(ImageDocument document, Layer layer) {
        this(document, layer, document != null ? document.getLayerCount() : 0);
    }

    @Override
    public void execute() {
        if (document != null && layer != null) {
            document.addLayer(index, layer);
            document.setSelectedLayer(layer);
        }
    }

    @Override
    public void undo() {
        if (document != null && layer != null) {
            document.removeLayer(layer);
        }
    }

    public Layer getLayer() {
        return layer;
    }

    public int getIndex() {
        return index;
    }
}
