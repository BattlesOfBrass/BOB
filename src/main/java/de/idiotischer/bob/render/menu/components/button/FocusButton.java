package de.idiotischer.bob.render.menu.components.button;

import de.idiotischer.bob.BOB;
import de.idiotischer.bob.focus.Focus;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class FocusButton extends JToggleButton {

    private final String treeId;
    private final Point p;
    private final Focus focus;
    private final String country;

    public FocusButton(String id, String country, Focus focus, Point point, Consumer<FocusButton> onCLick) {
        this.focus = focus;
        this.p = point;
        this.treeId = id;
        this.country = country;

        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setFocusable(false);


        addActionListener(e -> onCLick.accept(this));
        //addActionListener(e -> {
        //    //open small ui that gives description etc than have a start button like in hoi4 or a cancel button depending on if the focus is active or not
        //});
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();

        try {
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            Color bg = BOB.getInstance().getSettingsTheme().button().bgColor();
            Color border = BOB.getInstance().getSettingsTheme().button().borderColor().darker();

            g2.setColor(bg);
            g2.fillRect(0, 0, getWidth(), getHeight());

            g2.setColor(border);
            g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);

            float fontSize = (float) (12 * getWidth() / 120.0);
            g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, Math.max(1, Math.round(fontSize))));
            FontMetrics fm = g2.getFontMetrics();

            String name = focus.getName();
            int maxWidth = getWidth() - 12;

            String suffix = "...";

            if (fm.stringWidth(name) > maxWidth) {
                while (!name.isEmpty() && fm.stringWidth(name + suffix) > maxWidth) name = name.substring(0, name.length() - 1);
                name += suffix;
            }

            g2.setColor(Color.WHITE);

            int x = (getWidth() - fm.stringWidth(name)) / 2;
            int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();

            g2.drawString(name, x, y);

        } finally {
            g2.dispose();
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(120, 50);
    }

    public String getTreeId() {
        return treeId;
    }

    public Focus getFocus() {
        return focus;
    }

    public Point getPoint() {
        return p;
    }
}