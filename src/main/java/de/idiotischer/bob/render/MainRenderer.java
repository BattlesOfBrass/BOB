package de.idiotischer.bob.render;

import de.idiotischer.bob.BOB;
import de.idiotischer.bob.camera.Camera;
import de.idiotischer.bob.render.menu.Panel;
import de.idiotischer.bob.render.menu.components.button.TroopVisualButton;
import de.idiotischer.bob.tile.Tile;
import de.idiotischer.bob.troop.Troop;
import de.idiotischer.bob.troop.TroopStack;
import de.idiotischer.bob.util.ImageUtil;
import it.unimi.dsi.fastutil.BigArrays;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

public class MainRenderer extends Thread {
    private boolean running = true;

    private BufferedImage logicMap;
    private BufferedImage renderMap;
    private Graphics2D renderGraphics;

    private RenderPanel renderPanel;
    private VolatileImage background;
    private BufferedImage visualBorderOverlay;

    private Point dragStart = null;
    private Point dragEnd = null;

    private boolean lastMenuTile = false;
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
    private DragOverlay overlay;
    private static final int DRAG_THRESHOLD = 6;
    private boolean mapDirty;
    private int dragButton;
    private final Set<Tile> draggedTiles = new LinkedHashSet<>();

    private boolean canMoveRegardless = true;

    public MainRenderer() {
        super("Battles of Brass");
    }

    @Override
    public void start() {
        inMenu = true;
        lastMenuTile = true;

        setMap(BOB.getInstance().getScenarioSceneLoader().getMap());

        renderPanel = new RenderPanel(getMap(), this);

        overlay = new DragOverlay(this);

        frame.setContentPane(renderPanel);

        frame.setGlassPane(overlay);
        overlay.setVisible(true);

        menuPanel = new MenuPanel(getMap(), this);

        panels.add(renderPanel);

        setMap(BOB.getInstance().getScenarioSceneLoader().getMap());

        root.add(menuPanel, MENU);
        root.add(renderPanel, GAME);

        frame.setContentPane(root);

        cardLayout.show(root, MENU);

        if (Taskbar.isTaskbarSupported()) {
            Taskbar taskbar = Taskbar.getTaskbar();
            if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
                taskbar.setIconImage(BOB.getInstance().createIcon().getImage());
            }
        }

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

    public void setDirty(boolean dirty) {
        this.mapDirty = dirty;
    }

    @Override
    public void run() {
        listen();

        long lastTime = System.nanoTime();

        while (running) {
            long now = System.nanoTime();
            double deltaTime = (now - lastTime) / 1_000_000_000.0;
            lastTime = now;

            if (inMenu != lastMenuTile) {
                if (inMenu) {
                    cardLayout.show(root, MENU);
                    menuPanel.requestFocusInWindow();
                    setDirty(true);
                } else {
                    setMap(BOB.getInstance().getScenarioSceneLoader().getMap());
                    camera.zoomToMin();
                    cardLayout.show(root, GAME);
                    renderPanel.requestFocusInWindow();
                    setDirty(true);
                }

                lastMenuTile = inMenu;
            }

            if (!inMenu) {
                if (!renderPanel.isPaused() || canMoveRegardless) handleMovement(deltaTime);

                if(isMapDirty()) renderPanel.repaint();
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
                                handleTileClick(e.getX(), e.getY(), x,y);
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
                        dragButton = e.getButton();
                    }

                    @Override
                    public void mouseReleased(MouseEvent e) {
                        if(e.getButton() == MouseEvent.BUTTON1 && !keysPressed.contains(KeyEvent.VK_SHIFT)) {
                            renderPanel.getTroopButtonGroup().clear();
                        }

                        if (dragStart != null) {
                            dragEnd = e.getPoint();

                            double dx = dragEnd.x - dragStart.x;
                            double dy = dragEnd.y - dragStart.y;

                            double distSq = dx * dx + dy * dy;

                            boolean shift = (e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) != 0;

                            if (distSq >= DRAG_THRESHOLD * DRAG_THRESHOLD) {
                                overlay.onDragRelease(shift);
                            }
                        }

                        dragStart = null;
                        dragEnd = null;
                        if (draggedTiles.size() > 1 && !renderPanel.isPaused() && getDragButton() == MouseEvent.BUTTON3) {
                            List<TroopStack> stacks = renderPanel.getTroopButtonGroup().stream().map(TroopVisualButton::getStack).toList();

                            final List<Tile>[] usableTiles = new List[]{new ArrayList<>(draggedTiles)};

                            stacks.forEach(t -> {
                                if(usableTiles[0].isEmpty()) usableTiles[0] = new ArrayList<>(draggedTiles);

                                BOB.getInstance().getTroopManager().move(t, usableTiles[0].removeLast());
                            });
                        }

                        dragButton = -1;
                        draggedTiles.clear();

                        renderPanel.repaint();
                    }
                });

                panel.addMouseMotionListener(new MouseAdapter() {
                    @Override
                    public void mouseDragged(MouseEvent e) {
                        dragEnd = e.getPoint();

                        int x = camera.screenToWorldX(e.getX());
                        int y = camera.screenToWorldY(e.getY());

                        if (x < 0 || y < 0 || x >= logicMap.getWidth() || y >= logicMap.getHeight()) {
                            return;
                        }

                        Tile tile = BOB.getInstance().getTileManager().getTileAt(x, y);

                        if (tile != null) {
                            draggedTiles.add(tile);
                        }
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
        double dx = 0;
        double dy = 0;

        double mapW = camera.getMapWidth();
        double mapH = camera.getMapHeight();

        double mapDiag = Math.sqrt(mapW * mapW + mapH * mapH);

        double defaultDiag = 2000.0;
        double defaultSpeed = 200.0;

        double speed = defaultSpeed * (mapDiag / defaultDiag);

        if (keysPressed.contains(KeyEvent.VK_SHIFT)) {
            speed *= 1.8;
        }

        speed *= camera.getZoom();

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

    //    de.idiotischer.bob.tile.Tile tile = BOB.getInstance().getTileManager().getTileAt(x, y);
    //    if (tile != null) System.out.println("clicked tile: " + tile.getName());

    //    FloodFill.fill(map, x, y, player.country().countryColor());
    //}

    private void handleTileClick(int xRaw, int yRaw,int x, int y) {
        Set<TroopVisualButton> troops  = new HashSet<>();

        if (!(renderPanel.getComponentAt(xRaw,yRaw) instanceof TroopVisualButton)) {
            troops.addAll(renderPanel.selected);
            renderPanel.selected.clear();
        }

        if (renderPanel.isPaused()) return;
        if (x < 0 || y < 0 || x >= logicMap.getWidth() || y >= logicMap.getHeight()) return;

        Color oldColor = new Color(logicMap.getRGB(x, y), true);
        if (oldColor.getAlpha() == 0) return;

        if (BOB.getInstance().getScenarioSceneLoader().getTakenColors().contains(oldColor)) return;

        Tile tile = BOB.getInstance().getTileManager().getTileAt(x,y);

        if(tile == null) {
            System.out.println("couldnt find tile at: " + x + ", " + y + " is it unimplemented???");
            return;
        }

        if(BOB.getInstance().getPlayer().country() == null) return;
        BOB.getInstance().getTroopManager().moveAll(troops.stream().map(TroopVisualButton::getStack).collect(Collectors.toSet()), tile);
        //troops.forEach(c -> {
        //    BOB.getInstance().getTroopManager().move(c.getStack(), tile);
        //    //renderPanel.selected.add(c); maybe

        //    if(BOB.getInstance().isDebug()) System.out.println("moved stacks to new loc: " + tile.getName());
        //});
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

        Tile tile = BOB.getInstance().getTileManager().getTileAt(x,y);

        if(tile == null) {
            renderPanel.getHud().visible(false);
            return;
        }

        if(renderPanel.getHud().getTile() == tile) {
            renderPanel.getHud().visible(false);
            return;
        }

        renderPanel.getHud().setTile(tile);
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
        this.logicMap = ImageUtil.deepCopy(map);

        GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
        this.renderMap = gc.createCompatibleImage(logicMap.getWidth(), logicMap.getHeight(), Transparency.TRANSLUCENT);

        Graphics2D g = renderMap.createGraphics();
        g.drawImage(logicMap, 0, 0, null);
        g.dispose();

        this.renderGraphics = renderMap.createGraphics();

        BufferedImage src = BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getBackgroundImage();

        int w = Math.min(map.getWidth(), src.getWidth());
        int h = Math.min(map.getHeight(), src.getHeight());

        this.background = ImageUtil.btv(src.getSubimage(0, 0, w, h));

        this.visualBorderOverlay = gc.createCompatibleImage(map.getWidth(), map.getHeight(), Transparency.TRANSLUCENT);

        if (camera == null) {
            camera = new Camera(map.getWidth(), map.getHeight());
        } else {
            camera.setMapSize(map.getWidth(), map.getHeight());
        }

        if (renderPanel != null) {
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

    public int getDragButton() {
        return dragButton;
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

    public VolatileImage getBackground() {
        return background;
    }

    public boolean isMapDirty() {
        return mapDirty;
    }

    public Set<Tile> getDraggedTiles() {
        return draggedTiles;
    }

    public void setCanMoveRegardless(boolean canMoveRegardless) {
        this.canMoveRegardless = canMoveRegardless;
    }

    public boolean canMoveRegardless() {
        return canMoveRegardless;
    }
}