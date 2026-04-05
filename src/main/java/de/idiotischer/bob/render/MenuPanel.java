package de.idiotischer.bob.render;

import de.idiotischer.bob.BOB;
import de.idiotischer.bob.render.menu.Panel;
import de.idiotischer.bob.render.menu.impl.select.ScenarioSelectMenu;
import de.idiotischer.bob.render.menu.impl.StartMenu;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

public class MenuPanel extends JPanel implements Panel {

    private BufferedImage frame;
    private final MainRenderer renderer;

    private final StartMenu startMenu;
    private JPanel currentActiveMenu;

    private boolean scenarioSelect = false;

    public MenuPanel(BufferedImage map, MainRenderer renderer) {
        setFrame(map);
        this.renderer = renderer;

        this.setLayout(new GridBagLayout());

        this.startMenu = new StartMenu();
        this.currentActiveMenu = new ScenarioSelectMenu(BOB.getInstance().getScenarioSceneLoader().getCurrentScenario());

        this.add(startMenu);
        this.add(currentActiveMenu);

        this.setBackground(Color.BLACK);
        this.setFocusable(true);
        this.requestFocusInWindow();
        this.setPreferredSize(new Dimension(frame.getWidth(), frame.getHeight()));

        setInScenarioSelect(false);
    }


    public void setScenarioSelectMenu(JPanel newMenu) {
        if (this.currentActiveMenu != null) {
            this.remove(this.currentActiveMenu);
        }

        this.currentActiveMenu = newMenu;
        this.add(currentActiveMenu);

        this.currentActiveMenu.setVisible(scenarioSelect);

        this.revalidate();
        //this.repaint();
    }

    public void setInScenarioSelect(boolean b) {
        this.scenarioSelect = b;

        this.startMenu.setVisible(!b);
        if (currentActiveMenu != null) {
            currentActiveMenu.setVisible(b);
        }

        if (b) {
            this.requestFocusInWindow();
        }

        this.revalidate();
        //this.repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        g.drawImage(frame, 0, 0, getWidth(), getHeight(), this);

        g.setColor(new Color(255, 255, 255, 70));
        g.fillRect(0, 0, getWidth(), getHeight());

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }

    @Override public void mouseScroll(MouseWheelEvent e, int x, int y) {}
    @Override public void mouseClick(MouseEvent e, int x, int y) {}
    @Override public void mouseRelease(MouseEvent e, int x, int y) {}
    @Override public void mouseMove(MouseEvent e, int x, int y) {}

    @Override
    public void keyPress(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE && scenarioSelect) {
            setInScenarioSelect(false);
        }
    }

    @Override public void keyRelease(KeyEvent e) {}

    public void setFrame(BufferedImage frame) { this.frame = frame; }
    public void setScenarioSelect(boolean scenarioSelect) { setInScenarioSelect(scenarioSelect); }
}