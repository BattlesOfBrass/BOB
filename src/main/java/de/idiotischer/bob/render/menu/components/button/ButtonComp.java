package de.idiotischer.bob.render.menu.components.button;

import de.idiotischer.bob.BOB;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

public class ButtonComp implements IButtonComp {
    private final String text;
    private final String id;
    private final Color selectedColor;
    private final ButtonGroup group;

    private Rectangle bounds;
    private final Color bgColor;
    private final Color textColor;
    private final Consumer<ButtonComp> onClick;
    private final int arcWidth;
    private final int arcHeight;
    private final boolean centered;
    private final int borderWidth;
    private final Color borderColor;
    private final Color borderColorWhenHover;
    private final boolean unselectWhenUnhover;

    private boolean hovered = false;
    private boolean pressed = false;
    private boolean selected = false;

    private long lastClickTime = 0;
    private long CLICK_THRESHOLD = 200;
    private JPanel panel;
    private boolean debug = false;

    public ButtonComp(String text, Color textColor, Color borderColor, boolean unselectWhenUnhover, int x, int y, int width, int height, int arcWidth, int arcHeight, int borderWidth, Color borderColorWhenHover, Color color, boolean centered, Consumer<ButtonComp> onClick) {
        this("", text,textColor, Color.WHITE, borderColor, unselectWhenUnhover, x, y, width, height, arcWidth, arcHeight, borderWidth, borderColorWhenHover, color, centered, null, onClick);
    }

    public ButtonComp(String id, String text, Color textColor, Color borderColor, boolean unselectWhenUnhover, int x, int y, int width, int height, int arcWidth, int arcHeight, int borderWidth, Color borderColorWhenHover, Color color, boolean centered, ButtonGroup group, Consumer<ButtonComp> onClick) {
        this(id, text,textColor, Color.WHITE, borderColor, unselectWhenUnhover, x, y, width, height, arcWidth, arcHeight, borderWidth, borderColorWhenHover, color, centered,group, onClick);
    }

    public ButtonComp(String text, Color textColor, Color borderColor, boolean unselectWhenUnhover, int x, int y, int width, int height, int arcWidth, int arcHeight, int borderWidth, Color borderColorWhenHover, Color color, boolean centered, ButtonGroup group, Consumer<ButtonComp> onClick) {
        this("", text,textColor, Color.WHITE, borderColor, unselectWhenUnhover, x, y, width, height, arcWidth, arcHeight, borderWidth, borderColorWhenHover, color, centered,group, onClick);
    }

    public ButtonComp(String id, String text, Color textColor, Color selectedColor, Color borderColor, boolean unselectWhenUnhover, int x, int y, int width, int height, int arcWidth, int arcHeight, int borderWidth, Color borderColorWhenHover, Color color, boolean centered, ButtonGroup group, Consumer<ButtonComp> onClick) {
        this.id = id;
        this.text = text;
        this.bounds = new Rectangle(x, y, width, height);
        this.bgColor = color;
        this.textColor = textColor;
        this.selectedColor = selectedColor;
        this.onClick = onClick;
        this.arcWidth = arcWidth;
        this.arcHeight = arcHeight;
        this.centered = centered;
        this.borderWidth = borderWidth;
        this.borderColor = borderColor;
        this.borderColorWhenHover = borderColorWhenHover;
        this.unselectWhenUnhover = unselectWhenUnhover;
        this.group = group;

        if (this.group != null) {
            this.group.add(this);
        }
    }

    public void setBounds(Rectangle bounds) {
        this.bounds = bounds;
    }

    public void setPanel(JPanel pl) {
        this.panel = pl;
    }

    @Override
    public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        //g2.setColor(displayColor);
        //g2.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, arcWidth, arcHeight);

        JPanel pl = panel != null ? panel : BOB.getInstance().getMainRenderer().getGamePanel();

        int x = centered ? pl.getWidth() / 2 - (bounds.width / 2) : bounds.x;
        int y = centered ? pl.getHeight() / 2 - (bounds.height / 2) : bounds.y;
        x += bounds.x;
        y -= bounds.y;

        Color displayColor = bgColor;
        if (pressed && unselectWhenUnhover) {
            displayColor = bgColor.darker();
        } else if (hovered) {
            displayColor = bgColor.brighter();
        }

        //if (debug) {
        //    Rectangle r = getActualBounds();
        //    g2.setColor(Color.RED);
        //    g2.setStroke(new BasicStroke(2));
        //    g2.drawRect(r.x, r.y, r.width, r.height);
        //    g2.setColor(new Color(255, 0, 0, 50));
        //    g2.fillRect(r.x, r.y, r.width, r.height);
        //}

        g2.setColor(borderColor);

        if (hovered && borderWidth > 0) {
            g2.setColor(borderColorWhenHover);
        }

        g2.setStroke(new BasicStroke(borderWidth));
        g2.drawRoundRect(x, y, bounds.width, bounds.height, arcWidth, arcHeight);

        g2.setStroke(new BasicStroke(3));
        g2.setColor(Color.WHITE);
        if(selected) g2.drawRoundRect(x, y, bounds.width, bounds.height, arcWidth, arcHeight);;

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
            long currentTime = System.currentTimeMillis();

            if (currentTime - lastClickTime < CLICK_THRESHOLD) {
                return;
            }

            lastClickTime = currentTime;

            if (group != null) {
                group.select(this);
            }

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

    public Rectangle getActualBounds() {
        JPanel pl = panel != null ? panel : BOB.getInstance().getMainRenderer().getGamePanel();
        int x = centered ? pl.getWidth() / 2 - (bounds.width / 2) : bounds.x;
        int y = centered ? pl.getHeight() / 2 - (bounds.height / 2) : bounds.y;
        x += bounds.x;
        y -= bounds.y;
        return new Rectangle(x, y, bounds.width, bounds.height);
    }

    @Override
    public boolean isSelected() {
        return selected;
    }

    @Override
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public String getText() {
        return text;
    }

    public String getId() {
        return id;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }
}
