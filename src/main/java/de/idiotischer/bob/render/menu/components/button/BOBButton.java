package de.idiotischer.bob.render.menu.components.button;

import javax.swing.*;
import java.awt.*;

//TODO: check if this breaks theme changing, if so make a theme update event or smth
public class BOBButton extends JButton {

    private final Color bgColor;
    private final Color textColor;
    private final Color borderColor;
    private final Color borderColorHover;

    private final Color hoverBgColor;
    private final Color pressedBgColor;

    private final int arcWidth;
    private final int arcHeight;
    private final int borderWidth;
    private final String id;
    private boolean toggled;

    public BOBButton(String text, Color textColor, Color bgColor, Color borderColor, Color borderColorHover, int arc, int borderWidth) {
        this("", text, textColor, bgColor, borderColor, borderColorHover, arc, borderWidth);
    }

    public BOBButton(String id, String text, Color textColor, Color bgColor, Color borderColor, Color borderColorHover, int arc, int borderWidth) {
        super(text);
        this.id = id;
        this.textColor = textColor;
        this.bgColor = bgColor;
        this.borderColor = borderColor;
        this.borderColorHover = borderColorHover;
        this.arcWidth = arc;
        this.arcHeight = arc;
        this.borderWidth = borderWidth;
        this.hoverBgColor = bgColor.brighter();
        this.pressedBgColor = bgColor.darker();

        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setForeground(textColor);
        setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();
        ButtonModel model = getModel();

        Color drawBg = bgColor;
        Color bdColor = borderColor;

        if (model.isPressed() || isSelected()) {
            drawBg = pressedBgColor;
            if (isSelected()) bdColor = Color.WHITE;
        } else if (model.isRollover()) {
            drawBg = hoverBgColor;
            bdColor = borderColorHover;
        }

        g2.setColor(drawBg);
        g2.fillRoundRect(borderWidth / 2, borderWidth / 2, width - borderWidth, height - borderWidth, arcWidth, arcHeight);

        g2.setStroke(new BasicStroke(borderWidth));
        g2.setColor(bdColor);
        g2.drawRoundRect(borderWidth / 2, borderWidth / 2, width - borderWidth, height - borderWidth, arcWidth, arcHeight);

        FontMetrics fm = g2.getFontMetrics();
        int textX = (width - fm.stringWidth(getText())) / 2;
        int textY = (height - fm.getHeight()) / 2 + fm.getAscent();

        g2.setColor(textColor);
        g2.drawString(getText(), textX, textY);

        g2.dispose();
    }

    public void setToggled(boolean toggled) {
        this.toggled = toggled;
    }

    public boolean isToggled() {
        return toggled;
    }

    public String getId() {
        return id;
    }
}