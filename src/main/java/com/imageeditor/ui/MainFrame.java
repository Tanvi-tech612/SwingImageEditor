package com.imageeditor.ui;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {

    public MainFrame() {

        setTitle("Swing Image Editor");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        setLayout(new BorderLayout());

        CanvasPanel canvasPanel = new CanvasPanel();
        LayersPanel layersPanel = new LayersPanel();

        add(canvasPanel, BorderLayout.CENTER);
        add(layersPanel, BorderLayout.EAST);
    }
}