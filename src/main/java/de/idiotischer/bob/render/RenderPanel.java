package de.idiotischer.bob.render;

import de.idiotischer.bob.BOB;
import de.idiotischer.bob.render.menu.Panel;
import de.idiotischer.bob.render.menu.impl.HUD;
import de.idiotischer.bob.render.menu.impl.ESCMenu;
import de.idiotischer.bob.troop.Troop;
import de.idiotischer.bob.troop.TroopDrawer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.List;

public class RenderPanel extends JPanel implements Panel {

    private final MainRenderer renderer;
    private BufferedImage frame;

    private final HUD hud;
    private final ESCMenu escOverlay;

    private int curvature = 24;
    private boolean escMenu = false;

    public RenderPanel(BufferedImage map, MainRenderer renderer) {
        this.renderer = renderer;
        this.frame = map;

        this.setLayout(new OverlayLayout(this));

        this.escOverlay = new ESCMenu();
        this.hud = new HUD();

        this.add(escOverlay);
        this.add(hud);

        this.escOverlay.setVisible(false);
        this.hud.setVisible(true);

        this.setBackground(Color.BLACK);
        this.setFocusable(true);
        this.requestFocusInWindow();
        this.setIgnoreRepaint(true);
        this.setPreferredSize(new Dimension(frame.getWidth(), frame.getHeight()));
        renderer.getCamera().zoomToMin();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (renderer.getMap() == null) return;

        Graphics2D g2 = (Graphics2D) g;
        AffineTransform screenTransform = g2.getTransform();

        g2.transform(renderer.getCamera().getTransform());
        g2.drawImage(renderer.getMap(), 0, 0, null);

        if (renderer.getVisualBorderOverlay() != null) {
            g2.drawImage(renderer.getVisualBorderOverlay(), 0, 0, null);
        }

        drawTroops(g2);

        g2.setTransform(screenTransform);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        handleDragOverlay(g2);
    }

    public void drawTroops(Graphics2D g2) {
        List<Troop> visible = BOB.getInstance().getTroopManager().getVisible(BOB.getInstance().getPlayer().country());
        TroopDrawer.drawTroops(g2, visible);
    }

    private void handleDragOverlay(Graphics2D g2) {
        if(escMenu) return;

        Point start = renderer.getDragStart();
        Point end = renderer.getDragEnd();

        if (start != null && end != null) {
            int x = Math.min(start.x, end.x);
            int y = Math.min(start.y, end.y);
            int w = Math.abs(start.x - end.x);
            int h = Math.abs(start.y - end.y);

            g2.setColor(new Color(255, 255, 255, 50));
            g2.fillRoundRect(x, y, w, h, curvature, curvature);

            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2));
            g2.drawRoundRect(x, y, w, h, curvature, curvature);
        }
    }

    public void setEscMenu(boolean on) {
        this.escMenu = on;
        this.escOverlay.setVisible(on);

        this.revalidate();
        //this.repaint();
    }

    public boolean isEscMenu() {
        return escMenu;
    }

    public boolean isPaused() {
        return escMenu;
    }

    public HUD getHud() {
        return hud;
    }

    @Override
    public void mouseClick(MouseEvent e, int x, int y) {}

    @Override
    public void mouseRelease(MouseEvent e, int x, int y) {}

    @Override
    public void mouseMove(MouseEvent e, int x, int y) {}

    public void setFrame(BufferedImage frame) {
        this.frame = frame;
    }

    public BufferedImage getFrame() {
        return frame;
    }
}