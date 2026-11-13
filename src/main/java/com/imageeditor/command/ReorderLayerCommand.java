package com.imageeditor.command;

import com.imageeditor.model.ImageDocument;

public class ReorderLayerCommand implements Command {

    private final ImageDocument document;
    private final int oldIndex;
    private final int newIndex;

    public ReorderLayerCommand(ImageDocument document, int oldIndex, int newIndex) {
        this.document = document;
        this.oldIndex = oldIndex;
        this.newIndex = newIndex;
    }

    @Override
    public void execute() {
        if (document != null) {
            document.moveLayer(oldIndex, newIndex);
        }
    }

    @Override
    public void undo() {
        if (document != null) {
            document.moveLayer(newIndex, oldIndex);
        }
    }

    public int getOldIndex() {
        return oldIndex;
    }

    public int getNewIndex() {
        return newIndex;
    }
}
