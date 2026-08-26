package de.idiotischer.bob.render.menu.impl.select;

import javax.swing.*;
import java.awt.*;

// The Overlay on RenderPanel that is shown during conferences
public class PeaceMenuOverlay extends JPanel {
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;

        g2.setColor(Color.DARK_GRAY);
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.setStroke(new BasicStroke(12));
        g2.setColor(Color.DARK_GRAY.darker());
        g2.drawRect(0, 0, getWidth(), getHeight());
    }
}
