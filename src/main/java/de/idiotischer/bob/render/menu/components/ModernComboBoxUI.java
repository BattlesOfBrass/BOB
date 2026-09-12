package de.idiotischer.bob.render.menu.components;

import de.idiotischer.bob.BOB;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class ModernComboBoxUI extends BasicComboBoxUI {

    private static final Color BG = new Color(60, 60, 60);
    private static final Color BG2 = new Color(70, 70, 70);
    private static final Color TC = new Color(220, 220, 220);

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);

        JComboBox<?> box = (JComboBox<?>) c;
        box.setOpaque(false);
        box.setForeground(TC);
        box.setBackground(BG);
        box.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
    }

    @Override
    protected ComboPopup createPopup() {
        return new BasicComboPopup(comboBox) {

            @Override
            protected JScrollPane createScroller() {
                JScrollPane scrollPane = super.createScroller();

                JScrollBar vertical = scrollPane.getVerticalScrollBar();
                vertical.setUI(new ModernScrollBarUI());
                vertical.setPreferredSize(new Dimension(8, Integer.MAX_VALUE));
                vertical.setUnitIncrement(16);

                scrollPane.setBorder(BorderFactory.createEmptyBorder());
                scrollPane.getViewport().setOpaque(false);
                scrollPane.setOpaque(false);

                return scrollPane;
            }
        };
    }

    @Override
    protected JButton createArrowButton() {
        JButton arrow = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Color dark = BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().palette().defaultColor();

                g2.setColor(dark);

                int[] x = {6, 11, 16};
                int[] y = {8, 14, 8};

                g2.fillPolygon(x, y, 3);
                g2.dispose();
            }
        };

        arrow.setPreferredSize(new Dimension(20, 20));
        arrow.setContentAreaFilled(false);
        arrow.setBorderPainted(false);
        arrow.setFocusPainted(false);
        arrow.setOpaque(false);

        return arrow;
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = c.getWidth();
        int h = c.getHeight();
        Color dark = BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().palette().darkColor();

        g2.setColor(dark);
        g2.fillRoundRect(0, 0, w - 1, h - 1, 12, 12);

        Shape border = new RoundRectangle2D.Float(1, 1, w - 3, h - 3, 12, 12);

        Color def = BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().palette().defaultColor();

        g2.setColor(def);
        g2.setStroke(new BasicStroke(1f));
        g2.draw(border);

        g2.dispose();

        super.paint(g, c);
    }

    @Override
    protected ListCellRenderer<Object> createRenderer() {
        return new DefaultListCellRenderer() {

            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                label.setOpaque(true);
                label.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

                Color light = BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().palette().lightColor();

                label.setForeground(brighter(light, 30));

                Color dark = BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().palette().darkColor();

                if (isSelected) {
                    label.setBackground(dark.brighter());
                } else {
                    label.setBackground(index % 2 == 0 ? dark : brighter(dark,8));
                }

                return label;
            }
        };
    }


    private static Color brighter(Color color, double factor) {
        int r = (int) Math.min(255, color.getRed() * (1.0 + factor / 100.0));
        int g = (int) Math.min(255, color.getGreen() * (1.0 + factor / 100.0));
        int b = (int) Math.min(255, color.getBlue() * (1.0 + factor / 100.0));

        return new Color(r, g, b, color.getAlpha());
    }
}