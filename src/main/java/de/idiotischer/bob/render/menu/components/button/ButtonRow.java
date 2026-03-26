package de.idiotischer.bob.render.menu.components.button;

import de.idiotischer.bob.BOB;
import de.idiotischer.bob.render.menu.Component;
import de.idiotischer.bob.render.menu.components.button.ButtonComp;
import de.idiotischer.bob.render.menu.components.button.IButtonComp;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.List;

//add scrolling xD
public class ButtonRow implements Component {
    private final JPanel panel;
    private final Color color;

    private final boolean centered;

    private Rectangle bounds;
    private List<IButtonComp> children = new ArrayList<>();

    private final int padding = 10;
    private int spacing = 0;

    public ButtonRow(JPanel panel, Color color, boolean centered) {
        bounds = new Rectangle(0, 0, 0, 0);
        this.panel = panel;
        this.color = color;
        this.centered = centered;
    }

    public void setChildren(List<IButtonComp> children) {
        this.children = children;
        layoutChildren();
    }

    //IMPORTANT BC THIS MODIFIES THE RECTANGLE
    private void layoutChildren() {
        int xOffset = padding;

        for (Component child : children) {
            if (child instanceof IButtonComp btn) {
                Rectangle b = btn.getBounds();
                b.x += xOffset;
                xOffset += b.width + spacing;
            }
        }
    }

    @Override
    public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;

        JPanel pl = panel != null ? panel : BOB.getInstance().getMainRenderer().getGamePanel();

        Rectangle bounds = getActualBounds();

        int x = centered ? (pl.getWidth() - bounds.width) / 2 : bounds.x;
        int y = centered ? (pl.getHeight() - bounds.height) / 2 : bounds.y;

        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(3));
        g2.drawRoundRect(x, y, bounds.width, bounds.height, 24, 24);

        for (IButtonComp child : children) {
            //child.setDebug(true);
            child.paint(g2);
        }
    }

    @Override
    public void mouseScroll(MouseWheelEvent e, int x, int y) {
    }

    @Override
    public void mouseClick(MouseEvent e, int x, int y) {
        getChildren().forEach(component -> component.mouseClick(e, x, y));
    }

    @Override
    public void mouseRelease(MouseEvent e, int x, int y) {
        getChildren().forEach(component -> component.mouseRelease(e, x, y));
    }

    @Override
    public void mouseMove(MouseEvent e, int x, int y) {
        getChildren().forEach(component -> component.mouseMove(e, x, y));
    }

    public void setBounds(Rectangle rectangle) {
        this.bounds = rectangle;
    }

    public Rectangle getActualBounds() {
        JPanel pl = panel != null ? panel : BOB.getInstance().getMainRenderer().getGamePanel();
        int x = centered ? pl.getWidth() / 2 - (bounds.width / 2) : bounds.x;
        int y = centered ? pl.getHeight() / 2 - (bounds.height / 2) : bounds.y;
        x += bounds.x;
        y -= bounds.y;
        return new Rectangle(x, y, bounds.width, bounds.height);
    }

    public Rectangle getBounds() {
        return bounds;
    }

    public boolean isCentered() {
        return centered;
    }

    public List<IButtonComp> getChildren() {
        return children;
    }

    public JPanel getPanel() {
        return panel;
    }

    public void setSpacing(int spacing) {
        this.spacing = spacing;
    }
}