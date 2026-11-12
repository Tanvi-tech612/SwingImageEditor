package com.imageeditor.operations;

import java.awt.image.BufferedImage;

public class InvertOperation implements ImageOperation {

    @Override
    public BufferedImage apply(BufferedImage image) {
        return image;
    }
}