
package de.idiotischer.bob.render.menu.impl;

import de.idiotischer.bob.BOB;
import de.idiotischer.bob.camera.Camera;
import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.focus.Focus;
import de.idiotischer.bob.focus.FocusTree;
import de.idiotischer.bob.render.menu.components.button.FocusButton;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class FocusesUI extends JPanel {

    private static final int CELL_W = 150;
    private static final int CELL_H = 100;
    private static final int BTN_W = 120;
    private static final int BTN_H = 50;
    private static final int PAD_TOP = 30;

    private static final double ZOOM_FACTOR = 1.1;

    private String country;
    private boolean countryChanged = true;
    private String treeId = "";

    private JPanel focusPopup;
    private FocusButton selectedButton;

    private final Map<Focus, FocusButton> buttons = new LinkedHashMap<>();
    private final Map<String, FocusButton> buttonsById = new HashMap<>();
    private final Map<Focus, Point> absolutePs = new HashMap<>();

    private final JButton backButton;
    private final JPanel topPanel;
    private final JPanel canvasPanel;

    private long lastMovementTime = 0;

    private static final double DEFAULT_MOVE_SPEED = 200.0;
    private static final double DEFAULT_MAP_DIAG = 2000.0;

    private Camera camera;

    private Point lastMousePosition;
    private boolean dragging = false;
    JLabel titleLabel;

    public FocusesUI() {
        setOpaque(false);
        setLayout(new BorderLayout());
        setRequestFocusEnabled(false);
        setFocusable(false);

        backButton = new JButton("←");
        backButton.setPreferredSize(new Dimension(backButton.getPreferredSize().width + 10, backButton.getPreferredSize().height + 10));
        backButton.setFocusable(false);
        backButton.setForeground(Color.WHITE);
        backButton.setBackground(BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().button().bgColor());
        backButton.setBorder(BorderFactory.createLineBorder(BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().button().borderColor(), 4));
        backButton.setFocusPainted(false);
        backButton.addActionListener(e -> setVisible(false));

        topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 15));
        leftPanel.setOpaque(false);
        leftPanel.add(backButton);

        titleLabel = new JLabel("Countries's Focus Tree", SwingConstants.CENTER);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 18));

        topPanel.add(leftPanel, BorderLayout.WEST);
        topPanel.add(titleLabel, BorderLayout.CENTER);

        add(topPanel, BorderLayout.NORTH);

        canvasPanel = new JPanel(null) {
            @Override
            public void doLayout() {
                layoutButtons(getWidth());
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                if (camera == null) return;

                Graphics2D g2 = (Graphics2D) g.create();

                try {
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.transform(camera.getTransform());

                    drawConnections(g2);
                } finally {
                    g2.dispose();
                }
            }
        };

        canvasPanel.setOpaque(false);

        canvasPanel.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    dragging = true;
                    lastMousePosition = e.getPoint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                dragging = false;
                lastMousePosition = null;
            }
        });

        canvasPanel.addMouseMotionListener(new MouseMotionAdapter() {

            @Override
            public void mouseDragged(MouseEvent e) {
                if (!dragging || camera == null || lastMousePosition == null) return;

                int dx = e.getX() - lastMousePosition.x;
                int dy = e.getY() - lastMousePosition.y;

                camera.move(-dx, -dy, true);

                lastMousePosition = e.getPoint();

                updateButtonPositions();
                canvasPanel.repaint();
            }
        });

        canvasPanel.addMouseWheelListener(e -> {
            if (camera == null) return;

            double factor = e.getWheelRotation() < 0 ? ZOOM_FACTOR : 1.0 / ZOOM_FACTOR;

            camera.zoom(factor, e.getX(), e.getY());

            updateButtonPositions();
            canvasPanel.repaint();
        });

        canvasPanel.addComponentListener(new ComponentAdapter() {

            @Override
            public void componentResized(ComponentEvent e) {

                if (camera == null) return;

                camera.setViewportSize(canvasPanel.getWidth(), canvasPanel.getHeight());

                updateButtonPositions();
                canvasPanel.repaint();
            }
        });

        add(canvasPanel, BorderLayout.CENTER);
    }

    public void handleMovement(double deltaTime) {
        if (camera == null) return;

        double dx = 0;
        double dy = 0;

        double mapW = camera.getMapWidth();
        double mapH = camera.getMapHeight();

        double mapDiag = Math.sqrt(mapW * mapW + mapH * mapH);

        double speed = DEFAULT_MOVE_SPEED * (mapDiag / DEFAULT_MAP_DIAG);

        var keysPressed = BOB.getInstance().getMainRenderer().getKeysPressed();

        if (keysPressed.contains(KeyEvent.VK_SHIFT)) {
            speed *= 1.8;
        }

        speed *= camera.getZoom();

        if (keysPressed.contains(KeyEvent.VK_W)) dy -= speed * deltaTime;
        if (keysPressed.contains(KeyEvent.VK_S)) dy += speed * deltaTime;
        if (keysPressed.contains(KeyEvent.VK_A)) dx -= speed * deltaTime;
        if (keysPressed.contains(KeyEvent.VK_D)) dx += speed * deltaTime;

        if (dx == 0 && dy == 0) return;

        camera.move(dx, dy, false);
        camera.clamp();

        updateButtonPositions();
        canvasPanel.repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();

        try {
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            Color dark = BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().palette().darkColor();

            g2.setColor(dark);
            g2.fillRect(0, 10, getWidth(), getHeight() - 10);

            g2.setStroke(new BasicStroke(12));
            g2.setColor(dark.darker());
            g2.drawRect(6, 16, getWidth() - 12, getHeight() - 22);

            Country c = BOB.getInstance().getCountryManager().byAbbreviation(country);
            if (c == null) return;

            titleLabel.setText(c.countryName() + "'s Focus Tree");
        } finally {
            g2.dispose();
        }
    }

    public void calcFocuses(Country c) {
        FocusTree tree = BOB.getInstance().getFocusManager().getFocusTree(c);
        String newTreeId = tree == null ? "" : tree.id();

        if (!countryChanged && Objects.equals(treeId, newTreeId)) return;

        buttons.values().forEach(canvasPanel::remove);
        buttons.clear();
        buttonsById.clear();
        absolutePs.clear();

        camera = null;

        treeId = newTreeId;
        countryChanged = false;

        if (tree == null) {
            canvasPanel.revalidate();
            canvasPanel.repaint();
            return;
        }

        Set<Focus> focuses = tree.focuses();

        for (Focus focus : focuses) {
            if (focus == null) continue;
            getAbsolute(tree.id(), focus, absolutePs, new HashSet<>());
        }

        for (Focus focus : focuses) {
            if (focus == null) continue;

            Point p = absolutePs.get(focus);
            if (p == null) continue;

            FocusButton button = new FocusButton(tree.id(), c.getAbbreviation(), focus, p, this::openFocusPopup);

            buttons.put(focus, button);
            buttonsById.put(focus.getId(), button);
            canvasPanel.add(button);
        }

        initializeCamera();

        canvasPanel.revalidate();
        canvasPanel.repaint();
    }

    private void initializeCamera() {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (Point p : absolutePs.values()) {
            if (p == null) continue;

            minX = Math.min(minX, p.x);
            minY = Math.min(minY, p.y);

            maxX = Math.max(maxX, p.x);
            maxY = Math.max(maxY, p.y);
        }

        if (minX == Integer.MAX_VALUE) return;

        for (Point p : absolutePs.values()) if (p != null) p.translate(-minX, -minY);

        int mapWidth = (maxX - minX + 1) * CELL_W + CELL_W;
        int mapHeight = (maxY - minY + 1) * CELL_H + PAD_TOP + CELL_H;

        camera = new Camera(mapWidth, mapHeight);

        camera.setViewportSize(canvasPanel.getWidth(), canvasPanel.getHeight());

        camera.zoomToMin();

        updateButtonPositions();
    }

    private void layoutButtons(int width) {

        if (camera == null) return;

        int centerX = camera.getMapWidth() / 2;

        for (Map.Entry<Focus, FocusButton> e : buttons.entrySet()) {

            Point p = absolutePs.get(e.getKey());
            if (p == null) continue;

            int worldX = centerX + p.x * CELL_W - BTN_W / 2;
            int worldY = PAD_TOP + p.y * CELL_H;

            int x = camera.worldToScreenX(worldX);
            int y = camera.worldToScreenY(worldY);

            int w = Math.max(1, (int) (BTN_W * camera.getZoom()));
            int h = Math.max(1, (int) (BTN_H * camera.getZoom()));

            e.getValue().setBounds(x, y, w, h);
        }
    }

    private void updateButtonPositions() {
        layoutButtons(canvasPanel.getWidth());
    }

    private void drawConnections(Graphics2D g2) {
        if (camera == null) return;
        //also draw the thingy here for when a focus is exclusively avaible with another

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int centerX = camera.getMapWidth() / 2;

        for (Map.Entry<Focus, FocusButton> e : buttons.entrySet()) {

            Focus child = e.getKey();

            String prerequisite = child.getPrerequisite();

            if (prerequisite == null || prerequisite.isEmpty()) continue;

            Focus parent = buttons.keySet().stream().filter(f -> f.getId().equals(prerequisite)).findFirst().orElse(null);

            if (parent == null) continue;

            Point parentP = absolutePs.get(parent);
            Point childP = absolutePs.get(child);

            if (parentP == null || childP == null) continue;

            int x1 = centerX + parentP.x * CELL_W;
            int y1 = PAD_TOP + parentP.y * CELL_H + BTN_H;

            int x2 = centerX + childP.x * CELL_W;
            int y2 = PAD_TOP + childP.y * CELL_H;

            int midY = (y1 + y2) / 2;

            drawConnection(g2, parent, child, country, x1, y1, x2, y2, midY);
        }
    }
    private void drawConnection(Graphics2D g2, Focus from, Focus to, String country, int x1, int y1, int x2, int y2, int midY) {
        boolean fromDone = from.getCountryDone().contains(country);
        boolean toDone = to.getCountryDone().contains(country);

        Color color;

        if (fromDone && toDone) color = new Color(80, 200, 100);
        else if (fromDone || toDone) color = new Color(220, 180, 60);
        else color = new Color(110, 110, 110);

        g2.setColor(color);
        g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        g2.drawLine(x1, y1, x1, midY);
        g2.drawLine(x1, midY, x2, midY);
        g2.drawLine(x2, midY, x2, y2);
    }

    public void refresh() {
        SwingUtilities.invokeLater(() -> { closeFocusPopup(); canvasPanel.repaint(); });
    }

    @Override
    public void setVisible(boolean aFlag) {
        BOB.getInstance().getMainRenderer().setCanMove(!aFlag);
        super.setVisible(aFlag);

        if (aFlag) {
            Country c = BOB.getInstance().getCountryManager().byAbbreviation(country);
            if (c == null) return;
            calcFocuses(c);
        }
    }


    private Point getAbsolute(String treeId, Focus focus, Map<Focus, Point> absolutePs, Set<Focus> visits) {
        if (focus == null) return null;

        if (absolutePs.containsKey(focus)) return absolutePs.get(focus);


        if (!visits.add(focus)) throw new RuntimeException("Circular focus!");


        Point relative = focus.getPoint();

        if (relative == null) {
            visits.remove(focus);
            return null;
        }

        Point absolute = new Point(relative);

        visits.remove(focus);
        absolutePs.put(focus, absolute);

        return absolute;
    }

    private void openFocusPopup(FocusButton button) {
        if (selectedButton == button) {
            closeFocusPopup();
            return;
        }

        closeFocusPopup();

        selectedButton = button;

        Focus focus = button.getFocus();

        Color background = BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().palette().darkColor();

        focusPopup = new JPanel(new BorderLayout(15, 0));
        focusPopup.setBackground(background);

        focusPopup.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.WHITE, 2), BorderFactory.createEmptyBorder(12, 12, 12, 12)));

        JPanel infoPanel = new JPanel();
        infoPanel.setOpaque(false);
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));

        JLabel name = new JLabel(focus.getName());
        name.setForeground(Color.WHITE);
        name.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        name.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextArea description = new JTextArea(focus.getDescription());
        description.setForeground(Color.WHITE);
        description.setBackground(background);
        description.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        description.setEditable(false);
        description.setFocusable(false);
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        description.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel duration = new JLabel("Duration: " + focus.getTime() + " days");
        duration.setForeground(Color.LIGHT_GRAY);
        duration.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel ppCost = new JLabel("Takes PP: " + focus.getPPDebuff());
        ppCost.setForeground(Color.LIGHT_GRAY);
        ppCost.setAlignmentX(Component.LEFT_ALIGNMENT);

        infoPanel.add(name);
        infoPanel.add(Box.createVerticalStrut(10));
        infoPanel.add(description);
        infoPanel.add(Box.createVerticalStrut(12));
        infoPanel.add(duration);
        infoPanel.add(ppCost);

        JPanel actionPanel = new JPanel();
        actionPanel.setOpaque(false);
        actionPanel.setLayout(new BoxLayout(actionPanel, BoxLayout.Y_AXIS));

        JButton actionButton = new JButton();

        actionButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        actionButton.setForeground(Color.WHITE);
        actionButton.setFocusPainted(false);
        actionButton.setOpaque(true);
        actionButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        boolean completed = focus.getCountryDone().contains(country);

        Focus active = BOB.getInstance().getFocusManager().getActiveFocus(BOB.getInstance().getCountryManager().byAbbreviation(country));

        boolean activeFocus = active != null && active.getId().equals(focus.getId());
        boolean anotherFocusActive = active != null && !activeFocus;

        //TODO: theme this (add theming support)
        if (completed) {
            actionButton.setText("Completed");
            actionButton.setBackground(new Color(14, 142, 79));
            actionButton.setBorder(BorderFactory.createLineBorder(new Color(94, 216, 27), 2));
            actionButton.setEnabled(false);
        } else if (activeFocus) {
            actionButton.setText("Cancel Focus");
            actionButton.setBackground(new Color(115, 55, 7));
            actionButton.setBorder(BorderFactory.createLineBorder(new Color(228, 107, 13), 2));
            actionButton.addActionListener(e -> {
                BOB.getInstance().getFocusManager().cancelFocusReq(country, focus.getId(), button.getTreeId());
                closeFocusPopup();
                canvasPanel.repaint();
            });
        } else if (anotherFocusActive) {
            actionButton.setText("Another Focus Active");
            actionButton.setBackground(new Color(76, 73, 73));
            actionButton.setBorder(BorderFactory.createLineBorder(new Color(138, 133, 133), 2));
            actionButton.setEnabled(false);
        } else {
            actionButton.setText("Start Focus");
            actionButton.setBackground(new Color(16, 96, 5));
            actionButton.setBorder(BorderFactory.createLineBorder(new Color(30, 182, 10), 2));
            actionButton.addActionListener(e -> {
                BOB.getInstance().getFocusManager().startFocusReq(country, focus.getId(), button.getTreeId());
                closeFocusPopup();
                canvasPanel.repaint();
            });
        }

        actionButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        actionPanel.add(Box.createVerticalGlue());
        actionPanel.add(actionButton);
        actionPanel.add(Box.createVerticalGlue());

        focusPopup.add(infoPanel, BorderLayout.CENTER);
        focusPopup.add(actionPanel, BorderLayout.EAST);

        canvasPanel.add(focusPopup);

        int popupWidth = 430;
        int popupHeight = 190;
        int margin = 20;

        int x = canvasPanel.getWidth() - popupWidth - margin;

        focusPopup.setBounds(x, margin, popupWidth, popupHeight);

        canvasPanel.setComponentZOrder(focusPopup, 0);

        focusPopup.setVisible(true);

        canvasPanel.revalidate();
        canvasPanel.repaint();
    }

    private void closeFocusPopup() {
        if (focusPopup != null) {
            canvasPanel.remove(focusPopup);
            focusPopup = null;
        }

        if (selectedButton != null) {
            selectedButton.setSelected(false);
            selectedButton = null;
        }

        canvasPanel.repaint();
    }

    public void setCountryToView(String country) {
        if (!Objects.equals(country, this.country)) countryChanged = true;
        this.country = country;
    }

    public String getCountry() {
        return country;
    }

    @Override
    public Dimension getPreferredSize() {
        HUD hud = BOB.getInstance().getMainRenderer().getGamePanel().getHud();
        return new Dimension(hud.getWidth(), hud.getHeight() - hud.getTopBar().getHeight());
    }
}
