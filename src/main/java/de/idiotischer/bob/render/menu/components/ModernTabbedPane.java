package de.idiotischer.bob.render.menu.components;

import de.idiotischer.bob.BOB;

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

        @Override
        protected void installDefaults() {
            super.installDefaults();
            tabAreaInsets = new Insets(5, 5, 5, 5);
        }

        @Override
        protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected) {
            Color dark = BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().palette().darkColor();

            g.setColor(isSelected ? brighter(dark,20) : dark);
            g.fillRect(x, y, w, h);
        }

        private static Color brighter(Color color, double factor) {
            int r = (int) Math.min(255, color.getRed() * (1.0 + factor / 100.0));
            int g = (int) Math.min(255, color.getGreen() * (1.0 + factor / 100.0));
            int b = (int) Math.min(255, color.getBlue() * (1.0 + factor / 100.0));

            return new Color(r, g, b, color.getAlpha());
        }

        @Override
        protected void paintText(Graphics g, int tabPlacement, Font font, FontMetrics metrics, int tabIndex, String title, Rectangle textRect, boolean isSelected) {
            g.setFont(font);
            g.setColor(Color.WHITE);

            g.drawString(title, textRect.x, textRect.y + metrics.getAscent());
        }

        @Override
        protected void paintFocusIndicator(Graphics g, int tabPlacement, Rectangle[] rects, int tabIndex, Rectangle iconRect, Rectangle textRect, boolean isSelected) {
        }

        @Override
        protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
            Color dark = BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().palette().darkColor();

            g.setColor(dark);
            g.drawRect(0, 0, tabPane.getWidth() - 1, tabPane.getHeight() - 1);
        }

        @Override
        protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected) {
            Color dark = BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().palette().darkColor();

            g.setColor(dark);
            g.drawRect(x, y, w, h);
        }
    }
}