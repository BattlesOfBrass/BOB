package de.idiotischer.bob.render.menu.components;

import javax.swing.*;
import java.awt.*;

public class HUDTopBar extends JPanel {
    private String countryName = "---";

    public HUDTopBar() {
        setOpaque(false);
        setLayout(new BorderLayout());
    }

    public void setCountryName(String name) {
        this.countryName = name;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;

        g2.setColor(Color.DARK_GRAY);
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.setStroke(new BasicStroke(12));
        g2.setColor(Color.DARK_GRAY.darker());
        g2.drawRect(0, 0, getWidth(), getHeight());

        g2.setColor(Color.GREEN);
        Font font = new Font(Font.SANS_SERIF, Font.BOLD, 15);
        g2.setFont(font);

        String text = "Current country: " + countryName;

        FontMetrics fm = g2.getFontMetrics();

        int x = 20;

        int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(text, x, y);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension parentSize = getParent() != null ? getParent().getSize() : new Dimension(0, 40);
        return new Dimension(parentSize.width, 40);
    }
}