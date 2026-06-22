package de.idiotischer.bob.render.menu.components;

import javax.swing.*;
import javax.swing.plaf.basic.BasicSpinnerUI;
import java.awt.*;
import java.awt.event.ActionEvent;

public class ModernSpinnerUI extends BasicSpinnerUI {

    private final Color BG = new Color(60, 60, 60);
    private final Color BORDER = Color.GRAY;
    private final Color TEXT = new Color(230, 230, 230);

    @Override
    protected void installDefaults() {
        super.installDefaults();

        spinner.setOpaque(false);
        spinner.setBorder(new javax.swing.border.AbstractBorder() {
            @Override
            public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(BORDER);
                g2.drawRoundRect(x, y, width - 1, height - 1, 12, 12);

                g2.dispose();
            }

            @Override
            public Insets getBorderInsets(Component c) {
                return new Insets(6, 8, 6, 8);
            }

            @Override
            public Insets getBorderInsets(Component c, Insets insets) {
                insets.set(6, 8, 6, 8);
                return insets;
            }
        });
    }

    @Override
    protected void installListeners() {
        super.installListeners();

        SwingUtilities.invokeLater(() -> {
            JComponent editor = spinner.getEditor();

            if (editor instanceof JSpinner.DefaultEditor def) {
                JTextField tf = def.getTextField();

                tf.setBackground(BG);
                tf.setForeground(TEXT);
                tf.setCaretColor(TEXT);
                tf.setOpaque(true);
            }
        });
    }

    @Override
    protected Component createNextButton() {
        return createButton("▲", "up");
    }

    @Override
    protected Component createPreviousButton() {
        return createButton("▼", "down");
    }

    private JButton createButton(String text, String direction) {
        JButton btn = new JButton(text);

        btn.setFocusable(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);

        btn.setFont(new Font("Arial", Font.BOLD, 10));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btn.setForeground(new Color(180, 180, 180));

        btn.addActionListener((ActionEvent e) -> {
            if ("up".equals(direction) && spinner.getNextValue() != null) {
                spinner.setValue(spinner.getNextValue());
            } else if ("down".equals(direction) && spinner.getPreviousValue() != null) {
                spinner.setValue(spinner.getPreviousValue());
            }
        });

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setForeground(Color.WHITE);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setForeground(new Color(180, 180, 180));
            }
        });

        return btn;
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        super.paint(g, c);

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = c.getWidth();
        int h = c.getHeight();

        g2.setColor(BG);
        g2.fillRoundRect(0, 0, w - 1, h - 1, 12, 12);

        g2.setColor(BORDER);
        g2.drawRoundRect(0, 0, w - 1, h - 1, 12, 12);

        g2.dispose();
    }
}