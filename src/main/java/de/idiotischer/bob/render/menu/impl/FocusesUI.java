
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

    public FocusesUI() {
        setOpaque(false);
        setLayout(new BorderLayout());
        setRequestFocusEnabled(false);
        setFocusable(false);
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

        g2.dispose();
    }

    public void calcFocuses(Country c) {
        if(!countryChanged && !treeChanged) return;
        buttons.forEach(this::remove);
        buttons.clear();

        FocusTree tree = BOB.getInstance().getFocusManager().getFocusTree(c);
        Set<Focus> focuses = tree.focuses();
        Map<Focus,Point> absolutePs = new HashMap<>();

        for(Focus focus : focuses) getAbsolute(tree.id(),focus,absolutePs, new HashSet<>());
        for(Focus focus : focuses) {
            Point p = absolutePs.get(focus);

            int x = p.x;
            int y = p.y;

            FocusButton button = new FocusButton(tree.id(),focus,new Point(x,y));

            buttons.add(button);
            this.add(button);
        }

        treeChanged = false;
        countryChanged = false;
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
