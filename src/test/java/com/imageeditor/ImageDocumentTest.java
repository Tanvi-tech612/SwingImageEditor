package com.imageeditor;

import com.imageeditor.model.ImageDocument;
import com.imageeditor.model.Layer;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

public class ImageDocumentTest {

    @Test
    public void testEmptyDocument() {
        ImageDocument doc = new ImageDocument();
        assertEquals(0, doc.getLayerCount());
        assertNull(doc.getSelectedLayer());
        assertFalse(doc.hasImage());
        assertNull(doc.getCompositeImage());
    }

    @Test
    public void testAddLayerSelectsNewLayer() {
        ImageDocument doc = new ImageDocument();
        BufferedImage img1 = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB);
        Layer l1 = new Layer("Layer 1", img1);
        doc.addLayer(l1);

        assertEquals(1, doc.getLayerCount());
        assertSame(l1, doc.getSelectedLayer());
        assertEquals(0, doc.getSelectedIndex());
        assertTrue(doc.hasImage());

        BufferedImage img2 = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB);
        Layer l2 = new Layer("Layer 2", img2);
        doc.addLayer(l2);

        assertEquals(2, doc.getLayerCount());
        assertSame(l2, doc.getSelectedLayer());
        assertEquals(1, doc.getSelectedIndex());
    }

    @Test
    public void testPreventDeletingFinalRemainingLayer() {
        ImageDocument doc = new ImageDocument();
        BufferedImage img = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB);
        Layer l1 = new Layer("Layer 1", img);
        doc.addLayer(l1);

        // Attempt to remove only layer
        boolean removed = doc.removeLayer(l1);
        assertFalse(removed, "Should not allow removing the final remaining layer");
        assertEquals(1, doc.getLayerCount());
        assertSame(l1, doc.getSelectedLayer());

        // Now add a second layer
        Layer l2 = new Layer("Layer 2", img);
        doc.addLayer(l2);
        assertEquals(2, doc.getLayerCount());

        // Deleting when 2 layers exist should succeed
        boolean removedL2 = doc.removeLayer(l2);
        assertTrue(removedL2);
        assertEquals(1, doc.getLayerCount());
        assertSame(l1, doc.getSelectedLayer());
    }

    @Test
    public void testReorderingLayers() {
        ImageDocument doc = new ImageDocument();
        Layer l1 = new Layer("Bottom", new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB));
        Layer l2 = new Layer("Middle", new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB));
        Layer l3 = new Layer("Top", new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB));

        doc.addLayer(l1); // index 0
        doc.addLayer(l2); // index 1
        doc.addLayer(l3); // index 2

        assertFalse(doc.canMoveDown(l1));
        assertTrue(doc.canMoveUp(l1));
        assertTrue(doc.canMoveDown(l2));
        assertTrue(doc.canMoveUp(l2));
        assertTrue(doc.canMoveDown(l3));
        assertFalse(doc.canMoveUp(l3));

        // Move l1 up
        doc.moveLayerUp(l1);
        assertEquals(0, doc.indexOf(l2));
        assertEquals(1, doc.indexOf(l1));
        assertEquals(2, doc.indexOf(l3));

        // Move l3 down
        doc.moveLayerDown(l3);
        assertEquals(0, doc.indexOf(l2));
        assertEquals(1, doc.indexOf(l3));
        assertEquals(2, doc.indexOf(l1));
    }

    @Test
    public void testCompositeRenderingOrderAndVisibility() {
        ImageDocument doc = new ImageDocument();

        // Layer 1 (bottom): solid red 10x10
        BufferedImage redImg = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g1 = redImg.createGraphics();
        g1.setColor(Color.RED);
        g1.fillRect(0, 0, 10, 10);
        g1.dispose();
        Layer l1 = new Layer("Red Bottom", redImg);
        doc.addLayer(l1);

        // Layer 2 (top): solid blue 10x10
        BufferedImage blueImg = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = blueImg.createGraphics();
        g2.setColor(Color.BLUE);
        g2.fillRect(0, 0, 10, 10);
        g2.dispose();
        Layer l2 = new Layer("Blue Top", blueImg);
        doc.addLayer(l2);

        // When both are visible, blue (top) covers red (bottom)
        BufferedImage composite = doc.getCompositeImage();
        assertNotNull(composite);
        Color pixel = new Color(composite.getRGB(5, 5), true);
        assertEquals(Color.BLUE.getRGB(), pixel.getRGB());

        // When top layer is hidden, red (bottom) shows through
        l2.setVisible(false);
        BufferedImage compositeHidden = doc.getCompositeImage();
        Color pixelHidden = new Color(compositeHidden.getRGB(5, 5), true);
        assertEquals(Color.RED.getRGB(), pixelHidden.getRGB());

        // When bottom layer is moved to top, red is on top
        l2.setVisible(true);
        doc.moveLayerUp(l1); // l1 becomes top (index 1)
        BufferedImage compositeReordered = doc.getCompositeImage();
        Color pixelReordered = new Color(compositeReordered.getRGB(5, 5), true);
        assertEquals(Color.RED.getRGB(), pixelReordered.getRGB());
    }

    @Test
    public void testCompositeOpacity() {
        ImageDocument doc = new ImageDocument();

        // Layer 1: solid black
        BufferedImage blackImg = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g1 = blackImg.createGraphics();
        g1.setColor(Color.BLACK);
        g1.fillRect(0, 0, 10, 10);
        g1.dispose();
        doc.addLayer(new Layer("Black", blackImg));

        // Layer 2: solid white with 50% opacity
        BufferedImage whiteImg = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = whiteImg.createGraphics();
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, 10, 10);
        g2.dispose();
        Layer whiteLayer = new Layer("White", whiteImg);
        whiteLayer.setOpacity(0.5f);
        doc.addLayer(whiteLayer);

        BufferedImage comp = doc.getCompositeImage();
        int rgb = comp.getRGB(5, 5);
        int r = (rgb >> 16) & 255;
        int g = (rgb >> 8) & 255;
        int b = rgb & 255;

        // Blending white (255) at 50% over black (0) yields ~127-128
        assertTrue(r > 120 && r < 135, "Expected intermediate gray value for red, got " + r);
        assertTrue(g > 120 && g < 135, "Expected intermediate gray value for green, got " + g);
        assertTrue(b > 120 && b < 135, "Expected intermediate gray value for blue, got " + b);
    }
}
