package de.idiotischer.bob.render.menu.components.button;

import de.idiotischer.bob.BOB;
import de.idiotischer.bob.troop.TroopStack;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class TroopVisualButton extends JToggleButton {

    //hope this optimizes it even more
    private static final BasicStroke STROKE_7 = new BasicStroke(7f);
    private static final BasicStroke STROKE_1 = new BasicStroke(1f);
    private static final Color HIGHLIGHT_COLOR = new Color(255, 255, 255, 126);  //well this needs to be themed ig so yeah
    private static final Font FONT_BOLD_14 = new Font("SansSerif", Font.BOLD, 14); //well this needs to be themed ig so yeah

    private final TroopStack stack;

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

        int width = getWidth();
        int height = getHeight();

        Color dark = BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().palette().darkColor();
        g2.setColor(dark);
        g2.fillRect(0, 0, width, height);

        if (stack.getOwner() != null) {
            BufferedImage img = stack.getOwner().getFlagImage(BOB.getInstance().getScenarioSceneLoader().getCurrentScenario());
            if (img != null) g2.drawImage(img, 0, 0, width, height, null);
        }

        var playerCountry = BOB.getInstance().getPlayer().country();
        var controller = stack.getController();
        if (controller == null || controller == playerCountry) g2.setColor(Color.GREEN);
        else g2.setColor(controller.countryColor());


        g2.setStroke(STROKE_7);
        g2.drawRect(0, 0, width - 1, height - 1);

        if (BOB.getInstance().getMainRenderer().getGamePanel().getTroopButtonGroup().contains(this)) {
            g2.setColor(HIGHLIGHT_COLOR);
            g2.fillRect(0, 0, width, height);
        }

        String text = String.valueOf(stack.getTroops().size());
        g2.setFont(FONT_BOLD_14);
        FontMetrics fm = g2.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getAscent();
        int x = (width - textWidth) / 2 + 15;
        int y = (height + textHeight) / 2 - 2;

        g2.setColor(Color.BLACK);
        g2.drawString(text, x - 1, y - 1);
        g2.drawString(text, x - 1, y + 1);
        g2.drawString(text, x + 1, y - 1);
        g2.drawString(text, x + 1, y + 1);

        g2.setColor(Color.WHITE);
        g2.drawString(text, x, y);

        g2.setColor(Color.WHITE);
        g2.setStroke(STROKE_1);
        g2.drawRect(0, 0, width - 1, height - 1);

        g2.dispose();
    }

    public TroopStack getStack() {
        return stack;
    }
}