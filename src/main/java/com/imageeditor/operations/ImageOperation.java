package com.imageeditor.operations;

import java.awt.image.BufferedImage;

public interface ImageOperation {

    BufferedImage apply(BufferedImage image);
}