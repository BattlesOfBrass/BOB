package de.idiotischer.bob.render.menu.components;

import de.idiotischer.bob.BOB;
import de.idiotischer.bob.render.menu.Component;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.List;

public class ScrollContainer implements Component {
    private final JPanel panel;
    private final Color color;

    private final boolean centered;

    private Rectangle bounds;
    private List<Component> children = new ArrayList<>();

    private int scrollOffset = 0;
    private int contentHeight = 0;

    private int padding = 10;
    private int spacing = 10;

    private int scrollbarWidth = 8;

    public ScrollContainer(JPanel panel, Color color, boolean centered) {
        bounds = new Rectangle(0, 0, 0, 0);
        this.panel = panel;
        this.color = color;
        this.centered = centered;
    }

    public void setChildren(List<Component> children) {
        this.children = children;
        layoutChildren();
    }

    private void layoutChildren() {
        int yOffset = padding;

        for (Component child : children) {
            if (child instanceof ButtonComp btn) {
                Rectangle b = btn.getBounds();
                b.y = yOffset;
                yOffset += b.height + spacing;
            }
        }

        contentHeight = yOffset;
    }

    @Override
    public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();

        JPanel pl = panel != null ? panel : BOB.getInstance().getMapRenderer().getGamePanel();

        int x = centered ? pl.getWidth() / 2 - (bounds.width / 2) : bounds.x;
        int y = centered ? pl.getHeight() / 2 - (bounds.height / 2) : bounds.y;
        x += bounds.x;
        y -= bounds.y;

        //TODO: fix clip clippign everytrhing in and out of the box away
        //g2.setClip(bounds); shortly disabled
        g2.translate(x, x - scrollOffset);

        for (Component child : children) {
            child.paint(g2);
        }

        g2.translate(-x, scrollOffset);

        if (contentHeight > bounds.height) {
            float visibleRatio = (float) bounds.height / contentHeight;
            int scrollbarHeight = (int) (bounds.height * visibleRatio);
            int scrollbarY = (int) (scrollOffset * visibleRatio);

            g2.setColor(new Color(100, 100, 100, 180));
            g2.fillRect(x + bounds.width - scrollbarWidth, y + scrollbarY, scrollbarWidth, scrollbarHeight);
        }

        g2.dispose();
    }

    @Override
    public void mouseScroll(MouseWheelEvent e, int x, int y) {
        if (!bounds.contains(x, y)) return;

        int scrollAmount = e.getWheelRotation() * 20;
        scrollOffset += scrollAmount;

        int maxScroll = Math.max(0, contentHeight - bounds.height + padding);
        if (scrollOffset < 0) scrollOffset = 0;
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;

        for (Component child : children) {
            child.mouseScroll(e, x - bounds.x, y - bounds.y + scrollOffset);
        }
    }

    public void setBounds(Rectangle rectangle) {
        this.bounds = rectangle;
    }

    private Rectangle getActualBounds() {
        JPanel pl = panel != null ? panel : BOB.getInstance().getMapRenderer().getGamePanel();
        int x = centered ? pl.getWidth() / 2 - (bounds.width / 2) : bounds.x;
        int y = centered ? pl.getHeight() / 2 - (bounds.height / 2) : bounds.y;
        x += bounds.x;
        y -= bounds.y;
        return new Rectangle(x, y, bounds.width, bounds.height);
    }

    public Rectangle getBounds() {
        return bounds;
    }
}