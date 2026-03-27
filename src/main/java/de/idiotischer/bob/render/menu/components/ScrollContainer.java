package de.idiotischer.bob.render.menu.components;

import de.idiotischer.bob.BOB;
import de.idiotischer.bob.render.menu.Component;
import de.idiotischer.bob.render.menu.components.button.ButtonComp;
import de.idiotischer.bob.render.menu.components.button.IButtonComp;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.List;


//TODO: BUGS
// - scrolling lwk woirks between two clicks, not when unrelasing
// - scrollable height ain't calculated right (just using a placeholder now)
// - the thumb always snaps to the top or bottom and doesnt stay where it was dragged
// - buttons are scrolled too much (prob bc of placeholders
// - buttons are now defaulty too high up
// - on first scroll all offsets between the buttons are lost and they get merged on the same spot and scroll that way too
public class ScrollContainer implements Component {
    private final JPanel panel;
    private final Color color;

    private final boolean centered;

    private Rectangle bounds;
    private List<ButtonComp> children = new ArrayList<>();

    private int scrollOffset = 0;

    private final int padding = 10;
    private final int spacing = 10;

    private int scrollbarWidth = 12;
    private Rectangle scrollBounds;

    private boolean isDragging = false;
    private int dragStartY = 0;
    private int dragStartOffset = 0;

    private int totalContentHeight = 20000;
    private int visualThumbY = 0;

    public ScrollContainer(JPanel panel, Color color, boolean centered) {
        bounds = new Rectangle(0, 0, 0, 0);
        this.panel = panel;
        this.color = color;
        this.centered = centered;
    }

    public void setChildren(List<ButtonComp> children) {
        this.children = children;
        layoutChildren();
    }

    //IMPORTANT BC THIS MODIFIES THE RECTANGLE
    private void layoutChildren() {
        int yOffset = padding;

        for (Component child : children) {
            if (child instanceof ButtonComp btn) {
                Rectangle b = btn.getBounds();
                b.y += yOffset - scrollOffset;
                yOffset += b.height + spacing;
            }
        }

        this.totalContentHeight = yOffset;
    }

    @Override
    public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;

        JPanel pl = panel != null ? panel : BOB.getInstance().getMainRenderer().getGamePanel();

        Rectangle bounds = getActualBounds();

        int x = centered ? (pl.getWidth() - bounds.width) / 2 : bounds.x;
        int y = centered ? (pl.getHeight() - bounds.height) / 2 : bounds.y;

        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(3));
        g2.drawRoundRect(x, y, bounds.width, bounds.height, 24, 24);

        g2.setClip(x, y, bounds.width, bounds.height);

        //this will be the image container btu i am too stupdi so i just copied the scrollabr xD
        int heightShrink = 40;

        int imgFrameWidth = 350;
        int frameX = x + bounds.width - imgFrameWidth - 2;
        int bufferX = heightShrink / 2;

        g2.drawRoundRect(
                frameX - bufferX,
                y + (heightShrink / 2),
                imgFrameWidth,
                bounds.height - heightShrink,
                24,
                24
        );

        //scrollbar ykyk
        int barX = x + (bounds.width / 2) - scrollbarWidth - 2;

        this.scrollBounds = new Rectangle(barX - 15, y + (heightShrink / 2), scrollbarWidth, bounds.height - heightShrink);

        g2.drawRoundRect(
                scrollBounds.x,
                scrollBounds.y,
                scrollbarWidth,
                bounds.height - heightShrink,
                12,
                12
        );

        if (totalContentHeight > 0) {
            int drawY = isDragging ? visualThumbY : scrollBounds.y + (int)((scrollBounds.height - 30) * ((float)scrollOffset / (totalContentHeight - bounds.height)));

            g2.setColor(new Color(255, 255, 255, 180));
            g2.fillRoundRect(
                    scrollBounds.x + 2,
                    drawY,
                    scrollbarWidth - 3,
                    30,
                    8,
                    8
            );
        }

        for (ButtonComp child : children) {
            //child.setDebug(true);
            child.paint(g2);
        }
    }

    public Rectangle getThumbBounds() {
        if (scrollBounds == null) {
            System.out.println("scrollBounds == null");
            return new Rectangle(0, 0, 0, 0);
        }

        if (totalContentHeight <= bounds.height) {
            System.out.println("heighht lic is the full bug");
            return new Rectangle(0, 0, 0, 0);
        }

        int thumbHeight = 30;
        int maxScroll = totalContentHeight - bounds.height;

        if (maxScroll <= 0) maxScroll = 1;

        float scrollPercent = (float) scrollOffset / maxScroll;

        int thumbY = scrollBounds.y +
                (int) ((scrollBounds.height - thumbHeight) * scrollPercent);

        return new Rectangle(scrollBounds.x + 2, thumbY, scrollbarWidth - 3, thumbHeight);
    }

    @Override
    public void mouseScroll(MouseWheelEvent e, int x, int y) {

    }

    @Override
    public void mouseClick(MouseEvent e, int x, int y) {
        int maxScroll = Math.max(1, totalContentHeight - bounds.height);
        int currentThumbY = scrollBounds.y + (int)((scrollBounds.height - 30) * ((float)scrollOffset / maxScroll));

        Rectangle thumbHitbox = getThumbBounds();

        System.out.println("Mouse clicked at: " + x + ", " + y);
        System.out.println("Thumb bounds: " + thumbHitbox);

        if (thumbHitbox.contains(x, y)) {
            System.out.println(">>> CLICKED SCROLL THUMB <<<");

            isDragging = true;
            dragStartY = y;
            dragStartOffset = scrollOffset;
            visualThumbY = currentThumbY;
        } else {
            isDragging = false;
            System.out.println("Clicked outside thumb");
        }

        children.forEach(component -> component.mouseClick(e, x, y));
    }

    @Override
    public void mouseRelease(MouseEvent e, int x, int y) {
        isDragging = false;
        children.forEach(component -> component.mouseRelease(e, x, y));
    }

    @Override
    public void mouseMove(MouseEvent e, int x, int y) {
        if (isDragging) {
            System.out.println("Dragging... mouseY=" + y);

            int deltaY = y - dragStartY;
            float scrollRatio = (float) totalContentHeight / scrollBounds.height;
            this.scrollOffset = dragStartOffset + (int)(deltaY * scrollRatio);

            int maxScroll = Math.max(0, totalContentHeight - getActualBounds().height);
            if (scrollOffset < 0) scrollOffset = 0;
            if (scrollOffset > maxScroll) scrollOffset = maxScroll;

            this.visualThumbY = Math.max(scrollBounds.y, Math.min(y, scrollBounds.y + scrollBounds.height - 30));

            System.out.println("ScrollOffset: " + scrollOffset);
            System.out.println("ThumbY: " + visualThumbY);

            layoutChildren();
        }

        children.forEach(component -> component.mouseMove(e, x, y));
    }

    public void setBounds(Rectangle rectangle) {
        this.bounds = rectangle;
    }

    public Rectangle getActualBounds() {
        JPanel pl = panel != null ? panel : BOB.getInstance().getMainRenderer().getGamePanel();
        int x = centered ? pl.getWidth() / 2 - (bounds.width / 2) : bounds.x;
        int y = centered ? pl.getHeight() / 2 - (bounds.height / 2) : bounds.y;
        x += bounds.x;
        y -= bounds.y;
        return new Rectangle(x, y, bounds.width, bounds.height);
    }

    public Rectangle getScrollBounds() {
        return scrollBounds;
    }

    public Rectangle getActualScrollBounds() {
        JPanel pl = panel != null ? panel : BOB.getInstance().getMainRenderer().getGamePanel();
        int x = centered ? pl.getWidth() / 2 - (scrollBounds.width / 2) : scrollBounds.x;
        int y = centered ? pl.getHeight() / 2 - (scrollBounds.height / 2) : scrollBounds.y;
        x += scrollBounds.x;
        y -= scrollBounds.y;
        return new Rectangle(x, y, scrollBounds.width, scrollBounds.height);
    }

    public Rectangle getBounds() {
        return bounds;
    }

    public boolean isCentered() {
        return centered;
    }

    public List<ButtonComp> getChildren() {
        return children;
    }

    public JPanel getPanel() {
        return panel;
    }
}