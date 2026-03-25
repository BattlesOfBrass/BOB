package de.idiotischer.bob.render;

import de.idiotischer.bob.BOB;
import de.idiotischer.bob.map.FloodFill;
import de.idiotischer.bob.player.Player;
import de.idiotischer.bob.render.menu.Panel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainRenderer extends Thread {

    private final Player player;
    private boolean running = true;

    private RenderPanel renderPanel;
    private BufferedImage map;
    private BufferedImage visualBorderOverlay;
    private BufferedImage icon;

    private Point dragStart = null;
    private Point dragEnd = null;

    private double offsetX = 0;
    private double offsetY = 0;

    private boolean lastMenuState = false;
    private boolean inMenu = false;

    private double zoom = 1.0;
    private final JFrame frame = new JFrame("Battles of Brass");

    private final Set<Integer> keysPressed = new HashSet<>();
    private MenuPanel menuPanel;

    List<Panel> panels = new ArrayList<>();
    private boolean wasAtMinZoom;

    public MainRenderer(Player player) {
        this.player = player;
    }

    @Override
    public void start() {
        inMenu = true;

        map = BOB.getInstance().getScenarioSceneLoader().getMap();
        visualBorderOverlay = new BufferedImage(map.getWidth(), map.getHeight(), BufferedImage.TYPE_INT_ARGB);

        renderPanel = new RenderPanel(map,this);
        menuPanel = new MenuPanel(map,this);

        panels.add(renderPanel);
        panels.add(menuPanel);

        frame.setIconImage(BOB.getInstance().createIcon().getImage());
        frame.setBackground(Color.BLACK);
        frame.add(inMenu ? menuPanel : renderPanel);
        frame.pack();
        //frame.setLocationRelativeTo(renderPanel);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        //frame.setIconImage(icon);
        frame.setIgnoreRepaint(true); // TODO: check if it causes bugs or fixes window flicker on windows

        frame.addComponentListener(new FrameListen() {
            @Override
            public void componentResized(ComponentEvent e) {
                SwingUtilities.invokeLater(() -> {
                    double newMinZoom = getMinZoom();

                    if (wasAtMinZoom) {
                        zoom = newMinZoom;

                        offsetX = 0;
                        offsetY = 0;
                    } else {
                        zoom = Math.max(newMinZoom, zoom);
                    }

                    clampOffsets();
                    //renderPanel.repaint(); makes everything bug when going back to smaller window
                });
            }
        });

        super.start();

        frame.setExtendedState(Frame.MAXIMIZED_BOTH);
    }

    @Override
    public void run() {
        listen();

        while (running) {

            if (inMenu != lastMenuState) {
                frame.getContentPane().removeAll();

                if (inMenu) {
                    menuPanel.setVisible(true);
                    renderPanel.setVisible(false);

                    frame.add(menuPanel);

                    menuPanel.requestFocusInWindow();
                } else {
                    menuPanel.setVisible(false);
                    renderPanel.setVisible(true);

                    frame.add(renderPanel);

                    renderPanel.requestFocusInWindow();
                    zoom = getMinZoom();
                    clampOffsets();
                }

                frame.revalidate();
                frame.repaint();

                lastMenuState = inMenu;
            }

            if (!inMenu) {
                if(!renderPanel.isPaused()) handleMovement();
                renderPanel.setFrame(renderMap(map));
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
                        int x = (int) ((e.getX() + offsetX) / zoom);
                        int y = (int) ((e.getY() + offsetY) / zoom);

                        if(panel instanceof RenderPanel panel1) {
                            if (panel1.isEscMenu()) {
                                handleMenuClick(e, x, y);
                                return;
                            }

                            if(button == MouseEvent.BUTTON3){
                                handleCountryMenu(x,y);
                                return;
                            }

                            if(button == MouseEvent.BUTTON1) {
                                handleTileClick(x,y);
                            }

                        } else if(inMenu) handleMenuClick(e, x, y);

                    }

                    @Override
                    public void mouseReleased(MouseEvent e) {
                        int x = (int) ((e.getX() + offsetX) / zoom);
                        int y = (int) ((e.getY() + offsetY) / zoom);

                        if(panel instanceof RenderPanel panel1) {
                            if (panel1.isEscMenu()) handleMenuRelease(e, x, y);
                        } else if(inMenu) handleMenuRelease(e, x, y);
                    }
                });

                panel.addMouseWheelListener(new MouseAdapter() {
                    @Override
                    public void mouseWheelMoved(MouseWheelEvent e) {
                        if(renderPanel.isEscMenu()) return;

                        if (e.getScrollType() != MouseWheelEvent.WHEEL_UNIT_SCROLL) return;

                        double oldZoom = zoom;

                        int rotation = e.getWheelRotation();
                        zoom *= Math.pow(1.1, -rotation);

                        double minZoom = getMinZoom();

                        wasAtMinZoom = Math.abs(zoom - minZoom) < 0.0001;

                        zoom = Math.max(minZoom, Math.min(zoom, 20));

                        double mouseX = e.getX();
                        double mouseY = e.getY();

                        offsetX = (offsetX + mouseX) * (zoom / oldZoom) - mouseX;
                        offsetY = (offsetY + mouseY) * (zoom / oldZoom) - mouseY;

                        int x = (int) ((e.getX() + offsetX) / zoom);
                        int y = (int) ((e.getY() + offsetY) / zoom);

                        p.mouseScroll(e, x, y);
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
                        dragEnd = e.getPoint();
                        renderPanel.repaint();

                        dragStart = null;
                        dragEnd = null;
                    }
                });

                panel.addMouseMotionListener(new MouseAdapter() {
                    @Override
                    public void mouseDragged(MouseEvent e) {
                        dragEnd = e.getPoint();

                        panel.repaint();
                    }

                    @Override
                    public void mouseMoved(MouseEvent e) {
                        int x = (int) ((e.getX() + offsetX) / zoom);
                        int y = (int) ((e.getY() + offsetY) / zoom);

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

    private void handleMenuClick(MouseEvent e, int x, int y) {

        panels.forEach(p1 -> {
            if(p1 instanceof JPanel p)
                if(p.isVisible()){
                    p1.mouseClick(e, x, y);
                }

        });
    }

    private void handleKeyRelease(KeyEvent e) {

        panels.forEach(p1 -> {
            if(p1 instanceof JPanel p)
                if(p.isVisible()){
                    p1.keyRelease(e);
                }

        });
    }

    private void handleKeyPress(KeyEvent e) {

        panels.forEach(p1 -> {
            if(p1 instanceof JPanel p)
                if(p.isVisible()){
                    p1.keyPress(e);
                }

        });
    }

    private void handleMenuRelease(MouseEvent e, int x, int y) {

        panels.forEach(p1 -> {
            if(p1 instanceof JPanel p) if(p.isVisible()) p1.mouseRelease(e, x, y);
        });
    }

    private void handleMenuMove(MouseEvent e, int x, int y) {
        panels.forEach(p1 -> {
            if(p1 instanceof JPanel p) if(p.isVisible()) p1.mouseMove(e, x, y);
        });
    }

    private void handleCountryMenu(int x, int y) {

        if(renderPanel.isPaused()) return;

        Color oldColor = new Color(map.getRGB(x, y), true);

        if(BOB.getInstance().getScenarioSceneLoader().getTakenColors().contains(oldColor)) return;

        System.out.println("Click at " + x + ", " + y + " to open country menu for country at x,y");
    }

    private void handleTileClick(int x, int y) {
        //IO.println("Click at " + x + ", " + y);

        if(renderPanel.isPaused()) return;

        if (x > 0 && y > 0 && x < map.getWidth() && y < map.getHeight()) {
            Color oldColor = new Color(map.getRGB(x, y), true);

            if(BOB.getInstance().getScenarioSceneLoader().getTakenColors().contains(oldColor)) return;

            de.idiotischer.bob.state.State state = BOB.getInstance().getStateManager().getStateAt(x,y);

            if(state != null) System.out.println("clicked state: " + state.getName());

            FloodFill.fill(map, x,y, player.country().countryColor());
        }
        //FloodFill.fillBorder(visualBorderOverlay, map, x,y, Color.DARK_GRAY);
    }

    private void handleMovement() {
        int dx = 0;
        int dy = 0;
        int speed = 5;

        speed += (int) (zoom / 0.95); //damit schneller wenn näher

        if(keysPressed.contains(KeyEvent.VK_SHIFT)) speed += 7;

        if(keysPressed.contains(KeyEvent.VK_W) || keysPressed.contains(KeyEvent.VK_UP)) dy -= speed;
        if(keysPressed.contains(KeyEvent.VK_S) || keysPressed.contains(KeyEvent.VK_DOWN)) dy += speed;
        if(keysPressed.contains(KeyEvent.VK_A) || keysPressed.contains(KeyEvent.VK_LEFT)) dx -= speed;
        if(keysPressed.contains(KeyEvent.VK_D) || keysPressed.contains(KeyEvent.VK_RIGHT)) dx += speed;

        move(dx, dy);
    }

    public void move(int xMove, int yMove) {
        offsetX += xMove;
        offsetY += yMove;

        clampOffsets();
    }

    private void renderMenu() {
        menuPanel.setFrame(renderMap(map));
    }

    private BufferedImage renderMap(BufferedImage map) {
        BufferedImage frameBuffer = new BufferedImage(
                map.getWidth(),
                map.getHeight(),
                BufferedImage.TYPE_INT_ARGB
        );

        Graphics2D g = frameBuffer.createGraphics();

        g.drawImage(map, 0, 0, null);

        g.drawImage(visualBorderOverlay, 0, 0, null);

        g.dispose();

        return frameBuffer;
    }

    //private void render(BufferedImage map) {
    //    BufferedImage frame = new BufferedImage(
    //            map.getWidth(),
    //            map.getHeight(),
    //            BufferedImage.TYPE_INT_ARGB
    //    );
    //    Graphics2D g = frame.createGraphics();
    //    g.drawImage(map, 0, 0, null);
    //    g.dispose();
    //    renderPanel.setFrame(frame);
    //}

    //TODO: damit man nciht beim zoomen so kurz raus und wieder rein gebuggt wird halt die neuen offsets übergeben, dann die geclampten zurückgeben lassen und dann erst setzen
    public void clampOffsets() {
        int panelWidth = renderPanel.getWidth();
        int panelHeight = renderPanel.getHeight();

        int mapWidth = map.getWidth();
        int mapHeight = map.getHeight();

        double scaledWidth = mapWidth * zoom;
        double scaledHeight = mapHeight * zoom;

        if (scaledWidth <= panelWidth) {
            offsetX = -(panelWidth - scaledWidth) / 2;
        } else {
            offsetX = Math.max(0, Math.min(offsetX, scaledWidth - panelWidth));
        }

        if (scaledHeight <= panelHeight) {
            offsetY = -(panelHeight - scaledHeight) / 2;
        } else {
            offsetY = Math.max(0, Math.min(offsetY, scaledHeight - panelHeight));
        }
    }

    public void shutdown() {
        if(!BOB.getInstance().save()) System.out.println("Failed to safe before shutdown...");
        running = false;
        System.exit(0);
    }

    public static abstract class FrameListen implements ComponentListener{
        public void componentHidden(ComponentEvent arg0) {
        }
        public void componentMoved(ComponentEvent arg0) {
        }

        public abstract void componentResized(ComponentEvent arg0);

        public void componentShown(ComponentEvent arg0) {

        }
    }

    public double getMinZoom() {
        double panelWidth = renderPanel.getWidth();
        double panelHeight = renderPanel.getHeight();

        double mapWidth = map.getWidth();
        double mapHeight = map.getHeight();

        double zoomX = panelWidth / mapWidth;
        double zoomY = panelHeight / mapHeight;

        return Math.min(zoomX, zoomY);
    }

    public RenderPanel getGamePanel() {
        return renderPanel;
    }

    public double getZoom() {
        return zoom;
    }

    public double getOffsetX() { return offsetX; }
    public double getOffsetY() { return offsetY; }

    public void setOffsetX(double offsetX) {
        this.offsetX = offsetX;
    }

    public void setOffsetY(double offsetY) {
        this.offsetY = offsetY;
    }

    public boolean isWasAtMinZoom() {
        return wasAtMinZoom;
    }

    public void setZoom(double zoom) {
        this.zoom = zoom;
    }

    public void setWasAtMinZoom(boolean wasAtMinZoom) {
        this.wasAtMinZoom = wasAtMinZoom;
    }

    public Set<Integer> getKeysPressed() {
        return keysPressed;
    }

    public void setMainMenu(boolean mm) {
        this.inMenu = mm;
    }

    public void setMap(BufferedImage map) {
        this.map = map;
    }

    public MenuPanel getMenuPanel() {
        return menuPanel;
    }

    public Point getDragStart() {
        return dragStart;
    }

    public Point getDragEnd() {
        return dragEnd;
    }

    public JFrame getFrame() {
        return frame;
    }
}