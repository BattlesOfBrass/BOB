package de.idiotischer.bob.render.menu.components;

import de.idiotischer.bob.BOB;
import de.idiotischer.bob.render.menu.Component;
import de.idiotischer.bob.render.menu.components.button.ButtonComp;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.List;

public class ScrollContainer implements Component {
    private final JPanel panel;
    private final Color color;
    private final boolean centered;

    private Rectangle bounds;
    private List<ButtonComp> children = new ArrayList<>();

    private int scrollOffset = 0;
    private final int padding = 20;
    private final int spacing = 15;

    private final int scrollbarWidth = 12;
    private Rectangle scrollBounds;

    private boolean isDragging = false;
    private int dragStartMouseY = 0;
    private int dragStartOffset = 0;

    private int totalContentHeight = 0;
    private final int thumbHeight = 40;
    private final boolean centeredUseY;
    private int centeredYOffset = 0;

    public ScrollContainer(JPanel panel, Color color, boolean centered) {
        this(panel, color, centered, false);
    }

    public ScrollContainer(JPanel panel, Color color, boolean centered, boolean centeredUseY) {
        this.bounds = new Rectangle(0, 0, 0, 0);
        this.panel = panel;
        this.color = color;
        this.centered = centered;
        this.centeredUseY = centeredUseY;
    }

    public void setChildren(List<ButtonComp> children) {
        this.children = children;
        layoutChildren();
    }

    private void layoutChildren() {
        int yOffset = padding;

        for (ButtonComp btn : children) {
            Rectangle b = btn.getBounds();
            b.y = -yOffset;

            yOffset += b.height + spacing;
        }

        this.totalContentHeight = yOffset + padding;
    }

    @Override
    public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Rectangle actualBounds = getActualBounds();

        g2.setColor(Color.DARK_GRAY.darker());
        g2.setStroke(new BasicStroke(3));
        g2.drawRoundRect(actualBounds.x, actualBounds.y, actualBounds.width, actualBounds.height, 24, 24);

        int margin = 10;
        this.scrollBounds = new Rectangle(
                actualBounds.x + actualBounds.width - scrollbarWidth - margin,
                actualBounds.y + margin,
                scrollbarWidth,
                actualBounds.height - (margin * 2)
        );

        Shape oldClip = g2.getClip();

        if (totalContentHeight > actualBounds.height) {

            g2.setStroke(new BasicStroke(16));
            g2.setColor(new Color(255, 255, 255, 50));
            g2.fillRoundRect(scrollBounds.x, scrollBounds.y, scrollBounds.width, scrollBounds.height, 12, 12);

            Rectangle thumb = getThumbBounds();
            g2.setColor(new Color(255, 255, 255, 180));
            g2.fillRoundRect(thumb.x, thumb.y, thumb.width, thumb.height, 8, 8);
        }

        g2.setClip(actualBounds.x, actualBounds.y, actualBounds.width, actualBounds.height);

        for (ButtonComp child : children) {
            Graphics2D gChild = (Graphics2D) g2.create();

            gChild.translate(actualBounds.x, actualBounds.y - scrollOffset);
            child.paint(gChild);
            gChild.dispose();
        }

        g2.setClip(oldClip);

        g2.dispose();
    }

    public Rectangle getThumbBounds() {
        Rectangle actualBounds = getActualBounds();
        if (totalContentHeight <= actualBounds.height) return new Rectangle(0,0,0,0);

        int maxScroll = totalContentHeight - actualBounds.height;
        float scrollPercent = (float) scrollOffset / maxScroll;

        int availableHeight = scrollBounds.height - thumbHeight;
        int thumbY = scrollBounds.y + (int) (availableHeight * scrollPercent);

        return new Rectangle(scrollBounds.x + 2, thumbY, scrollbarWidth - 4, thumbHeight);
    }

    private void dispatchEventToChildren(MouseEvent e, String type) {
        Rectangle actualBounds = getActualBounds();

        if (!type.equals("move") && !actualBounds.contains(e.getPoint())) {
            return;
        }

        int relX = e.getX() - actualBounds.x;
        int relY = e.getY() - actualBounds.y + scrollOffset;

        MouseEvent translatedEvent = new MouseEvent(
                (java.awt.Component) e.getSource(), e.getID(), e.getWhen(), e.getModifiersEx(),
                relX, relY, e.getClickCount(), e.isPopupTrigger(), e.getButton()
        );

        for (ButtonComp child : children) {
            switch (type) {
                case "click" -> child.mouseClick(translatedEvent, relX, relY);
                case "move" -> child.mouseMove(translatedEvent, relX, relY);
                case "release" -> child.mouseRelease(translatedEvent, relX, relY);
            }
        }
    }

    @Override
    public void mouseClick(MouseEvent e, int x, int y) {
        if (getThumbBounds().contains(e.getPoint())) {
            isDragging = true;
            dragStartMouseY = e.getY();
            dragStartOffset = scrollOffset;
        } else {
            dispatchEventToChildren(e, "click");
        }
    }

    @Override
    public void mouseMove(MouseEvent e, int x, int y) {
        if (isDragging) {
            Rectangle actualBounds = getActualBounds();
            int deltaY = e.getY() - dragStartMouseY;

            float scrollRatio = (float) (totalContentHeight - actualBounds.height) / (scrollBounds.height - thumbHeight);
            scrollOffset = dragStartOffset + (int) (deltaY * scrollRatio);

            int maxScroll = Math.max(0, totalContentHeight - actualBounds.height);
            scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
        } else {
            dispatchEventToChildren(e, "move");
        }
    }

    public boolean isCentered() {
        return centered;
    }

    public JPanel getPanel() {
        return panel;
    }

    @Override
    public void mouseRelease(MouseEvent e, int x, int y) {
        isDragging = false;
        dispatchEventToChildren(e, "release");
    }

    @Override
    public void mouseScroll(MouseWheelEvent e, int x, int y) {
        Rectangle actualBounds = getActualBounds();

        if (!actualBounds.contains(x, y)) return;

        int scrollAmount = e.getWheelRotation() * 30;
        scrollOffset += scrollAmount;

        int maxScroll = Math.max(0, totalContentHeight - actualBounds.height);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
    }

    public Rectangle getActualBounds() {
        JPanel pl = panel != null
                ? panel
                : BOB.getInstance().getMainRenderer().getGamePanel();

        if (pl == null) return bounds;

        int x = bounds.x;
        int y = bounds.y;

        if (centered) {
            x = (pl.getWidth() - bounds.width) / 2;
            y = (pl.getHeight() - bounds.height) / 2;
            if(centeredUseY) {
                y -= centeredYOffset;
            }
        }

        return new Rectangle(x, y, bounds.width, bounds.height);
    }

    public void setCenteredYOffset(int centeredYOffset) {
        this.centeredYOffset = centeredYOffset;
    }

    public List<ButtonComp> getChildren() {
        return children;
    }

    public void setBounds(Rectangle bounds) { this.bounds = bounds; layoutChildren(); }
}