package de.idiotischer.bob.render.menu.components;

import javax.swing.*;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.*;

public class ModernTabbedPane extends JTabbedPane {

    public ModernTabbedPane() {
        setUI(new DarkTabbedPaneUI());
        setBackground(Color.DARK_GRAY);
        setForeground(Color.WHITE);
    }

    private static class DarkTabbedPaneUI extends BasicTabbedPaneUI {

        private final Color bg = Color.DARK_GRAY;
        private final Color selected = new Color(80, 80, 80);
        private final Color border = new Color(60, 60, 60);

        @Override
        protected void installDefaults() {
            super.installDefaults();
            tabAreaInsets = new Insets(5, 5, 5, 5);
        }

        @Override
        protected void paintTabBackground(Graphics g, int tabPlacement,
                                          int tabIndex, int x, int y, int w, int h,
                                          boolean isSelected) {

            g.setColor(isSelected ? selected : bg);
            g.fillRect(x, y, w, h);
        }

        @Override
        protected void paintText(Graphics g, int tabPlacement,
                                 Font font, FontMetrics metrics,
                                 int tabIndex, String title,
                                 Rectangle textRect, boolean isSelected) {

            g.setFont(font);
            g.setColor(Color.WHITE);

            g.drawString(title,
                    textRect.x,
                    textRect.y + metrics.getAscent());
        }

        @Override
        protected void paintFocusIndicator(Graphics g, int tabPlacement,
                                           Rectangle[] rects, int tabIndex,
                                           Rectangle iconRect, Rectangle textRect,
                                           boolean isSelected) {
        }

        @Override
        protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
            g.setColor(border);
            g.drawRect(0, 0, tabPane.getWidth() - 1, tabPane.getHeight() - 1);
        }

        @Override
        protected void paintTabBorder(Graphics g, int tabPlacement,
                                     int tabIndex, int x, int y, int w, int h,
                                     boolean isSelected) {
            g.setColor(border);
            g.drawRect(x, y, w, h);
        }
    }
}