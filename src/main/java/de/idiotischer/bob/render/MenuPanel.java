package de.idiotischer.bob.render;

import de.idiotischer.bob.BOB;
import de.idiotischer.bob.render.menu.Panel;
import de.idiotischer.bob.render.menu.impl.MultiplayerMenu;
import de.idiotischer.bob.render.menu.impl.SettingsMenu;
import de.idiotischer.bob.render.menu.impl.select.ScenarioSelectMenu;
import de.idiotischer.bob.render.menu.impl.StartMenu;
import de.idiotischer.bob.util.ImageUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;

public class MenuPanel extends JPanel implements Panel {

    private final SettingsMenu stMenu;
    private final MultiplayerMenu mpMenu;
    private final StartMenu startMenu;

    private JPanel scenarioMenu;

    private final CardLayout layout;
    private MenuTile currentTile;

    private final MainRenderer renderer;

    private VolatileImage scaledBackground;
    private BufferedImage scaledOverlay;

    private int cachedW = -1;
    private int cachedH = -1;

    public MenuPanel(BufferedImage map, MainRenderer renderer) {
        this.renderer = renderer;

        this.layout = new CardLayout();
        this.setLayout(layout);

        this.stMenu = new SettingsMenu();
        this.mpMenu = new MultiplayerMenu();
        this.startMenu = new StartMenu();

        this.scenarioMenu = wrap(new ScenarioSelectMenu(BOB.getInstance().getScenarioSceneLoader().getCurrentScenario()));

        this.add(wrap(stMenu), "SETTINGS");
        this.add(wrap(startMenu), "START");
        this.add(scenarioMenu, "SCENARIO");
        this.add(wrap(mpMenu), "MP");

        setBackground(Color.BLACK);
        setFocusable(true);
        requestFocusInWindow();

        this.currentTile = MenuTile.START;
        updateMenuVisibility();

        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "escape");

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
        MULTIPLAYER,
        SETTINGS
    }

    private void updateMenuVisibility() {
        switch (currentTile) {
            case START -> layout.show(this, "START");
            case SCENARIO_SELECT -> layout.show(this, "SCENARIO");
            case MULTIPLAYER -> layout.show(this, "MP");
            case SETTINGS -> layout.show(this, "SETTINGS");
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


    public void setSettingsMenu(boolean b) {
        currentTile = b ? MenuTile.SETTINGS : MenuTile.START;
        updateMenuVisibility();
    }

    public BufferedImage getFrame() {
        return renderer.getLogicMap();
    }

    private void updateCachedImages(int w, int h) {
        if (w <= 0 || h <= 0) return;

        if (w == cachedW && h == cachedH && scaledBackground != null) return;

        cachedW = w;
        cachedH = h;

        scaledBackground = scale(renderer.getBackground(), w, h);
        scaledOverlay = scale(renderer.getVisualBorderOverlay(), w, h);
    }

    private BufferedImage scale(BufferedImage src, int w, int h) {
        if (src == null) return null;

        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = out.createGraphics();

        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        g2.drawImage(src, 0, 0, w, h, null);
        g2.dispose();

        return out;
    }

    private VolatileImage scale(VolatileImage src, int w, int h) {
        if (src == null || w <= 0 || h <= 0) return null;

        GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
        VolatileImage out = gc.createCompatibleVolatileImage(w, h, Transparency.TRANSLUCENT);

        do {
            int status = src.validate(gc);

            if (status == VolatileImage.IMAGE_INCOMPATIBLE) return null;


            Graphics2D g2 = out.createGraphics();

            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            g2.drawImage(src, 0, 0, w, h, null);
            g2.dispose();

        } while (out.contentsLost());

        return out;
    }

    private BufferedImage cachedLowMap = null;
    private VolatileImage cachedLowBackground = null;
    private BufferedImage cachedLowOverlay = null;

    private BufferedImage lastKnownMapRef = null;
    private VolatileImage lastKnownBgRef = null;
    private BufferedImage lastOverlayRef = null;

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        BufferedImage currentMap = renderer.getMap();
        if (currentMap == null) return;

        Graphics2D g2 = (Graphics2D) g;

        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);

        AffineTransform screenTransform = g2.getTransform();

        AffineTransform cameraTransform = renderer.getCamera().getTransform();
        double scaleX = cameraTransform.getScaleX();
        g2.transform(cameraTransform);

        Rectangle visible = renderer.getCamera().getVisibleWorldBounds(getWidth(), getHeight());

        boolean useLow = (scaleX <= 0.25);

        VolatileImage currentBg = BOB.getInstance().getMainRenderer().getBackground();
        BufferedImage currentOverlay = renderer.getVisualBorderOverlay();

        boolean mapChanged = (currentMap != lastKnownMapRef);

        if (useLow && (cachedLowMap == null || mapChanged)) {
            GraphicsConfiguration gfxConfig = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();

            cachedLowMap = ImageUtil.createLowMipmap(currentMap, gfxConfig);
            lastKnownMapRef = currentMap;

            if (currentBg != null) {
                cachedLowBackground = ImageUtil.createLowMipmap(currentBg, gfxConfig);
                lastKnownBgRef = currentBg;
            }
            if (currentOverlay != null) {
                cachedLowOverlay = ImageUtil.createLowMipmap(currentOverlay, gfxConfig);
                lastOverlayRef = currentOverlay;
            }
        }

        if (useLow && currentBg != null && currentBg != lastKnownBgRef) {
            GraphicsConfiguration gfxConfig = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
            cachedLowBackground = ImageUtil.createLowMipmap(currentBg, gfxConfig);
            lastKnownBgRef = currentBg;
        }
        if (useLow && currentOverlay != null && currentOverlay != lastOverlayRef) {
            GraphicsConfiguration gfxConfig = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
            cachedLowOverlay = ImageUtil.createLowMipmap(currentOverlay, gfxConfig);
            lastOverlayRef = currentOverlay;
        }

        int x2 = visible.x + visible.width;
        int y2 = visible.y + visible.height;

        if (currentBg != null) {
            VolatileImage bgToDraw = useLow ? cachedLowBackground : currentBg;
            g2.drawImage(bgToDraw, visible.x, visible.y, x2, y2, visible.x, visible.y, x2, y2, null);
        }

        BufferedImage mapToDraw = useLow ? cachedLowMap : currentMap;
        g2.drawImage(mapToDraw, visible.x, visible.y, x2, y2, visible.x, visible.y, x2, y2, null);

        if (currentOverlay != null) {
            BufferedImage overlayToDraw = useLow ? cachedLowOverlay : currentOverlay;
            g2.drawImage(overlayToDraw, visible.x, visible.y, x2, y2, visible.x, visible.y, x2, y2, null);
        }

        g2.setTransform(screenTransform);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }

    public void invalidateMapCache() {
        this.cachedLowMap = null;
        this.cachedLowBackground = null;
        this.cachedLowOverlay = null;
    }
}