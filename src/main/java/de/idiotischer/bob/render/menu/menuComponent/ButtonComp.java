package de.idiotischer.bob.render.menu.menuComponent;

import de.idiotischer.bob.BOB;
import de.idiotischer.bob.render.menu.Component;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

public class ButtonComp implements Component {
    private final String text;
    private final Rectangle bounds;
    private final Color bgColor;
    private final Color textColor;
    private final Consumer<ButtonComp> onClick;
    private final int arcWidth;
    private final int arcHeight;
    private final boolean centered;
    private final int borderWidth;
    private final Color borderColor;
    private final Color borderColorWhenHover;

    private boolean hovered = false;
    private boolean pressed = false;

    public ButtonComp(String text, Color textColor, Color borderColor, int x, int y, int width, int height, int arcWidth, int arcHeight, int borderWidth, Color borderColorWhenHover, Color color, boolean centered, Consumer<ButtonComp> onClick) {
        this.text = text;
        this.bounds = new Rectangle(x, y, width, height);
        this.bgColor = color;
        this.textColor = textColor;
        this.onClick = onClick;
        this.arcWidth = arcWidth;
        this.arcHeight = arcHeight;
        this.centered = centered;
        this.borderWidth = borderWidth;
        this.borderColor = borderColor;
        this.borderColorWhenHover = borderColorWhenHover;
    }

    @Override
    public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        //g2.setColor(displayColor);
        //g2.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, arcWidth, arcHeight);

        JPanel pl = BOB.getInstance().getMapRenderer().getGamePanel();

        int x = centered ? pl.getWidth() / 2 - (bounds.width / 2) : bounds.x;
        int y = centered ? pl.getHeight() / 2 - (bounds.height / 2) : bounds.y;
        x += bounds.x;
        y -= bounds.y;


        Color displayColor = bgColor;
        if (pressed) {
            displayColor = bgColor.darker();
        } else if (hovered) {
            displayColor = bgColor.brighter();
        }

        g2.setColor(displayColor);
        g2.fillRoundRect(x, y, bounds.width, bounds.height, arcWidth, arcHeight);

        g2.setColor(borderColor);

        if (hovered && borderWidth > 0) {
            g2.setColor(borderColorWhenHover);
        }

        g2.setStroke(new BasicStroke(borderWidth));
        g2.drawRoundRect(x, y, bounds.width, bounds.height, arcWidth, arcHeight);

        g.setColor(displayColor);
        g.fillRoundRect(x, y, bounds.width, bounds.height, arcWidth, arcHeight);

        g.setColor(textColor);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));

        FontMetrics fm = g.getFontMetrics();
        int textX = x + (bounds.width - fm.stringWidth(text)) / 2;
        int textY = y + ((bounds.height - fm.getHeight()) / 2) + fm.getAscent();

        g.drawString(text, textX, textY);
    }

    @Override
    public void mouseClick(MouseEvent e, int x, int y) {
        if (getActualBounds().contains(e.getPoint()) && onClick != null) {
            onClick.accept(this);
        }
        pressed = false;
    }

    @Override
    public void mouseRelease(MouseEvent e, int x, int y) {
        pressed = getActualBounds().contains(e.getPoint());
    }

    @Override
    public void mouseMove(MouseEvent e, int x, int y) {
        hovered = getActualBounds().contains(e.getPoint());
    }

    public Rectangle getBounds() {
        return bounds;
    }

    private Rectangle getActualBounds() {
        JPanel pl = BOB.getInstance().getMapRenderer().getGamePanel();
        int x = centered ? pl.getWidth() / 2 - (bounds.width / 2) : bounds.x;
        int y = centered ? pl.getHeight() / 2 - (bounds.height / 2) : bounds.y;
        x += bounds.x;
        y -= bounds.y;
        return new Rectangle(x, y, bounds.width, bounds.height);
    }

    public String getText() {
        return text;
    }
}
