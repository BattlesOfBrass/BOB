package de.idiotischer.bob.render;

import de.idiotischer.bob.BOB;
import de.idiotischer.bob.map.FloodFill;
import de.idiotischer.bob.player.Player;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

public class MainRenderer extends Thread {

    private final Player player;
    private boolean running = true;

    private RenderPanel panel;
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

    static void main(String[] args) {
        new MainRenderer(null).start();
    }

    public MainRenderer(Player player) {
        this.player = player;
    }

    @Override
    public void start() {
        inMenu = false;

        map = BOB.getInstance().getScenarioSceneLoader().getMap();
        visualBorderOverlay = new BufferedImage(map.getWidth(), map.getHeight(), BufferedImage.TYPE_INT_ARGB);

        panel = new RenderPanel(map,this);
        menuPanel = new MenuPanel(map,this);

        frame.setIconImage(BOB.getInstance().createIcon().getImage());
        frame.setBackground(Color.BLACK);
        frame.add(inMenu ? menuPanel : panel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                shutdown();
            }
        });

        //frame.setIconImage(icon);

        super.start();
    }

    public void setMainMenu(boolean mm) {
        this.inMenu = mm;
    }

    public MenuPanel getMenuPanel() {
        return menuPanel;
    }

    @Override
    public void run() {
        listen();

        while (running) {

            if (inMenu != lastMenuState) {
                frame.getContentPane().removeAll();

                if (inMenu) {
                    BOB.getInstance().getCountries().reload();
                    frame.add(menuPanel);
                    menuPanel.requestFocusInWindow();
                } else {
                    frame.add(panel);
                    panel.requestFocusInWindow();
                }

                frame.revalidate();
                frame.repaint();

                lastMenuState = inMenu;
            }

            if (!inMenu) {
                if(!panel.isPaused()) handleMovement();
                renderMap(map);
                panel.repaint();
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

    private void renderMenu() {

    }

    public void listen() {

        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int button = e.getButton();

                int x = (int) ((e.getX() + offsetX) / zoom);
                int y = (int) ((e.getY() + offsetY) / zoom);

                if(inMenu || panel.isEscMenu()) handleMenuClick(e,x,y);
                else {
                    if(button == MouseEvent.BUTTON3) handleCountryMenu(x,y);
                    if(button == MouseEvent.BUTTON1) handleTileClick(x,y);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                int x = (int) ((e.getX() + offsetX) / zoom);
                int y = (int) ((e.getY() + offsetY) / zoom);

                if(inMenu || panel.isEscMenu()) handleMenuRelease(e,x,y);
            }
        });

        panel.addMouseWheelListener(new MouseAdapter() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                if(panel.isEscMenu()) return;

                if (e.getScrollType() != MouseWheelEvent.WHEEL_UNIT_SCROLL) return;

                double oldZoom = zoom;

                int rotation = e.getWheelRotation();
                zoom *= Math.pow(1.1, -rotation);

                zoom = Math.max(1, Math.min(zoom, 20));

                double mouseX = e.getX();
                double mouseY = e.getY();

                offsetX = (offsetX + mouseX) * (zoom / oldZoom) - mouseX;
                offsetY = (offsetY + mouseY) * (zoom / oldZoom) - mouseY;

                panel.repaint();
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
                panel.repaint();

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

                if(inMenu || panel.isEscMenu()) handleMenuMove(e,x,y);
            }
        });

        panel.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                keysPressed.add(e.getKeyCode());

                if(keysPressed.contains(KeyEvent.VK_ESCAPE)) {
                    panel.setEscMenu(!panel.isEscMenu());
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                keysPressed.remove(e.getKeyCode());
            }
        });

        //panel.addKeyListener(new KeyAdapter() {
        //            @Override
        //            public void keyPressed(KeyEvent e) {
        //                char forward = 'w';
        //                char backward = 's';
        //                char right = 'a';
        //                char left = 'd';
        //
        //                if(e.getKeyChar() == forward) move(0, -1);
        //                else if(e.getKeyChar() == backward) move(0, 1);
        //                else if(e.getKeyChar() == right) move(-1, 0);
        //                else if(e.getKeyChar() == left) move(1, 0);
        //            }
        //        });
    }

    private void handleMenuClick(MouseEvent e, int x, int y) {
        panel.onClick(e,x,y);
    }

    private void handleMenuRelease(MouseEvent e, int x, int y) {
        panel.onRelease(e,x,y);
    }

    private void handleMenuMove(MouseEvent e, int x, int y) {
        panel.onMove(e,x,y);
    }

    private void handleMovement() {
        int dx = 0;
        int dy = 0;
        int speed = 5;

        if(keysPressed.contains(KeyEvent.VK_SHIFT)) speed += 5;

        if(keysPressed.contains(KeyEvent.VK_W)) dy -= speed;
        if(keysPressed.contains(KeyEvent.VK_S)) dy += speed;
        if(keysPressed.contains(KeyEvent.VK_A)) dx -= speed;
        if(keysPressed.contains(KeyEvent.VK_D)) dx += speed;

        move(dx, dy);
    }

    public Set<Integer> getKeysPressed() {
        return keysPressed;
    }

    public void move(int xMove, int yMove) {
        offsetX += xMove;
        offsetY += yMove;
    }

    private void handleCountryMenu(int x, int y) {

        if(panel.isPaused()) return;

        Color oldColor = new Color(map.getRGB(x, y), true);

        if(BOB.getInstance().getScenarioSceneLoader().getTakenColors().contains(oldColor)) return;

        IO.println("Click at " + x + ", " + y + " to open country menu for country at x,y");
    }

    private void handleTileClick(int x, int y) {
        //IO.println("Click at " + x + ", " + y);

        if(panel.isPaused()) return;

        if (x > 0 && y > 0 && x < map.getWidth() && y < map.getHeight()) {
            Color oldColor = new Color(map.getRGB(x, y), true);

            if(BOB.getInstance().getScenarioSceneLoader().getTakenColors().contains(oldColor)) return;

            de.idiotischer.bob.state.State state = BOB.getInstance().getStateManager().getStateAt(x,y);

            if(state != null) IO.println("clicked state: " + state.getName());

            FloodFill.fill(map, x,y, player.country().countryColor());
        }
        //FloodFill.fillBorder(visualBorderOverlay, map, x,y, Color.DARK_GRAY);
    }

    private void renderMap(BufferedImage map) {
        BufferedImage frameBuffer = new BufferedImage(
                map.getWidth(),
                map.getHeight(),
                BufferedImage.TYPE_INT_ARGB
        );

        Graphics2D g = frameBuffer.createGraphics();

        g.drawImage(map, 0, 0, null);

        g.drawImage(visualBorderOverlay, 0, 0, null);

        g.dispose();

        panel.setFrame(frameBuffer);
    }

    public Point getDragStart() {
        return dragStart;
    }

    public Point getDragEnd() {
        return dragEnd;
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
    //    panel.setFrame(frame);
    //}

    public void shutdown() {
        if(!BOB.getInstance().save()) IO.println("Failed to safe before shutdown...");
        running = false;
        System.exit(0);
    }

    public RenderPanel getGamePanel() {
        return panel;
    }

    public double getZoom() {
        return zoom;
    }

    public double getOffsetX() { return offsetX; }
    public double getOffsetY() { return offsetY; }
}