package de.idiotischer.bob.render;

import de.idiotischer.bob.BOB;
import de.idiotischer.bob.render.menu.components.button.TroopVisualButton;

import javax.swing.*;
import java.awt.*;
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

        g2.setColor(new Color(255, 255, 255, 50));
        g2.fillRoundRect(x, y, w, h, curvature, curvature);

        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(x, y, w, h, curvature, curvature);
    }

    public void onDragRelease(boolean shift) {
        selectTroopsInDrag(shift);
    }

    private void selectTroopsInDrag(boolean shift) {
        Point start = renderer.getDragStart();
        Point end = renderer.getDragEnd();

        if (start == null || end == null) return;

        Rectangle selection = new Rectangle(
                Math.min(start.x, end.x),
                Math.min(start.y, end.y),
                Math.abs(start.x - end.x),
                Math.abs(start.y - end.y)
        );

        if(!shift) renderer.getGamePanel().selected.clear();

        for (Component c : renderer.getGamePanel().getTroopLayer().getComponents()) {
            if (!(c instanceof TroopVisualButton button)) continue;

            if (!button.isVisible()) continue;

            if(!Objects.equals(button.getStack().getController().getAbbreviation(), BOB.getInstance().getPlayer().country().getAbbreviation())) continue;

            if (selection.intersects(button.getBounds())) {
                renderer.getGamePanel().selected.add(button);
            }
        }
    }
}