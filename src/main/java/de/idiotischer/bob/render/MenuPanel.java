package de.idiotischer.bob.render;

import de.idiotischer.bob.BOB;
import de.idiotischer.bob.render.menu.Panel;
import de.idiotischer.bob.render.menu.impl.MultiplayerMenu;
import de.idiotischer.bob.render.menu.impl.select.ScenarioSelectMenu;
import de.idiotischer.bob.render.menu.impl.StartMenu;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

public class MenuPanel extends JPanel implements Panel {

    private final MultiplayerMenu mpMenu;
    private final StartMenu startMenu;
    private JPanel scenarioMenu;

    private final CardLayout layout;
    private MenuState currentState;

    public MenuPanel(BufferedImage map, MainRenderer renderer) {
        this.layout = new CardLayout();
        this.setLayout(layout);

        this.mpMenu = new MultiplayerMenu();
        this.startMenu = new StartMenu();
        this.scenarioMenu = wrap(new ScenarioSelectMenu(
                BOB.getInstance().getScenarioSceneLoader().getCurrentScenario()
        ));

        this.add(wrap(startMenu), "START");
        this.add(scenarioMenu, "SCENARIO");
        this.add(wrap(mpMenu), "MP");

        this.setBackground(Color.BLACK);
        this.setFocusable(true);
        this.requestFocusInWindow();

        this.currentState = MenuState.START;
        updateMenuVisibility();
    }

    private JPanel wrap(JPanel panel) {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setOpaque(false);
        wrapper.add(panel);
        return wrapper;
    }

    public enum MenuState {
        START,
        SCENARIO_SELECT,
        MULTIPLAYER
    }

    private void updateMenuVisibility() {
        switch (currentState) {
            case START -> layout.show(this, "START");
            case SCENARIO_SELECT -> layout.show(this, "SCENARIO");
            case MULTIPLAYER -> layout.show(this, "MP");
        }

        revalidate();
        repaint();

        doLayout();
    }

    public void setScenarioSelectMenu(JPanel newMenu) {
        this.remove(scenarioMenu);
        this.scenarioMenu = wrap(newMenu);

        this.add(scenarioMenu, "SCENARIO");
        updateMenuVisibility();
    }

    public void setInScenarioSelect(boolean b) {
        currentState = b ? MenuState.SCENARIO_SELECT : MenuState.START;
        updateMenuVisibility();
    }

    public void setInMultiplayerMenu(boolean b) {
        currentState = b ? MenuState.MULTIPLAYER : MenuState.START;
        updateMenuVisibility();
    }

    public BufferedImage getFrame() {
        return BOB.getInstance().getMainRenderer().getLogicMap();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        g.drawImage(BOB.getInstance().getMainRenderer().getBackground(), 0, 0, getWidth(), getHeight(), this);
        g.drawImage(getFrame(), 0, 0, getWidth(), getHeight(), this);
        g.drawImage(BOB.getInstance().getMainRenderer().getVisualBorderOverlay(), 0, 0, getWidth(), getHeight(), this);

        g.setColor(new Color(255, 255, 255, 70));
        g.fillRect(0, 0, getWidth(), getHeight());

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }

    @Override
    protected void processKeyEvent(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            currentState = MenuState.START;
            updateMenuVisibility();
        }
    }
}