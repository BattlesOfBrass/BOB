package de.idiotischer.bob.render.menu.components;

import javax.swing.border.AbstractBorder;
import java.awt.*;

public class RoundedBorder extends AbstractBorder {

    private final int radius;
    private final int thickness;
    private final Color color;

    public RoundedBorder(int radius, int thickness, Color color) {
        this.radius = radius;
        this.thickness = thickness;
        this.color = color;
    }

    @Override
    public Insets getBorderInsets(Component c) {
        return new Insets(thickness + 6, thickness + 10,
                thickness + 6, thickness + 10);
    }

    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
        insets.left = insets.right = 10;
        insets.top = insets.bottom = 8;
        return insets;
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(color);
        g2.setStroke(new BasicStroke(thickness));

        int offset = thickness / 2;

        g2.drawRoundRect(
                x + offset,
                y + offset,
                width - thickness,
                height - thickness,
                radius,
                radius
        );

        g2.dispose();
    }
}