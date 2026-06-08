package de.idiotischer.bob.render.menu.components.button;

import de.idiotischer.bob.troop.TroopStack;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class TroopVisualButton extends JButton {

    private final TroopStack troopStack;
    public double scale;

    public TroopVisualButton(TroopStack troopStack) {
        this.troopStack = troopStack;
    }

    @Override
    public void paint(Graphics g) {
        Point point = troopStack.getTile().getPoints().getFirst();

        //later i'm gonna multiply by scale ro smth, idrk rn
        int width = 65;
        int height = 15;

        int x = point.x - width;
        int y = point.y - height;

        Image image = troopStack.getOwner().getFlagImage().getScaledInstance(32,6, 0);

        g.drawImage(image, x, y, width, height, null);

        g.setColor(Color.DARK_GRAY.darker());
        g.fillRect(x,y,width, height);
        g.setColor(troopStack.getController().countryColor().brighter());
        g.drawRect(x,y,width, height);
    }
}
