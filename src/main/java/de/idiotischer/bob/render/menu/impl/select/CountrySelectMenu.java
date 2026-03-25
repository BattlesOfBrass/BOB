package de.idiotischer.bob.render.menu.impl.select;

import de.idiotischer.bob.BOB;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;

public class CountrySelectMenu extends SelectMenu{

    public CountrySelectMenu(JPanel panel, int layoutScaleX, int layoutScaleY) {
        super(panel, layoutScaleX, layoutScaleY);
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);

        g.setColor(Color.DARK_GRAY);

        Graphics2D g2 = (Graphics2D) g;
        //center logic so für alles übernehmen
        int x = parent.getWidth() / 2 - (layoutScaleX / 2);
        int y = parent.getHeight() / 2 - (layoutScaleY / 2);

        g2.setStroke(new BasicStroke(16));
    }

    @Override
    public void keyPress(KeyEvent e) {
        if(e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            BOB.getInstance().getMainRenderer().getMenuPanel().setInScenarioSelect(false);
        }
    }
}
