package de.idiotischer.bob.render.menu.components.button;

import de.idiotischer.bob.BOB;
import de.idiotischer.bob.focus.Focus;
import de.idiotischer.bob.troop.TroopStack;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class FocusButton extends JToggleButton {

    private Point p;
    private Focus focus;

    public FocusButton(Focus focus, Point p) {
        this.focus = focus;
        this.p = p;

        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setFocusable(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color dark = BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().palette().darkColor();

        g2.setColor(dark);

        FontMetrics fm = g2.getFontMetrics();

        int x = 0;
        int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(focus.getName(), x, y);

        g2.dispose();
    }

    public Focus getFocus() {
        return focus;
    }

    public Point getPoint() {
        return p;
    }
}