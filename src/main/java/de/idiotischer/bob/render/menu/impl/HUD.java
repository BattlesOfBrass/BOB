package de.idiotischer.bob.render.menu.impl;

import de.idiotischer.bob.BOB;
import de.idiotischer.bob.render.MainRenderer;
import de.idiotischer.bob.render.menu.Menu;

import java.awt.*;
import java.awt.event.MouseEvent;

public class HUD implements Menu {

    private final MainRenderer renderer;

    public HUD(MainRenderer renderer) {
        this.renderer = renderer;
    }

    @Override
    public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.setColor(Color.GREEN);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        g.drawString("Current country: " + BOB.getInstance().getPlayer().country().countryName(),15,15);
    }

    @Override
    public void mouseClick(MouseEvent e, int x, int y) {

    }
}
