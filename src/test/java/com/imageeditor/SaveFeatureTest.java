package com.imageeditor;

import com.imageeditor.controller.EditorController;
import com.imageeditor.operations.GrayscaleOperation;
import com.imageeditor.operations.InvertOperation;
import com.imageeditor.ui.MainFrame;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class SaveFeatureTest {

    public static void main(String[] args) {
        System.out.println("Running Save Feature Tests...");
        int passed = 0;
        int total = 0;

        // Test 1: Verify Save Image button exists in MainFrame
        total++;
        try {
            // Run on headless-safe or inspect components
            MainFrame frame = new MainFrame();
            boolean foundSaveButton = false;
            for (Component comp : frame.getContentPane().getComponents()) {
                if (comp instanceof JPanel panel) {
                    for (Component inner : panel.getComponents()) {
                        if (inner instanceof JButton btn && "Save Image".equals(btn.getText())) {
                            foundSaveButton = true;
                            break;
                        }
                    }
                }
            }
            frame.dispose();
            if (foundSaveButton) {
                System.out.println("PASS: Save Image button found in MainFrame toolbar.");
                passed++;
            } else {
                System.err.println("FAIL: Save Image button not found in MainFrame toolbar.");
            }
        } catch (Exception e) {
            System.err.println("FAIL: Exception checking MainFrame: " + e.getMessage());
        }

        // Test 2: Error when no image is loaded
        total++;
        try {
            EditorController controller = new EditorController();
            File tmp = File.createTempFile("test_save", ".png");
            tmp.deleteOnExit();
            try {
                controller.saveImage(tmp, "png");
                System.err.println("FAIL: Expected IllegalStateException when no image is loaded.");
            } catch (IllegalStateException e) {
                System.out.println("PASS: Properly rejected saving when no image loaded: " + e.getMessage());
                passed++;
            }
        } catch (Exception e) {
            System.err.println("FAIL: Test 2 exception: " + e.getMessage());
        }

        // Test 3: Save currently edited image as PNG and verify it is edited, not original
        total++;
        try {
            EditorController controller = new EditorController();
            File inputFile = new File("test-image.jpg");
            controller.loadImage(inputFile);

            BufferedImage origImage = ImageIO.read(inputFile);
            int origRgb = origImage.getRGB(50, 50);

            // Apply Invert
            controller.applyOperation(new InvertOperation());

            File outPng = File.createTempFile("test_inverted", ".png");
            outPng.deleteOnExit();
            controller.saveImage(outPng, "png");

            // Read back saved image
            BufferedImage savedPng = ImageIO.read(outPng);
            if (savedPng == null) {
                throw new AssertionError("Saved PNG could not be read.");
            }
            int savedRgb = savedPng.getRGB(50, 50);

            // Calculate expected inverted RGB
            int origR = (origRgb >> 16) & 255;
            int origG = (origRgb >> 8) & 255;
            int origB = origRgb & 255;
            int expectedR = 255 - origR;
            int expectedG = 255 - origG;
            int expectedB = 255 - origB;

            int savedR = (savedRgb >> 16) & 255;
            int savedG = (savedRgb >> 8) & 255;
            int savedB = savedRgb & 255;

            if (savedR == expectedR && savedG == expectedG && savedB == expectedB) {
                System.out.println("PASS: Saved PNG matches edited (inverted) image, NOT original image.");
                passed++;
            } else {
                System.err.printf("FAIL: Saved pixel (%d,%d,%d) != Expected inverted (%d,%d,%d), Original was (%d,%d,%d)%n",
                        savedR, savedG, savedB, expectedR, expectedG, expectedB, origR, origG, origB);
            }
        } catch (Exception e) {
            System.err.println("FAIL: Test 3 exception: " + e.getMessage());
            e.printStackTrace();
        }

        // Test 4: Save currently edited image as JPG
        total++;
        try {
            EditorController controller = new EditorController();
            File inputFile = new File("test-image.jpg");
            controller.loadImage(inputFile);

            // Apply Grayscale
            controller.applyOperation(new GrayscaleOperation());

            File outJpg = File.createTempFile("test_gray", ".jpg");
            outJpg.deleteOnExit();
            controller.saveImage(outJpg, "jpg");

            BufferedImage savedJpg = ImageIO.read(outJpg);
            if (savedJpg == null) {
                throw new AssertionError("Saved JPG could not be read.");
            }

            int savedRgb = savedJpg.getRGB(50, 50);
            int savedR = (savedRgb >> 16) & 255;
            int savedG = (savedRgb >> 8) & 255;
            int savedB = savedRgb & 255;

            // In grayscale with JPEG lossy compression, R, G, B should be almost identical (within small tolerance)
            if (Math.abs(savedR - savedG) <= 5 && Math.abs(savedG - savedB) <= 5) {
                System.out.println("PASS: Saved JPG successfully created and contains grayscale edited image.");
                passed++;
            } else {
                System.err.printf("FAIL: Saved JPG RGB values diverge too much for grayscale: R=%d, G=%d, B=%d%n",
                        savedR, savedG, savedB);
            }
        } catch (Exception e) {
            System.err.println("FAIL: Test 4 exception: " + e.getMessage());
            e.printStackTrace();
        }

        // Test 5: Invalid formats properly rejected
        total++;
        try {
            EditorController controller = new EditorController();
            File inputFile = new File("test-image.jpg");
            controller.loadImage(inputFile);

            File outGif = new File("test_invalid.gif");
            try {
                controller.saveImage(outGif);
                System.err.println("FAIL: Should have thrown IllegalArgumentException for .gif");
            } catch (IllegalArgumentException e) {
                System.out.println("PASS: Invalid format .gif correctly rejected: " + e.getMessage());
                passed++;
            }
        } catch (Exception e) {
            System.err.println("FAIL: Test 5 exception: " + e.getMessage());
        }

        // Test 6: Auto-detection of extension in saveImage(File)
        total++;
        try {
            EditorController controller = new EditorController();
            File inputFile = new File("test-image.jpg");
            controller.loadImage(inputFile);

            File testPng = File.createTempFile("auto_test", ".png");
            testPng.deleteOnExit();
            controller.saveImage(testPng);

            File testJpg = File.createTempFile("auto_test", ".jpeg");
            testJpg.deleteOnExit();
            controller.saveImage(testJpg);

            if (ImageIO.read(testPng) != null && ImageIO.read(testJpg) != null) {
                System.out.println("PASS: auto-detection of format from .png and .jpeg worked.");
                passed++;
            } else {
                System.err.println("FAIL: Auto-detected files could not be read.");
            }
        } catch (Exception e) {
            System.err.println("FAIL: Test 6 exception: " + e.getMessage());
        }

        System.out.printf("%nResult: %d / %d tests passed.%n", passed, total);
        if (passed != total) {
            System.exit(1);
        }
    }
}
