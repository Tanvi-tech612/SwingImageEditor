package com.imageeditor.command;

import com.imageeditor.model.ImageDocument;
import com.imageeditor.operations.ImageOperation;

import java.awt.image.BufferedImage;

public class ImageOperationCommand implements Command {

    private ImageDocument document;
    private ImageOperation operation;
    private BufferedImage previousImage;

    public ImageOperationCommand(
            ImageDocument document,
            ImageOperation operation) {

        this.document = document;
        this.operation = operation;
    }

    @Override
    public void execute() {

        previousImage = document.getImage();

        BufferedImage newImage =
                operation.apply(previousImage);

        document.setImage(newImage);
    }

    @Override
    public void undo() {

        document.setImage(previousImage);
    }
}
