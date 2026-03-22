package de.idiotischer.bob.render;

import de.idiotischer.bob.render.menu.impl.DefaultMenu;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class MenuPanel extends JPanel {

    private BufferedImage frame;
    private final MainRenderer renderer;

    int layoutScaleX = 800;
    int layoutScaleY = 400;

    public MenuPanel(BufferedImage map, MainRenderer renderer) {
        setFrame(map);

        this.renderer = renderer;

        this.setBackground(Color.BLACK);
        this.setFocusable(true);
        this.requestFocusInWindow();
        this.setPreferredSize(new Dimension(frame.getWidth(), frame.getHeight()));
    }

    public void setFrame(BufferedImage frame) {
        this.frame = frame;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        JButton button = new JButton();
        button.paintComponents(g);

        Graphics2D g2 = (Graphics2D) g;

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.drawImage(frame, 0, 0, getWidth(),getHeight(),this);

        g.setColor(new Color(255, 255, 255, 70));
        g.fillRect(0,0, getWidth(), getHeight());

        g.setColor(Color.DARK_GRAY);
        //center logic so für alles übernehmen
        g.fillRoundRect(getWidth() / 2 - (layoutScaleX / 2), getHeight() / 2 - (layoutScaleY / 2), layoutScaleX, layoutScaleY,32,32);
    }
}
