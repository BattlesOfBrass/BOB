
package de.idiotischer.bob.render.menu.impl;

import de.idiotischer.bob.BOB;
import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.focus.Focus;
import de.idiotischer.bob.focus.FocusManager;
import de.idiotischer.bob.focus.FocusTree;
import de.idiotischer.bob.game.GameState;
import de.idiotischer.bob.networking.packet.impl.pp.RequestPacket;
import de.idiotischer.bob.networking.packet.impl.pp.Type;
import de.idiotischer.bob.render.menu.components.button.FocusButton;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.*;

public class FocusesUI extends JPanel {

    private String country;
    private boolean countryChanged = true;
    private boolean treeChanged = true;
    private Set<FocusButton> buttons = new HashSet<>();
    private String treeId = "";
    private final JButton backButton;
    private final Map<Focus,Point> absolutePs = new HashMap<>();

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
        backButton.setBorder(BorderFactory.createLineBorder(BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().button().borderColor(),4));
        backButton.setFocusPainted(false);

        backButton.addActionListener(e -> setVisible(false));

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 15));
        topPanel.setOpaque(false);
        topPanel.add(backButton);

        add(topPanel, BorderLayout.NORTH);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;

        Color dark = BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().palette().darkColor();

        g2.setColor(dark);
        g2.fillRect(0, 10, getWidth(), getHeight() - 10);
        g2.setStroke(new BasicStroke(12));
        g2.setColor(dark.darker());
        g2.drawRect(0, 10, getWidth(), getHeight() - 10);

        g2.setColor(Color.WHITE);
        Font font = /*FileUtil.getFont();*/new Font(Font.SANS_SERIF, Font.BOLD, 15);
        g2.setFont(font);

        Country c = BOB.getInstance().getCountryManager().byAbbreviation(country);

        if(c == null) return;

        FocusTree tree = BOB.getInstance().getFocusManager().getFocusTree(c);

        if(tree != null)
            if(!Objects.equals(treeId, tree.id())) {
                treeId = tree.id();
                treeChanged = true;
            }

        FontMetrics fm = g2.getFontMetrics();

        int x = getWidth() / 2;
        int y = (getHeight() - fm.getHeight()) + fm.getAscent();
        g2.drawString(c.countryName() + "'s focustree", x, y);

        calcFocuses(c);

        if(tree != null) drawConnections(g2,tree,absolutePs);

        g2.dispose();
    }

    public void calcFocuses(Country c) {
        if(!countryChanged && !treeChanged) return;
        buttons.forEach(this::remove);
        buttons.clear();
        absolutePs.clear();

        FocusTree tree = BOB.getInstance().getFocusManager().getFocusTree(c);

        if (tree == null) {
            buttons.forEach(this::remove);
            buttons.clear();
            return;
        }

        Set<Focus> focuses = tree.focuses();

        for(Focus focus : focuses) getAbsolute(tree.id(),focus,absolutePs, new HashSet<>());
        for(Focus focus : focuses) {
            Point p = absolutePs.get(focus);

            int x = p.x + 20; //for now + 20, maybe do * 2 or higher than 20 imo
            int y = p.y + 20; //for now + 20, maybe do * 2 or higher than 20 imo

            FocusButton button = new FocusButton(tree.id(),c.getAbbreviation(), focus,new Point(x,y));

            buttons.add(button);
            this.add(button);
        }

        treeChanged = false;
        countryChanged = false;
    }

    private void drawConnections(Graphics2D g2, FocusTree tree, Map<Focus, Point> absolutePs) {
        g2.setStroke(new BasicStroke(3f));
        g2.setColor(Color.WHITE);

        for (Focus focus : tree.focuses()) {
            String prerequisite = focus.getPrerequisite();

            if (prerequisite == null || prerequisite.isEmpty()) continue;

            Focus parent = BOB.getInstance().getFocusManager().focusBy(tree.id(), prerequisite);

            if (parent == null) continue;

            Point from = absolutePs.get(parent);
            Point to = absolutePs.get(focus);

            if (from == null || to == null) continue;

            g2.drawLine(from.x + 30, from.y + 10, to.x + 30, to.y + 10);
        }
    }

    @Override
    public void setVisible(boolean aFlag) {
        BOB.getInstance().getMainRenderer().setCanMove(!aFlag);
        super.setVisible(aFlag);
    }

    private Point getAbsolute(String treeId, Focus focus, Map<Focus, Point> absolutePs, Set<Focus> vists) {
        if(absolutePs.containsKey(focus))
            return absolutePs.get(focus);

        if(!vists.add(focus)) throw new RuntimeException("Circular focus!");

        Point relative = focus.getPoint();
        Point absolute = new Point(relative);

        String prerequisite = focus.getPrerequisite();

        if(prerequisite != null && !prerequisite.isEmpty()) {
            Focus par = BOB.getInstance().getFocusManager().focusBy(treeId,prerequisite);

            if(par != null) {
                Point parAbs = getAbsolute(treeId,par,absolutePs,vists);

                absolute.translate(parAbs.x,parAbs.y);
            }
        }

        vists.remove(focus);
        absolutePs.put(focus,absolute);

        return absolute;
    }

    public void setCountryToView(String country) {
        if(!Objects.equals(country, this.country)) countryChanged = true;
        this.country = country;
    }

    public String getCountry() {
        return country;
    }
}
