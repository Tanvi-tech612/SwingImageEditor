package com.imageeditor;

import com.imageeditor.model.Layer;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

public class LayerTest {

    @Test
    public void testLayerInitialization() {
        BufferedImage img = new BufferedImage(100, 50, BufferedImage.TYPE_INT_ARGB);
        Layer layer = new Layer("Test Layer", img);

        assertNotNull(layer.getId());
        assertEquals("Test Layer", layer.getName());
        assertSame(img, layer.getImage());
        assertTrue(layer.isVisible());
        assertEquals(1.0f, layer.getOpacity(), 0.0001f);
        assertEquals(100, layer.getWidth());
        assertEquals(50, layer.getHeight());
    }

    @Test
    public void testUniqueId() {
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        Layer l1 = new Layer("L1", img);
        Layer l2 = new Layer("L2", img);

        assertNotNull(l1.getId());
        assertNotNull(l2.getId());
        assertNotEquals(l1.getId(), l2.getId());
    }

    @Test
    public void testOpacityClamping() {
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        Layer layer = new Layer("L", img);

        layer.setOpacity(0.5f);
        assertEquals(0.5f, layer.getOpacity(), 0.0001f);

        layer.setOpacity(-0.5f);
        assertEquals(0.0f, layer.getOpacity(), 0.0001f);

        layer.setOpacity(1.5f);
        assertEquals(1.0f, layer.getOpacity(), 0.0001f);
    }

    @Test
    public void testVisibilityToggle() {
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        Layer layer = new Layer("L", img);

        assertTrue(layer.isVisible());
        layer.setVisible(false);
        assertFalse(layer.isVisible());
        layer.setVisible(true);
        assertTrue(layer.isVisible());
    }

    @Test
    public void testNullImageDimensions() {
        Layer layer = new Layer("Null Image", null);
        assertEquals(0, layer.getWidth());
        assertEquals(0, layer.getHeight());
        assertNull(layer.getImage());
    }
}
