package de.idiotischer.bob.render.menu.components;

import javax.swing.*;
import javax.swing.plaf.basic.BasicTextFieldUI;
import java.awt.*;

public class ModernTextField extends BasicTextFieldUI {

    @Override
    protected void installDefaults() {
        super.installDefaults();
        this.getComponent().setOpaque(false);
        this.getComponent().setBorder(new RoundedBorder(12,3,Color.DARK_GRAY.darker()));
        this.getComponent().setForeground(Color.WHITE);
        this.getComponent().setCaretColor(Color.WHITE);
        this.getComponent().setBackground(new Color(0, 0, 0, 0));
        this.getComponent().setFont(new Font("SansSerif", Font.PLAIN, 14));
    }

    @Override
    protected void paintSafely(Graphics g) {
        JComponent c = getComponent();

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setClip(new java.awt.geom.RoundRectangle2D.Float(
                0, 0, c.getWidth(), c.getHeight(), 12, 12));

        super.paintSafely(g2);
        g2.dispose();
    }
}