
package com.imageeditor.operations;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class OperationTest {

    public static void main(String[] args) {

        try {
            File inputFile = new File("test-image.jpg");
            BufferedImage original = ImageIO.read(inputFile);

            if (original == null) {
                System.out.println("Could not load image!");
                return;
            }

            BufferedImage grayscale =
                    new GrayscaleOperation().apply(original);

            BufferedImage inverted =
                    new InvertOperation().apply(original);

            BufferedImage brighter =
                    new BrightnessOperation(50).apply(original);

            BufferedImage rotated =
                    new RotateOperation().apply(original);

            ImageIO.write(grayscale, "png", new File("grayscale.png"));
            ImageIO.write(inverted, "png", new File("inverted.png"));
            ImageIO.write(brighter, "png", new File("brighter.png"));
            ImageIO.write(rotated, "png", new File("rotated.png"));

            System.out.println("All operations completed successfully!");

        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
