
package com.imageeditor.controller;

import com.imageeditor.model.ImageDocument;
import com.imageeditor.operations.ImageOperation;
import com.imageeditor.command.CommandHistory;
import com.imageeditor.command.ImageOperationCommand;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class EditorController {

    private final ImageDocument document;
    private final CommandHistory commandHistory;

    public EditorController() {
        document = new ImageDocument();
        commandHistory = new CommandHistory();
    }

    public ImageDocument getDocument() {
        return document;
    }

    public void loadImage(File file) throws IOException {

        BufferedImage image = ImageIO.read(file);

        if (image == null) {
            throw new IOException("Unsupported image format.");
        }

        document.setImage(image);
        commandHistory.clear();
    }

    public void applyOperation(ImageOperation operation) {

        if (!document.hasImage()) {
            throw new IllegalStateException("Please open an image first.");
        }

        ImageOperationCommand command =
                new ImageOperationCommand(document, operation);

        commandHistory.executeCommand(command);
    }

    public void undo() {
        commandHistory.undo();
    }

    public void redo() {
        commandHistory.redo();
    }

    public boolean canUndo() {
        return commandHistory.canUndo();
    }

    public boolean canRedo() {
        return commandHistory.canRedo();
    }
}
