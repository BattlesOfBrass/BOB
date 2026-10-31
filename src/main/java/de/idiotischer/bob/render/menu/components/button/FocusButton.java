package de.idiotischer.bob.render.menu.components.button;

import de.idiotischer.bob.BOB;
import de.idiotischer.bob.focus.Focus;
import de.idiotischer.bob.troop.TroopStack;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Objects;

public class FocusButton extends JToggleButton {

    private final String treeId;
    private final Point p;
    private final Focus focus;
    private final String country;

    public FocusButton(String id, String country, Focus focus, Point point) {
        this.focus = focus;
        this.p = point;
        this.treeId = id;
        this.country = country;

        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setFocusable(false);

        addActionListener(e -> {
            //open small ui that gives description etc than have a start button like in hoi4 or a cancel button depending on if the focus is active or not
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color dark = BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().palette().darkColor();

        g2.setColor(Color.WHITE);

        FontMetrics fm = g2.getFontMetrics();

        Color background = dark;

        if (getModel().isSelected()) background = new Color(180, 140, 50);
        else if (getModel().isRollover()) background = dark.brighter();

        g2.setColor(background);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);

        Focus active = BOB.getInstance().getFocusManager().getActiveFocus(country);
        boolean isActive = active != null && Objects.equals(active.getId(), focus.getId());

        if (isActive) {
            g2.setColor(new Color(255, 190, 50));
            g2.setStroke(new BasicStroke(3f));

            g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 6, 6);
        }

        int x = 8;
        int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(focus.getName(), x, y);

        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(60, 20);
    }

    public String getTreeId() {
        return treeId;
    }

    public Focus getFocus() {
        return focus;
    }

    public Point getPoint() {
        return p;
    }
}