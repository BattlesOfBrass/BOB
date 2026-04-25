package de.idiotischer.bob.render;

import de.idiotischer.bob.BOB;
import de.idiotischer.bob.camera.Camera;
import de.idiotischer.bob.map.FloodFill;
import de.idiotischer.bob.player.Player;
import de.idiotischer.bob.render.menu.Panel;
import de.idiotischer.bob.state.StateManager;
import de.idiotischer.bob.util.PosUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

import static de.idiotischer.bob.util.ImageUtil.deepCopy;

public class MainRenderer extends Thread {

    private final Player player;
    private boolean running = true;

    private BufferedImage logicMap;
    private BufferedImage renderMap;
    private Graphics2D renderGraphics;

    private RenderPanel renderPanel;
    private BufferedImage background;
    private BufferedImage visualBorderOverlay;

    private Point dragStart = null;
    private Point dragEnd = null;

    private boolean lastMenuState = false;
    private boolean inMenu = false;
    private final JFrame frame = new JFrame("Battles of Brass");
    private final Set<Integer> keysPressed = new HashSet<>();
    private MenuPanel menuPanel;

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel root = new JPanel(cardLayout);

    private static final String MENU = "MENU";
    private static final String GAME = "GAME";

    private final List<Panel> panels = new ArrayList<>();
    private Camera camera;

    public MainRenderer(Player player) {
        this.player = player;
    }

    @Override
    public void start() {
        inMenu = true;
        lastMenuState = true;

        setMap(BOB.getInstance().getScenarioSceneLoader().getMap());

        renderPanel = new RenderPanel(getMap(), this);
        menuPanel = new MenuPanel(getMap(), this);

        panels.add(renderPanel);

        setMap(BOB.getInstance().getScenarioSceneLoader().getMap());

        root.add(menuPanel, MENU);
        root.add(renderPanel, GAME);

        frame.setContentPane(root);

        cardLayout.show(root, MENU);

        frame.setIconImage(BOB.getInstance().createIcon().getImage());
        frame.setBackground(Color.BLACK);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        frame.setExtendedState(Frame.MAXIMIZED_BOTH);

        camera.setViewportSize(renderPanel.getWidth(), renderPanel.getHeight());

        frame.addComponentListener(new FrameListen() {
            @Override
            public void componentResized(ComponentEvent e) {
                SwingUtilities.invokeLater(() -> {
                    camera.setViewportSize(renderPanel.getWidth(), renderPanel.getHeight());
                    camera.clamp();
                });
            }
        });

        super.start();
    }

    @Override
    public void run() {
        listen();

        long lastTime = System.nanoTime();

        while (running) {
            long now = System.nanoTime();
            double deltaTime = (now - lastTime) / 1_000_000_000.0;
            lastTime = now;

            if (inMenu != lastMenuState) {
                if (inMenu) {
                    cardLayout.show(root, MENU);
                    menuPanel.requestFocusInWindow();
                } else {
                    setMap(BOB.getInstance().getScenarioSceneLoader().getMap());
                    camera.zoomToMin();
                    cardLayout.show(root, GAME);
                    renderPanel.requestFocusInWindow();
                }

                lastMenuState = inMenu;
            }

            if (!inMenu) {
                if (!renderPanel.isPaused()) handleMovement(deltaTime);

                renderPanel.repaint();
            } else {
                renderMenu();
                menuPanel.repaint();
            }

            try {
                Thread.sleep(16);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void listen() {

        for(Panel p : panels) {
            if(p instanceof JPanel panel) {
                panel.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        int button = e.getButton();

                        //TODO: fix wrong x and y when maximizing or sqashing tab
                        int x = camera.screenToWorldX(e.getX());
                        int y = camera.screenToWorldY(e.getY());

                        //int sx = e.getX();
                        //int sy = e.getY();

                        //int wx = camera.screenToWorldX(sx);
                        //int wy = camera.screenToWorldY(sy);

                        //int sx2 = (int) (wx * camera.getZoom() - camera.getX());
                        //int sy2 = (int) (wy * camera.getZoom() - camera.getY());

                        //System.out.println("screen: " + sx + "," + sy);
                        //System.out.println("world: " + wx + "," + wy);
                        //System.out.println("back:  " + sx2 + "," + sy2);

                        if(panel instanceof RenderPanel panel1) {
                            if (panel1.isEscMenu()) {
                                handleMenuClick(e, x, y);
                                return;
                            }

                            if(button == MouseEvent.BUTTON1){
                                handleCountryMenu(x,y);
                                return;
                            }

                            if(button == MouseEvent.BUTTON3) {
                                handleTileClick(x,y);
                            }

                        } else if(inMenu) handleMenuClick(e, x, y);

                    }

                    @Override
                    public void mouseReleased(MouseEvent e) {
                        int x = camera.screenToWorldX(e.getX());
                        int y = camera.screenToWorldY(e.getY());

                        if(panel instanceof RenderPanel panel1) {
                            if (panel1.isEscMenu()) handleMenuRelease(e, x, y);
                        } else if(inMenu) handleMenuRelease(e, x, y);
                    }
                });

                panel.addMouseWheelListener(new MouseAdapter() {
                    @Override
                    public void mouseWheelMoved(MouseWheelEvent e) {
                        if (inMenu && getGamePanel().isEscMenu() /*gucken ob probleme macht*/) {

                            p.mouseScroll(e, e.getX(), e.getY());
                        } else {
                            if (renderPanel.isEscMenu()) return;
                            double factor = Math.pow(1.1, -e.getWheelRotation());
                            camera.zoom(factor, e.getX(), e.getY());

                            int x = camera.screenToWorldX(e.getX());
                            int y = camera.screenToWorldY(e.getY());
                            p.mouseScroll(e, x, y);
                        }
                    }
                });

                panel.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        dragStart = e.getPoint();
                        dragEnd = dragStart;
                    }

                    @Override
                    public void mouseReleased(MouseEvent e) {
                        //dragEnd = e.getPoint();
                        //renderPanel.repaint();

                        dragStart = null;
                        dragEnd = null;
                    }
                });

                panel.addMouseMotionListener(new MouseAdapter() {
                    @Override
                    public void mouseDragged(MouseEvent e) {
                        dragEnd = e.getPoint();

                        //panel.repaint();
                    }

                    @Override
                    public void mouseMoved(MouseEvent e) {
                        int x = camera.screenToWorldX(e.getX());
                        int y = camera.screenToWorldY(e.getY());

                        if(panel instanceof RenderPanel panel1) {
                            if (panel1.isEscMenu()) handleMenuMove(e, x, y);
                        } else if(inMenu) handleMenuMove(e, x, y);
                    }
                });

                panel.addKeyListener(new KeyAdapter() {
                    @Override
                    public void keyPressed(KeyEvent e) {
                        keysPressed.add(e.getKeyCode());

                        if(panel instanceof RenderPanel panel1) {
                            if (keysPressed.contains(KeyEvent.VK_ESCAPE)) {
                                panel1.setEscMenu(!panel1.isEscMenu());
                            }
                        }

                        handleKeyPress(e);
                    }

                    @Override
                    public void keyReleased(KeyEvent e) {
                        keysPressed.remove(e.getKeyCode());

                        handleKeyRelease(e);
                    }
                });
            }
        }
    }


    private void handleMovement(double deltaTime) {
        double speed = 100;

        double dx = 0;
        double dy = 0;

        if (keysPressed.contains(KeyEvent.VK_SHIFT)) speed += 100;

        speed *= camera.getZoom() / 0.97;

        if (keysPressed.contains(KeyEvent.VK_W)) dy -= speed * deltaTime;
        if (keysPressed.contains(KeyEvent.VK_S)) dy += speed * deltaTime;
        if (keysPressed.contains(KeyEvent.VK_A)) dx -= speed * deltaTime;
        if (keysPressed.contains(KeyEvent.VK_D)) dx += speed * deltaTime;

        camera.move(dx, dy, false);
        camera.clamp();
    }

    //private void handleTileClick(int x, int y) {
    //    if (renderPanel.isPaused()) return;
    //    if (x < 0 || y < 0 || x >= map.getWidth() || y >= map.getHeight()) return;

    //    Color oldColor = new Color(map.getRGB(x, y), true);
    //    if (BOB.getInstance().getScenarioSceneLoader().getTakenColors().contains(oldColor)) return;

    //    de.idiotischer.bob.state.State state = BOB.getInstance().getStateManager().getStateAt(x, y);
    //    if (state != null) System.out.println("clicked state: " + state.getName());

    //    FloodFill.fill(map, x, y, player.country().countryColor());
    //}

    private void handleTileClick(int x, int y) {
        if (renderPanel.isPaused()) return;
        if (x < 0 || y < 0 || x >= logicMap.getWidth() || y >= logicMap.getHeight()) return;

        Color oldColor = new Color(logicMap.getRGB(x, y), true);
        if (oldColor.getAlpha() == 0) return;

        if (BOB.getInstance().getScenarioSceneLoader().getTakenColors().contains(oldColor)) return;

        de.idiotischer.bob.state.State state = BOB.getInstance().getStateManager().getStateAt(x,y);

        if(state == null) {
            System.out.println("couldnt find state at: " + x + ", " + y + " is it unimplemented???");
            return;
        }

        SwingUtilities.invokeLater(() -> {
            //FloodFill.fillAll(logicMap, state.getPoints(), player.country().countryColor());
            BOB.getInstance().getStateManager().recolorState(state, player.country().countryColor());
        });

        //syncBuffers();

        renderPanel.repaint();
    }

    private void handleCountryMenu(int x, int y) {
        if (renderPanel.isPaused()) {
            renderPanel.getHud().visible(false);
            return;
        }
        if (x < 0 || y < 0 || x >= logicMap.getWidth() || y >= logicMap.getHeight()) {
            renderPanel.getHud().visible(false);
            return;
        }

        Color oldColor = new Color(logicMap.getRGB(x, y), true);
        if (oldColor.getAlpha() == 0) {
            renderPanel.getHud().visible(false);
            return;
        }

        if (BOB.getInstance().getScenarioSceneLoader().getTakenColors().contains(oldColor)) {
            renderPanel.getHud().visible(false);
            return;
        }

        de.idiotischer.bob.state.State state = BOB.getInstance().getStateManager().getStateAt(x,y);

        if(state == null) {
            renderPanel.getHud().visible(false);
            return;
        }

        if(renderPanel.getHud().getState() == state) {
            renderPanel.getHud().visible(false);
            return;
        }

        renderPanel.getHud().setState(state);
        renderPanel.getHud().visible(true);

        //Color oldColor = new Color(logicMap.getRGB(x, y), true);
        //if (BOB.getInstance().getScenarioSceneLoader().getTakenColors().contains(oldColor)) return;

        //System.out.println("Open country menu at " + x + "," + y);
    }

    private void handleMenuClick(MouseEvent e, int x, int y) {
        panels.forEach(p1 -> { if (p1 instanceof JPanel p && p.isVisible()) p1.mouseClick(e, x, y); });
    }

    private void handleMenuRelease(MouseEvent e, int x, int y) {
        panels.forEach(p1 -> { if (p1 instanceof JPanel p && p.isVisible()) p1.mouseRelease(e, x, y); });
    }

    private void handleMenuMove(MouseEvent e, int x, int y) {
        panels.forEach(p1 -> { if (p1 instanceof JPanel p && p.isVisible()) p1.mouseMove(e, x, y); });
    }

    private void handleKeyPress(KeyEvent e) {
        panels.forEach(p1 -> { if (p1 instanceof JPanel p && p.isVisible()) p1.keyPress(e); });
    }

    private void handleKeyRelease(KeyEvent e) {
        panels.forEach(p1 -> { if (p1 instanceof JPanel p && p.isVisible()) p1.keyRelease(e); });
    }

    private BufferedImage renderMap(BufferedImage map) {
        return map;
    }

    private void renderMenu() {
    }

    public Camera getCamera() { return camera; }
    public Set<Integer> getKeysPressed() { return keysPressed; }
    public BufferedImage getMap() { return renderMap; }

    public void setMap(BufferedImage map) {
        this.logicMap = deepCopy(map);

        this.renderMap = new BufferedImage(
                logicMap.getWidth(),
                logicMap.getHeight(),
                BufferedImage.TYPE_INT_ARGB
        );

        this.renderGraphics = renderMap.createGraphics();

        this.background = BOB.getInstance().getScenarioSceneLoader()
                .getCurrentScenario().getBackgroundImage()
                .getSubimage(0, 0, map.getWidth(), map.getHeight());

        this.visualBorderOverlay = new BufferedImage(map.getWidth(), map.getHeight(), BufferedImage.TYPE_INT_ARGB);

        if (camera == null) camera = new Camera(map.getWidth(), map.getHeight());
        if (renderPanel != null){
            camera.setViewportSize(renderPanel.getWidth(), renderPanel.getHeight());
        }

        syncBuffers();
    }

    public void syncBuffers() {
        if (renderGraphics != null && logicMap != null) {
            renderGraphics.drawImage(logicMap, 0, 0, null);
        }
    }

    public MenuPanel getMenuPanel() { return menuPanel; }
    public Point getDragStart() { return dragStart; }
    public Point getDragEnd() { return dragEnd; }
    public JFrame getFrame() { return frame; }

    public void shutdown() {
        if (!BOB.getInstance().save()) System.out.println("Failed to save before shutdown...");
        running = false;
        System.exit(0);
    }

    public static abstract class FrameListen implements ComponentListener {
        public void componentHidden(ComponentEvent arg0) {}
        public void componentMoved(ComponentEvent arg0) {}
        public abstract void componentResized(ComponentEvent arg0);
        public void componentShown(ComponentEvent arg0) {}
    }

    public BufferedImage getVisualBorderOverlay() {
        return visualBorderOverlay;
    }

    //TODO: so boolean zeug bulletproof machen sonst desync bugs
    public void setMainMenu(boolean b) {
        this.inMenu = b;
    }

    public RenderPanel getGamePanel() {
        return renderPanel;
    }

    public BufferedImage getLogicMap() {
        return logicMap;
    }

    public BufferedImage getBackground() {
        return background;
    }
}