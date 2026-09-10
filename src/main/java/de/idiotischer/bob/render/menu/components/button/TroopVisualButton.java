package de.idiotischer.bob.render.menu.components.button;

import de.idiotischer.bob.BOB;
import de.idiotischer.bob.tile.Tile;
import de.idiotischer.bob.troop.TroopStack;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

//TODO: fix color now completely wrong for some stupid reason
public class TroopVisualButton extends JToggleButton {

    private TroopStack stack;

    public TroopVisualButton(TroopStack stack) {
        this.stack = stack;

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

        g2.setColor(Color.DARK_GRAY);
        g2.fillRect(0, 0, getWidth(), getHeight());

        if (stack.getOwner() != null) {
            BufferedImage img = stack.getOwner().getFlagImage(BOB.getInstance().getScenarioSceneLoader().getCurrentScenario());
            g2.drawImage(img, 0, 0, getWidth(), getHeight(), null);
        }

        g2.setColor(stack.getController() == null ? Color.GREEN : stack.getController().countryColor());
        if (stack.getController() != null &&
                stack.getController() == BOB.getInstance().getPlayer().country()) {
            g2.setColor(Color.GREEN);
        }
        g2.setStroke(new BasicStroke(7f));
        g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);

        if (BOB.getInstance().getMainRenderer().getGamePanel().getTroopButtonGroup().contains(this)) {
            g2.setColor(new Color(255, 255, 255, 126));
            g2.fillRect(0, 0, getWidth(), getHeight());
        }

        String text = String.valueOf(stack.getTroops().size());
        Font font = getFont().deriveFont(Font.BOLD, 14f);
        g2.setFont(font);

        FontMetrics fm = g2.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getAscent();

        int x = (getWidth() - textWidth) / 2 + 15;
        int y = (getHeight() + textHeight) / 2 - 2;

        g2.setColor(Color.BLACK);
        g2.drawString(text, x - 1, y - 1);
        g2.drawString(text, x - 1, y + 1);
        g2.drawString(text, x + 1, y - 1);
        g2.drawString(text, x + 1, y + 1);

        g2.setColor(Color.WHITE);
        g2.drawString(text, x, y);

        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(1f));

        g2.drawRect(0, 0, getWidth() -1 , getHeight() -1);

        g2.dispose();
    }

    public TroopStack getStack() {
        return stack;
    }

}