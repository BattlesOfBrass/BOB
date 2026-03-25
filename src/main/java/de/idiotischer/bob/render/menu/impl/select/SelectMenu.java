package de.idiotischer.bob.render.menu.impl.select;

import de.idiotischer.bob.render.menu.Menu;
import de.idiotischer.bob.render.menu.components.ScrollContainer;

import javax.swing.*;
import java.awt.*;

public class SelectMenu implements Menu {

    protected final int layoutScaleX;
    protected final int layoutScaleY;

    protected final JPanel parent;

    public SelectMenu(JPanel panel, int layoutScaleX, int layoutScaleY) {
        this.parent = panel;

        this.layoutScaleX = layoutScaleX;
        this.layoutScaleY = layoutScaleY;
    }
    @Override
    public void paint(Graphics g) {
        g.setColor(Color.DARK_GRAY);

        Graphics2D g2 = (Graphics2D) g;

        int x = parent.getWidth() / 2 - (layoutScaleX / 2);
        int y = parent.getHeight() / 2 - (layoutScaleY / 2);

        g2.setStroke(new BasicStroke(16));

        drawBack(g2, x, y);
    }

    private void drawBack(Graphics2D g2, int x, int y) {
        g2.setColor(Color.DARK_GRAY.darker());
        g2.drawRoundRect(x, y, layoutScaleX, layoutScaleY + 40,32,32);

        g2.setColor(Color.DARK_GRAY);
        g2.fillRoundRect(x, y, layoutScaleX, layoutScaleY + 40,32,32);
    }

}
