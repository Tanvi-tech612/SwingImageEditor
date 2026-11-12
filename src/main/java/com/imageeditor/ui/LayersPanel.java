package com.imageeditor.ui;

import javax.swing.*;
import java.awt.*;

public class LayersPanel extends JPanel {

    public LayersPanel() {

        setPreferredSize(new Dimension(220, 0));
        setLayout(new BorderLayout());

        JLabel title = new JLabel("Layers");
        add(title, BorderLayout.NORTH);
    }
}