package de.idiotischer.bob.render.menu.components;

import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;

public class ModernScrollBarUI extends BasicScrollBarUI {

    @Override
    protected JButton createDecreaseButton(int orientation) { return createZeroButton(); }
    @Override
    protected JButton createIncreaseButton(int orientation) { return createZeroButton(); }

    private JButton createZeroButton() {
        JButton jbutton = new JButton();
        jbutton.setPreferredSize(new Dimension(0, 0));
        jbutton.setMinimumSize(new Dimension(0, 0));
        jbutton.setMaximumSize(new Dimension(0, 0));
        return jbutton;
    }

    @Override
    protected void installDefaults() {
        super.installDefaults();
        scrollbar.setOpaque(false);
    }

    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
        g.setColor(Color.GRAY);
        g.fillRoundRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height, 12,12);
    }

    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
        if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Color.DARK_GRAY.brighter());

        if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
            g2.fillRoundRect(thumbBounds.x + 2, thumbBounds.y,
                    thumbBounds.width - 4, thumbBounds.height,
                    10, 10);
        } else {
            g2.fillRoundRect(thumbBounds.x, thumbBounds.y + 2,
                    thumbBounds.width, thumbBounds.height - 4,
                    10, 10);
        }

        g2.dispose();
    }
}