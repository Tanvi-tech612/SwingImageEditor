package com.imageeditor.controller;

import com.imageeditor.model.ImageDocument;

public class EditorController {

    private final ImageDocument document;

    public EditorController() {
        document = new ImageDocument();
    }

    public ImageDocument getDocument() {
        return document;
    }
}