package de.idiotischer.bob.render.menu.impl;

import de.idiotischer.bob.BOB;
import de.idiotischer.bob.render.menu.Menu;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

public class CountrySelectMenu implements Menu {

    private final JPanel parent;
    private final int layoutScaleX;
    private final int layoutScaleY;

    public CountrySelectMenu(JPanel panel, int layoutScaleX, int layoutScaleY) {
        this.parent = panel;
        this.layoutScaleX = layoutScaleX;
        this.layoutScaleY = layoutScaleY;
    }

    @Override
    public void paint(Graphics g) {
        g.setColor(Color.DARK_GRAY);

        Graphics2D g2 = (Graphics2D) g;
        //center logic so für alles übernehmen
        int x = parent.getWidth() / 2 - (layoutScaleX / 2);
        int y = parent.getHeight() / 2 - (layoutScaleY / 2);

        g2.setStroke(new BasicStroke(16));

        g.setColor(Color.DARK_GRAY.darker());
        g2.drawRoundRect(x, y, layoutScaleX, layoutScaleY,32,32);

        g.setColor(Color.DARK_GRAY);
        g.fillRoundRect(x, y, layoutScaleX, layoutScaleY,32,32);
    }

    @Override
    public void keyPress(KeyEvent e) {
        if(e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            BOB.getInstance().getMapRenderer().getMenuPanel().setInScenarioSelect(false);
        }
    }
}
