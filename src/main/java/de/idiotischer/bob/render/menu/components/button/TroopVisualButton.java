package de.idiotischer.bob.render.menu.components.button;

import com.aspose.psd.internal.bD.B;
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

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g2.setStroke(new BasicStroke(0.3f));

        g2.setColor(Color.DARK_GRAY);
        g2.fillRect(0, 0, getWidth(), getHeight());

        if (stack.getOwner() != null) {
            BufferedImage img = stack.getOwner().getFlagImage();

            g2.drawImage(
                    img,
                    0,
                    0,
                    getWidth(),
                    getHeight(),
                    null
            );
        }

        g2.setColor(stack.getController() == null ? Color.GREEN : stack.getController().countryColor());

        if(stack.getController() != null && stack.getController() == BOB.getInstance().getPlayer().country())
            g2.setColor(Color.GREEN);

        g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);

        if (BOB.getInstance().getMainRenderer().getGamePanel().getTroopButtonGroup().contains(this)) {

            g2.setColor(new Color(255, 255, 255, 126));
            g2.setStroke(new BasicStroke(3f));

            g2.fillRect(0, 0, getWidth(), getHeight());
        }

        g2.setColor(Color.WHITE);
        g2.setFont(getFont().deriveFont(Font.BOLD, 14f));

        String text = String.valueOf(stack.getTroops().size());

        FontMetrics fm = g2.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getAscent();

        g2.drawString(text, (getWidth() - textWidth) / 2 + 15, (getHeight() + textHeight) / 2 - 2);

        g2.dispose();
    }

    public TroopStack getStack() {
        return stack;
    }

}