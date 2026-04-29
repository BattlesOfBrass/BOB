package de.idiotischer.bob.render.menu.impl;

import de.idiotischer.bob.render.menu.impl.select.CountrySelectMenu;

import javax.swing.*;
import java.awt.*;

public class MultiplayerMenu extends JPanel {
    private CountrySelectMenu countrySelectMenu;

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(Color.DARK_GRAY);
        g2.fillRoundRect(4, 4, getWidth() - 8, getHeight() - 8, 30, 30);

        g2.setStroke(new BasicStroke(8));
        g2.setColor(Color.DARK_GRAY.darker());
        g2.drawRoundRect(4, 4, getWidth() - 8, getHeight() - 8, 30, 30);

        g2.dispose();
    }
}
