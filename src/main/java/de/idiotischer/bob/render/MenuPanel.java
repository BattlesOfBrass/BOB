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
    private MenuTile currentTile;

    // cache renderer (avoid repeated singleton calls)
    private final MainRenderer renderer;

    // cached scaled images
    private BufferedImage scaledBackground;
    private BufferedImage scaledOverlay;

    private int cachedW = -1;
    private int cachedH = -1;

    public MenuPanel(BufferedImage map, MainRenderer renderer) {
        this.renderer = renderer;

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

        setBackground(Color.BLACK);
        setFocusable(true);
        requestFocusInWindow();

        this.currentTile = MenuTile.START;
        updateMenuVisibility();

        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                "escape"
        );

        getActionMap().put("escape", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                currentTile = MenuTile.START;
                updateMenuVisibility();
            }
        });
    }

    private JPanel wrap(JPanel panel) {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setOpaque(false);
        wrapper.add(panel);
        return wrapper;
    }

    public enum MenuTile {
        START,
        SCENARIO_SELECT,
        MULTIPLAYER
    }

    private void updateMenuVisibility() {
        switch (currentTile) {
            case START -> layout.show(this, "START");
            case SCENARIO_SELECT -> layout.show(this, "SCENARIO");
            case MULTIPLAYER -> layout.show(this, "MP");
        }

        revalidate();
        repaint();
    }

    public void setScenarioSelectMenu(JPanel newMenu) {
        this.remove(scenarioMenu);
        this.scenarioMenu = wrap(newMenu);

        this.add(scenarioMenu, "SCENARIO");
        updateMenuVisibility();
    }

    public void setInScenarioSelect(boolean b) {
        currentTile = b ? MenuTile.SCENARIO_SELECT : MenuTile.START;
        updateMenuVisibility();
    }

    public void setInMultiplayerMenu(boolean b) {
        currentTile = b ? MenuTile.MULTIPLAYER : MenuTile.START;
        updateMenuVisibility();
    }

    public BufferedImage getFrame() {
        return renderer.getLogicMap();
    }

    // ---- PERFORMANCE: scale images only when needed ----

    private void updateCachedImages(int w, int h) {
        if (w <= 0 || h <= 0) return;

        if (w == cachedW && h == cachedH && scaledBackground != null) {
            return;
        }

        cachedW = w;
        cachedH = h;

        scaledBackground = scale(renderer.getBackground(), w, h);
        scaledOverlay = scale(renderer.getVisualBorderOverlay(), w, h);
    }

    private BufferedImage scale(BufferedImage src, int w, int h) {
        if (src == null) return null;

        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = out.createGraphics();

        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        g2.drawImage(src, 0, 0, w, h, null);
        g2.dispose();

        return out;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        int w = getWidth();
        int h = getHeight();

        updateCachedImages(w, h);

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (scaledBackground != null) g2.drawImage(scaledBackground, 0, 0, null);

        BufferedImage frame = getFrame();
        if (frame != null) g2.drawImage(frame, 0, 0, w, h, null);


        if (scaledOverlay != null) g2.drawImage(scaledOverlay, 0, 0, null);

        g2.setColor(new Color(255, 255, 255, 70));
        g2.fillRect(0, 0, w, h);
    }
}