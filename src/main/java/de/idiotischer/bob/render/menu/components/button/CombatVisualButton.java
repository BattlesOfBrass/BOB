package de.idiotischer.bob.render.menu.components.button;

import de.idiotischer.bob.combat.CombatStatus;
import javax.swing.*;
import java.awt.*;

public class CombatVisualButton extends JButton {
    private CombatStatus status;
    private double dirX;
    private double dirY;

    private int battleProgress = 84;

    public CombatVisualButton(CombatStatus status) {
        this.status = status;

        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setFocusable(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int size = Math.min(w, h) - 15;

        int cx = w / 2;
        int cy = h / 2;
        int radius = size / 2;

        g2.translate(cx, cy);

        if (dirX != 0 || dirY != 0) {
            double angle = Math.atan2(dirY, dirX);
            g2.rotate(angle);

            g2.setColor(new Color(0, 170, 0));
            int[] xPoints = {radius - 5, radius - 5, radius + 15};
            int[] yPoints = {-12, 12, 0};
            g2.fillPolygon(xPoints, yPoints, 3);

            g2.setColor(new Color(0, 210, 0));
            int[] xPointsInner = {radius - 4, radius - 4, radius + 11};
            int[] yPointsInner = {-9, 9, 0};
            g2.fillPolygon(xPointsInner, yPointsInner, 3);

            g2.rotate(-angle);
        }

        g2.setColor(new Color(20, 80, 20));
        g2.fillOval(-radius, -radius, size, size);

        GradientPaint greens = new GradientPaint(0, -radius, new Color(0, 190, 0), 0, radius, new Color(0, 170, 0));
        g2.setPaint(greens);
        g2.fillOval(-radius + 2, -radius + 2, size - 4, size - 4);

        g2.setColor(new Color(255, 255, 255, 120));
        g2.fillOval(-radius + 6, -radius + 3, size - 12, radius - 2);

        int innerW = (int) (size * 0.75);
        int innerH = (int) (size * 0.45);
        int ix = -innerW / 2;
        int iy = -innerH / 4;

        //g2.setColor(new Color(40, 40, 40));
        //g2.fillRoundRect(ix, iy, innerW, innerH, 10, 10);
        //g2.setColor(new Color(100, 100, 100));
        //g2.setStroke(new BasicStroke(1.5f));
        //g2.drawRoundRect(ix, iy, innerW, innerH, 10, 10);

        g2.setColor(Color.WHITE);
        Font font = getFont().deriveFont(Font.BOLD, (int)(innerH * 0.75));
        g2.setFont(font);

        String text = String.valueOf(status != null ? status.getPower(status.getAttackers()) : battleProgress);
        FontMetrics fm = g2.getFontMetrics();
        int tx = -fm.stringWidth(text) / 2;
        int ty = iy + ((innerH - fm.getHeight()) / 2) + fm.getAscent();

        g2.drawString(text, tx, ty);

        g2.dispose();
    }

    public CombatStatus getStatus() {
        return status;
    }

    public void setDirection(int ax, int ay, int dx, int dy) {
        double vx = dx - ax;
        double vy = dy - ay;

        double len = Math.sqrt(vx * vx + vy * vy);
        if (len == 0) {
            dirX = 0;
            dirY = 0;
            return;
        }

        dirX = vx / len;
        dirY = vy / len;
        repaint();
    }
}