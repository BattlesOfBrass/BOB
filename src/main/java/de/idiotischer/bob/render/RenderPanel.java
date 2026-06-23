package de.idiotischer.bob.render;

import de.idiotischer.bob.BOB;
import de.idiotischer.bob.combat.CombatStatus;
import de.idiotischer.bob.map.FloodFill;
import de.idiotischer.bob.render.menu.Panel;
import de.idiotischer.bob.render.menu.components.button.CombatVisualButton;
import de.idiotischer.bob.render.menu.components.button.TroopVisualButton;
import de.idiotischer.bob.render.menu.impl.HUD;
import de.idiotischer.bob.render.menu.impl.ESCMenu;
import de.idiotischer.bob.troop.Troop;
import de.idiotischer.bob.troop.TroopDrawer;
import de.idiotischer.bob.troop.TroopStack;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

public class RenderPanel extends JPanel implements Panel {

    private final MainRenderer renderer;
    private BufferedImage frame;

    private final HUD hud;
    private final ESCMenu escOverlay;
    private final JPanel troopLayer;

    private int curvature = 24;
    private boolean escMenu = false;

    public Set<TroopVisualButton> selected = new HashSet<>();

    private final Map<UUID, CombatVisualButton> combatButtons = new HashMap<>();

    public RenderPanel(BufferedImage map, MainRenderer renderer) {
        this.renderer = renderer;
        this.frame = map;

        this.setLayout(new OverlayLayout(this));

        this.escOverlay = new ESCMenu();
        this.hud = new HUD();
        this.troopLayer = new JPanel(null);

        troopLayer.setOpaque(false);
        troopLayer.setFocusable(false);

        this.add(escOverlay);
        this.add(hud);
        this.add(troopLayer);

        this.escOverlay.setVisible(false);
        this.hud.setVisible(true);

        this.setBackground(Color.BLACK);
        this.setFocusable(true);
        this.requestFocusInWindow();
        this.setPreferredSize(new Dimension(frame.getWidth(), frame.getHeight()));
        renderer.getCamera().zoomToMin();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (renderer.getMap() == null) return;

        Graphics2D g2 = (Graphics2D) g;
        AffineTransform screenTransform = g2.getTransform();

        g2.transform(renderer.getCamera().getTransform());

        if (BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getBackgroundImage() != null) {
            g2.drawImage(BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getBackgroundImage(), 0, 0, null);
        }

        g2.drawImage(renderer.getMap(), 0, 0, null);

        if (renderer.getVisualBorderOverlay() != null) {
            g2.drawImage(renderer.getVisualBorderOverlay(), 0, 0, null);
        }

        updateTroopButtons(g2);
        updateCombatButtons();

        g2.setTransform(screenTransform);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        //handleDragOverlay(g2);
    }

    public void updateTroopButtons(Graphics2D g2) {
        List<TroopStack> visible = new ArrayList<>(BOB.getInstance().getTroopManager().getVisible(BOB.getInstance().getPlayer().country()).values());

        if(isPeaceConference()) visible.clear();

        if(isPeaceConference()) {
            List<Component> toRemove = new ArrayList<>();

            for (Component c : troopLayer.getComponents()) {
                if (c instanceof TroopVisualButton button) {
                    toRemove.add(c);
                    selected.remove(button);

                }
            }

            for (Component c : toRemove) {
                troopLayer.remove(c);
            }

            selected.clear();

            return;
        }

        AffineTransform transform = renderer.getCamera().getTransform();

        int baseWidth = (int) (136 / 2.4);
        int baseHeight = (int) (78 / 2.4);

        double zoom = renderer.getCamera().getZoom();

        double scale = 1.0 / Math.max(zoom, 0.1);
        scale = Math.min(scale, 1.05);
        scale = Math.max(scale, 1.0);

        int width = (int) (baseWidth * scale);
        int height = (int) (baseHeight * scale);

        List<Component> toRemove = new ArrayList<>();
        for (Component c : troopLayer.getComponents()) {
            if (c instanceof TroopVisualButton button) {
                if (!visible.contains(button.getStack())) {
                    toRemove.add(c);
                    selected.remove(button);
                }
            }
        }

        for (Component c : toRemove) {
            troopLayer.remove(c);
        }

        for (int i = 0; i < visible.size(); i++) {
            TroopStack stack = visible.get(i);

            TroopVisualButton button = null;

            for (Component c : troopLayer.getComponents()) {
                if (c instanceof TroopVisualButton troopButton
                        && troopButton.getStack() == stack) {
                    button = troopButton;
                    break;
                }
            }

            if (button == null) {
                button = new TroopVisualButton(stack);

                TroopVisualButton finalButton = button;

                TroopVisualButton finalButton1 = button;
                button.addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mousePressed(java.awt.event.MouseEvent e) {
                        boolean shiftHeld = (e.getModifiersEx() & java.awt.event.InputEvent.SHIFT_DOWN_MASK) != 0;
                        boolean alreadySelected = selected.contains(finalButton);

                        if(!Objects.equals(finalButton1.getStack().getController().getAbbreviation(), BOB.getInstance().getPlayer().country().getAbbreviation())) return;

                        if (!shiftHeld) {
                            selected.clear();
                        }

                        if (alreadySelected) {
                            selected.remove(finalButton);
                        } else {
                            selected.add(finalButton);
                        }
                    }
                });

                troopLayer.add(button);
            }

            Point world = stack.getTile().getPoints().getFirst();
            Point screen = new Point();
            transform.transform(world, screen);

            int stackIndexOnTile = 0;
            for (int j = 0; j < i; j++) {
                TroopStack other = visible.get(j);
                if (other.getTile() == stack.getTile()) {
                    stackIndexOnTile++;
                }
            }

            int yOffset = (stackIndexOnTile * (height + 2));

            button.setBounds(
                    screen.x - width / 2,
                    screen.y - height / 2 - yOffset,
                    width,
                    height
            );
        }

        troopLayer.revalidate();
        troopLayer.repaint();
    }

    private TroopVisualButton getButton(TroopStack stack) {
        for (Component c : troopLayer.getComponents()) {
            if (c instanceof TroopVisualButton button
                    && button.getStack() == stack) {
                return button;
            }
        }
        return null;
    }

    private void updateCombatButtons() {
        Set<UUID> activeIds = new HashSet<>();

        if(isPeaceConference()) {
            combatButtons.forEach((key, value) -> troopLayer.remove(value));

            combatButtons.clear();

            return;
        }

        double zoom = renderer.getCamera().getZoom();

        double scale = 1.0 / Math.max(zoom, 0.1);
        scale = Math.min(scale, 1.05);
        scale = Math.max(scale, 1.0);

        int baseCombatSize = 60;
        int combatSize = (int) (baseCombatSize * scale);

        for (CombatStatus combat : BOB.getInstance().getCombatManager().getActiveCombats()) {
            if (combat.isFinished()
                    || combat.getAttackers().isEmpty()
                    || combat.getDefenders().isEmpty()) {
                BOB.getInstance().getCombatManager().remove(combat.getUuid());
                continue;
            }

            activeIds.add(combat.getUuid());

            if (combat.getAttackers().isEmpty() || combat.getDefenders().isEmpty()) {
                BOB.getInstance().getCombatManager().remove(combat.getUuid());
                continue;
            }

            TroopStack attacker = new ArrayList<>(combat.getAttackers()).getFirst();
            TroopStack defender = new ArrayList<>(combat.getDefenders()).getFirst();

            TroopVisualButton atkButton = getButton(attacker);
            TroopVisualButton defButton = getButton(defender);

            if (atkButton == null || defButton == null) {
                continue;
            }

            CombatVisualButton combatButton = combatButtons.computeIfAbsent(combat.getUuid(), uuid -> {
                CombatVisualButton b = new CombatVisualButton(uuid);
                troopLayer.add(b);
                return b;
            });

            Rectangle a = atkButton.getBounds();
            Rectangle d = defButton.getBounds();

            int ax = a.x + a.width / 2;
            int ay = a.y + a.height / 2;

            int dx = d.x + d.width / 2;
            int dy = d.y + d.height / 2;

            int centerX = (ax + dx) / 2;
            int centerY = (ay + dy) / 2;

            combatButton.setDirection(ax, ay, dx, dy);

            combatButton.setBounds(centerX - combatSize / 2, centerY - combatSize / 2, combatSize, combatSize);
        }

        combatButtons.entrySet().removeIf(entry -> {
            boolean remove = !activeIds.contains(entry.getKey());

            if (remove) {
                troopLayer.remove(entry.getValue());
            }

            return remove;
        });
    }

    /*private void drawTroops(Graphics2D g2) {
        List<TroopStack> visible =
                BOB.getInstance().getTroopManager()
                        .getVisible(BOB.getInstance().getPlayer().country());

        double zoom = renderer.getCamera().getZoom();
        double scale = 1.0 / Math.max(zoom, 0.1);
        scale = Math.max(0.5, Math.min(scale, 2.0));

        int width = (int) (48 * scale) / 4;
        int height = (int) (26 * scale) / 4;

        Stroke oldStroke = g2.getStroke();
        g2.setStroke(new BasicStroke(0.3f));

        for (TroopStack stack : visible) {
            Point p = stack.getTile().getPoints().getFirst();

            int x = p.x - width / 2;
            int y = p.y - height / 2;

            g2.setColor(Color.DARK_GRAY);
            g2.fillRect(x, y, width, height);

            if (stack.getOwner() != null) {
                BufferedImage img = stack.getOwner().getFlagImage();

                g2.drawImage(img, x, y, x + width, y + height, 0, 0, img.getWidth(), img.getHeight(), null);
            }

            g2.setColor(stack.getController() == null ? Color.GREEN : stack.getController().countryColor().brighter());

            Shape r = new Rectangle2D.Double(x, y, width, height);
            g2.draw(r);
        }

        g2.setStroke(oldStroke);
    }*/


    public void setEscMenu(boolean on) {
        this.escMenu = on;
        this.escOverlay.setVisible(on);

        if(on) {
            getHud().visible(false);
        }

        this.revalidate();
        //this.repaint();
    }

    public boolean isEscMenu() {
        return escMenu;
    }

    public boolean isPaused() {
        return escMenu;
    }

    public HUD getHud() {
        return hud;
    }

    @Override
    public void mouseClick(MouseEvent e, int x, int y) {}

    @Override
    public void mouseRelease(MouseEvent e, int x, int y) {}

    @Override
    public void mouseMove(MouseEvent e, int x, int y) {}

    public void setFrame(BufferedImage frame) {
        this.frame = frame;
    }

    public Set<TroopVisualButton> getTroopButtonGroup() {
        return selected;
    }

    public BufferedImage getFrame() {
        return frame;
    }

    public JPanel getTroopLayer() {
        return troopLayer;
    }

    public boolean isPeaceConference() {
        return false;
    }
}