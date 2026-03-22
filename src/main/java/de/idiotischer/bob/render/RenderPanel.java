package de.idiotischer.bob.render;

import de.idiotischer.bob.render.menu.Menu;
import de.idiotischer.bob.render.menu.impl.DefaultMenu;
import de.idiotischer.bob.render.menu.impl.ESCMenu;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

public class RenderPanel extends JPanel {

    private final MainRenderer renderer;
    private final Menu menu;
    private BufferedImage frame;

    int curvature = 24;
    private boolean escMenu = false;
    private final ESCMenu escOverlay;

    public RenderPanel(BufferedImage map, MainRenderer renderer) {
        setFrame(map);

        this.renderer = renderer;
        this.menu = new DefaultMenu(renderer);
        this.escOverlay = new ESCMenu();

        this.setBackground(Color.BLACK);
        this.setFocusable(true);
        this.requestFocusInWindow();
        this.setPreferredSize(new Dimension(frame.getWidth(), frame.getHeight()));
    }

    public void setFrame(BufferedImage frame) {
        this.frame = frame;
    }

    //@Override
    //public Dimension getPreferredSize() {
    //    return new Dimension(frame.getWidth(), frame.getHeight());
    //}

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (frame == null) return;

        Graphics2D g2 = (Graphics2D) g;

        handleZoom(g2); //map vor antialiasing rendern

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        handleDragOverlay(g2);

        //wegen z als letztes
        menu.paint(g);

        if(escMenu) {
            escOverlay.paint(g);
        }

        repaint();
    }

    private void handleZoom(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        AffineTransform oldTransform = g2.getTransform();

        g2.translate(-renderer.getOffsetX(), -renderer.getOffsetY());

        g2.scale(renderer.getZoom(), renderer.getZoom());

        g2.drawImage(frame, 0, 0, getWidth(),getHeight(),null);

        g2.setTransform(oldTransform);
    }

    //TODO: gucken ob curvature sinn macht weil bei kleinen selections ist es inakkurat
    private void handleDragOverlay(Graphics2D g2) {
        Point start = renderer.getDragStart();
        Point end = renderer.getDragEnd();

        if (start != null && end != null) {
            int x = Math.min(start.x, end.x);
            int y = Math.min(start.y, end.y);
            int w = Math.abs(start.x - end.x);
            int h = Math.abs(start.y - end.y);

            g2.setColor(new Color(255, 255, 255, 50));
            g2.fillRoundRect(x, y, w, h,curvature, curvature);

            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2));
            g2.drawRoundRect(x, y, w, h,curvature, curvature);
        }
    }

    public BufferedImage getFrame() {
        return frame;
    }

    public void setEscMenu(boolean on) {
        this.escMenu = on;
    }

    public boolean isEscMenu() {
        return escMenu;
    }

    public boolean isPaused() {
        return escMenu; // + andere sachen später
    }

    public void onClick(MouseEvent e, int x, int y) {
        escOverlay.mouseClick(e, x, y);
    }

    public void onRelease(MouseEvent e, int x, int y) {
        escOverlay.mouseRelease(e, x, y);
    }

    public void onMove(MouseEvent e, int x, int y) {
        escOverlay.mouseMove(e, x, y);
    }
}