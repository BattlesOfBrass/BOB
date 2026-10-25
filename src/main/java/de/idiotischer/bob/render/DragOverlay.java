package de.idiotischer.bob.render;

import de.idiotischer.bob.BOB;
import de.idiotischer.bob.render.menu.components.button.TroopVisualButton;
import de.idiotischer.bob.tile.Tile;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.util.Objects;

public class DragOverlay extends JComponent {

    private final MainRenderer renderer;
    private int curvature = 24;

    public DragOverlay(MainRenderer renderer) {
        this.renderer = renderer;
        setOpaque(false);
        setVisible(true);
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (renderer.getGamePanel().isEscMenu()) return;

        Point start = renderer.getDragStart();
        Point end = renderer.getDragEnd();

        if (start == null || end == null) return;

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int x = Math.min(start.x, end.x);
        int y = Math.min(start.y, end.y);
        int w = Math.abs(start.x - end.x);
        int h = Math.abs(start.y - end.y);

        if (renderer.getDragButton() == MouseEvent.BUTTON3) {
            g2.setColor(Color.RED);

            AffineTransform cameraTransform = renderer.getCamera().getTransform();
            g2.transform(cameraTransform);

            Tile previous = null;
            int tiles = 0;

            for (Tile tile : renderer.getDraggedTiles()) {
                Point current = tile.getPoints().getFirst();

                if(tiles + 1 > renderer.getGamePanel().getTroopButtonGroup().size()) break;

                if (previous != null) {
                    Point previousPoint = previous.getPoints().getFirst();
                    g2.drawLine(previousPoint.x, previousPoint.y, current.x, current.y);
                }

                previous = tile;
                tiles++;
            }
        } else if (renderer.getDragButton() == MouseEvent.BUTTON1) {
            g2.setColor(new Color(255, 255, 255, 50));
            g2.fillRoundRect(x, y, w, h, curvature, curvature);

            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2));
            g2.drawRoundRect(x, y, w, h, curvature, curvature);
        }
    }

    public void onDragRelease(boolean shift) {
        selectTroopsInDrag(shift);
    }

    private void selectTroopsInDrag(boolean shift) {
        if (renderer.getDragButton() != MouseEvent.BUTTON1) return;

        Point start = renderer.getDragStart();
        Point end = renderer.getDragEnd();

        if (start == null || end == null) return;

        Rectangle selection = new Rectangle(Math.min(start.x, end.x), Math.min(start.y, end.y), Math.abs(start.x - end.x), Math.abs(start.y - end.y));

        if(!shift) renderer.getGamePanel().selected.clear();

        for (Component c : renderer.getGamePanel().getTroopLayer().getComponents()) {
            if (!(c instanceof TroopVisualButton button)) continue;

            if (!button.isVisible()) continue;

            if(BOB.getInstance().getPlayer() == null) continue;
            if(BOB.getInstance().getPlayer().country() == null) continue;

            if(!Objects.equals(button.getStack().getController().getAbbreviation(), BOB.getInstance().getPlayer().country().getAbbreviation())) continue;

            if (selection.intersects(button.getBounds())) {
                renderer.getGamePanel().selected.add(button);
            }
        }
    }
}