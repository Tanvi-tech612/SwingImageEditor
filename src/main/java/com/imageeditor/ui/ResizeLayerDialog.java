package com.imageeditor.ui;

import com.imageeditor.operations.ResizeOperation;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.KeyEvent;

public class ResizeLayerDialog extends JDialog {

    private final int originalWidth;
    private final int originalHeight;
    private final double originalAspectRatio;

    private final JTextField widthField;
    private final JTextField heightField;
    private final JCheckBox maintainAspectRatioCheckBox;
    private final JButton resizeButton;
    private final JButton cancelButton;

    private boolean updatingFields = false;
    private boolean confirmed = false;
    private int selectedWidth;
    private int selectedHeight;

    public ResizeLayerDialog(Frame owner, String layerName, int originalWidth, int originalHeight) {
        super(owner, "Resize Layer", true);

        if (originalWidth <= 0 || originalHeight <= 0) {
            throw new IllegalArgumentException("Original layer dimensions must be positive.");
        }

        this.originalWidth = originalWidth;
        this.originalHeight = originalHeight;
        this.originalAspectRatio = (double) originalWidth / (double) originalHeight;
        this.selectedWidth = originalWidth;
        this.selectedHeight = originalHeight;

        setResizable(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setBorder(new EmptyBorder(16, 18, 14, 18));

        // Header info displaying selected layer and current dimensions
        String displayName = (layerName != null && !layerName.isBlank()) ? layerName : "Selected Layer";
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel("Resize Layer: " + displayName);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 13));

        JLabel currentDimLabel = new JLabel("Current dimensions: " + originalWidth + " × " + originalHeight + " px");
        currentDimLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        currentDimLabel.setForeground(new Color(100, 105, 115));

        headerPanel.add(titleLabel);
        headerPanel.add(Box.createVerticalStrut(4));
        headerPanel.add(currentDimLabel);
        content.add(headerPanel, BorderLayout.NORTH);

        // Input form
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 4, 6, 4);
        gbc.anchor = GridBagConstraints.WEST;

        JLabel widthLabel = new JLabel("Width (px):");
        widthLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        widthField = new JTextField(String.valueOf(originalWidth), 12);
        widthField.setFont(new Font("SansSerif", Font.PLAIN, 12));

        JLabel heightLabel = new JLabel("Height (px):");
        heightLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        heightField = new JTextField(String.valueOf(originalHeight), 12);
        heightField.setFont(new Font("SansSerif", Font.PLAIN, 12));

        maintainAspectRatioCheckBox = new JCheckBox("Maintain Aspect Ratio", true);
        maintainAspectRatioCheckBox.setFont(new Font("SansSerif", Font.PLAIN, 12));
        maintainAspectRatioCheckBox.setFocusPainted(false);

        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(widthLabel, gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        formPanel.add(widthField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        formPanel.add(heightLabel, gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        formPanel.add(heightField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(10, 2, 2, 4);
        formPanel.add(maintainAspectRatioCheckBox, gbc);

        content.add(formPanel, BorderLayout.CENTER);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        cancelButton = new JButton("Cancel");
        resizeButton = new JButton("Resize");

        cancelButton.setFont(new Font("SansSerif", Font.PLAIN, 12));
        resizeButton.setFont(new Font("SansSerif", Font.PLAIN, 12));
        cancelButton.setFocusPainted(false);
        resizeButton.setFocusPainted(false);

        buttonPanel.add(cancelButton);
        buttonPanel.add(resizeButton);
        content.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(content);
        getRootPane().setDefaultButton(resizeButton);

        // Bind Escape key to Cancel
        getRootPane().registerKeyboardAction(
                e -> onCancel(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        setupListeners();
        pack();
        setMinimumSize(new Dimension(320, getPreferredSize().height));
        setLocationRelativeTo(owner);
    }

    private void setupListeners() {
        widthField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                onWidthChanged();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                onWidthChanged();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                onWidthChanged();
            }
        });

        heightField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                onHeightChanged();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                onHeightChanged();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                onHeightChanged();
            }
        });

        maintainAspectRatioCheckBox.addActionListener(e -> {
            if (maintainAspectRatioCheckBox.isSelected()) {
                onWidthChanged();
            }
        });

        resizeButton.addActionListener(e -> onResize());
        cancelButton.addActionListener(e -> onCancel());
    }

    private void onWidthChanged() {
        if (updatingFields || !maintainAspectRatioCheckBox.isSelected()) {
            return;
        }
        String text = widthField.getText().trim();
        if (text.isEmpty()) {
            return;
        }
        try {
            long parsedWidth = Long.parseLong(text);
            if (parsedWidth > 0 && parsedWidth <= ResizeOperation.MAX_DIMENSION) {
                int proportionalHeight = calculateProportionalHeight((int) parsedWidth, originalWidth, originalHeight);
                updatingFields = true;
                try {
                    heightField.setText(String.valueOf(proportionalHeight));
                } finally {
                    updatingFields = false;
                }
            }
        } catch (NumberFormatException ignored) {
            // Wait until the user finishes typing a valid integer
        }
    }

    private void onHeightChanged() {
        if (updatingFields || !maintainAspectRatioCheckBox.isSelected()) {
            return;
        }
        String text = heightField.getText().trim();
        if (text.isEmpty()) {
            return;
        }
        try {
            long parsedHeight = Long.parseLong(text);
            if (parsedHeight > 0 && parsedHeight <= ResizeOperation.MAX_DIMENSION) {
                int proportionalWidth = calculateProportionalWidth((int) parsedHeight, originalWidth, originalHeight);
                updatingFields = true;
                try {
                    widthField.setText(String.valueOf(proportionalWidth));
                } finally {
                    updatingFields = false;
                }
            }
        } catch (NumberFormatException ignored) {
            // Wait until the user finishes typing a valid integer
        }
    }

    public static int calculateProportionalHeight(int newWidth, int originalWidth, int originalHeight) {
        double aspectRatio = (double) originalWidth / (double) originalHeight;
        return Math.max(1, (int) Math.round(newWidth / aspectRatio));
    }

    public static int calculateProportionalWidth(int newHeight, int originalWidth, int originalHeight) {
        double aspectRatio = (double) originalWidth / (double) originalHeight;
        return Math.max(1, (int) Math.round(newHeight * aspectRatio));
    }

    public static Dimension parseAndValidate(String widthText, String heightText) {
        String trimmedW = widthText != null ? widthText.trim() : "";
        String trimmedH = heightText != null ? heightText.trim() : "";

        if (trimmedW.isEmpty() || trimmedH.isEmpty()) {
            throw new IllegalArgumentException("Width and height cannot be empty.");
        }

        long parsedWidth;
        long parsedHeight;
        try {
            parsedWidth = Long.parseLong(trimmedW);
            parsedHeight = Long.parseLong(trimmedH);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Please enter valid numeric whole numbers for width and height.");
        }

        if (parsedWidth <= 0 || parsedHeight <= 0) {
            throw new IllegalArgumentException("Width and height must be positive integers greater than zero.");
        }

        if (parsedWidth > ResizeOperation.MAX_DIMENSION || parsedHeight > ResizeOperation.MAX_DIMENSION) {
            throw new IllegalArgumentException(
                    "Width and height cannot exceed " + ResizeOperation.MAX_DIMENSION + " px."
            );
        }

        int width = (int) parsedWidth;
        int height = (int) parsedHeight;
        ResizeOperation.validateDimensions(width, height);
        return new Dimension(width, height);
    }

    private void onResize() {
        try {
            Dimension validated = parseAndValidate(widthField.getText(), heightField.getText());
            selectedWidth = validated.width;
            selectedHeight = validated.height;
            confirmed = true;
            dispose();
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    ex.getMessage(),
                    "Invalid Dimensions",
                    JOptionPane.WARNING_MESSAGE
            );
        }
    }

    private void onCancel() {
        confirmed = false;
        dispose();
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public int getSelectedWidth() {
        return selectedWidth;
    }

    public int getSelectedHeight() {
        return selectedHeight;
    }

    public double getOriginalAspectRatio() {
        return originalAspectRatio;
    }

    public JTextField getWidthField() {
        return widthField;
    }

    public JTextField getHeightField() {
        return heightField;
    }

    public JCheckBox getMaintainAspectRatioCheckBox() {
        return maintainAspectRatioCheckBox;
    }

    public JButton getResizeButton() {
        return resizeButton;
    }

    public JButton getCancelButton() {
        return cancelButton;
    }
}

