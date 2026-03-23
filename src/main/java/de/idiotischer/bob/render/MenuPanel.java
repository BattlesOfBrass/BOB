package de.idiotischer.bob.render;

import de.idiotischer.bob.render.menu.Menu;
import de.idiotischer.bob.render.menu.Panel;
import de.idiotischer.bob.render.menu.impl.ScenarioSelectMenu;
import de.idiotischer.bob.render.menu.impl.StartMenu;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class MenuPanel extends JPanel implements Panel {

    private BufferedImage frame;
    private final MainRenderer renderer;

    private final int layoutScaleX = 800;
    private final int layoutScaleY = 400;

    private boolean scenarioSelect = false;

    private final ScenarioSelectMenu scenarioSelectMenu;
    private final StartMenu startMenu;

    private final List<Menu> menuList = new ArrayList<>();

    public MenuPanel(BufferedImage map, MainRenderer renderer) {
        setFrame(map);

        this.renderer = renderer;

        this.scenarioSelectMenu = new ScenarioSelectMenu(this, layoutScaleX, layoutScaleY);
        this.startMenu = new StartMenu(this);

        this.menuList.add(scenarioSelectMenu);
        this.menuList.add(startMenu);


        this.setBackground(Color.BLACK);
        this.setFocusable(true);
        this.requestFocusInWindow();
        this.setIgnoreRepaint(true); // TODO: check if it causes bugs or fixes window flicker on windows
        this.setPreferredSize(new Dimension(frame.getWidth(), frame.getHeight()));
    }

    public void setInScenarioSelect(boolean b) {
        this.scenarioSelect = b;
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

        g.drawImage(frame, 0, 0, getWidth(),getHeight(),this);
        g.setColor(new Color(255, 255, 255, 70));
        g.fillRect(0,0, getWidth(), getHeight());

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if(scenarioSelect) {
            scenarioSelectMenu.reload();
            scenarioSelectMenu.paint(g2);
        } else {
            startMenu.paint(g2);
        }

    }

    @Override
    public void mouseScroll(MouseWheelEvent e, int x, int y) {
        menuList.forEach(p -> p.mouseScroll(e, x, y));
    }

    @Override
    public void mouseClick(MouseEvent e, int x, int y) {
        menuList.forEach(p -> p.mouseClick(e, x, y));
    }

    @Override
    public void mouseRelease(MouseEvent e, int x, int y) {
        menuList.forEach(p -> p.mouseRelease(e, x, y));
    }

    @Override
    public void mouseMove(MouseEvent e, int x, int y) {
        menuList.forEach(p -> p.mouseMove(e, x, y));
    }

    @Override
    public void keyPress(KeyEvent e) {
        menuList.forEach(p -> p.keyPress(e));
    }

    @Override
    public void keyRelease(KeyEvent e) {
        menuList.forEach(p -> p.keyRelease(e));
    }

}
