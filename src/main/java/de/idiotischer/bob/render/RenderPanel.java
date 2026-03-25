package de.idiotischer.bob.render;

import de.idiotischer.bob.BOB;
import de.idiotischer.bob.render.menu.Menu;
import de.idiotischer.bob.render.menu.Panel;
import de.idiotischer.bob.render.menu.impl.HUD;
import de.idiotischer.bob.render.menu.impl.ESCMenu;
import de.idiotischer.bob.troop.Troop;
import de.idiotischer.bob.troop.TroopDrawer;
import de.idiotischer.bob.troop.TroopManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.List;

public class RenderPanel extends JPanel implements Panel {

    private final MainRenderer renderer;
    private final Menu menu;
    private BufferedImage frame;

    int curvature = 24;
    private boolean escMenu = false;
    private final ESCMenu escOverlay;

    public RenderPanel(BufferedImage map, MainRenderer renderer) {
        setFrame(map);

        this.renderer = renderer;
        this.menu = new HUD(renderer);
        this.escOverlay = new ESCMenu();

        this.setBackground(Color.BLACK);
        this.setFocusable(true);
        this.requestFocusInWindow();
        this.setIgnoreRepaint(true); // TODO: check if it causes bugs or fixes window flicker on windows
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

        drawTroops(g2);
    }

    private void handleZoom(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        AffineTransform oldTransform = g2.getTransform();

        g2.translate(-renderer.getOffsetX(), -renderer.getOffsetY());

        g2.scale(renderer.getZoom(), renderer.getZoom());

        g2.drawImage(frame, 0, 0,null);

        g2.setTransform(oldTransform);
    }

    public void drawTroops(Graphics2D g2) {
        List<Troop> visible = BOB.getInstance().getTroopManager().getVisible(BOB.getInstance().getPlayer().country());

        TroopDrawer.drawTroops(g2, visible);
    }

    //TODO: gucken ob curvature sinn macht weil bei kleinen selections ist es inakkurat
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

    @Override
    public void mouseClick(MouseEvent e, int x, int y) {
        escOverlay.mouseClick(e, x, y);
    }

    @Override
    public void mouseRelease(MouseEvent e, int x, int y) {
        escOverlay.mouseRelease(e, x, y);
    }

    @Override
    public void mouseMove(MouseEvent e, int x, int y) {
        escOverlay.mouseMove(e, x, y);
    }
}