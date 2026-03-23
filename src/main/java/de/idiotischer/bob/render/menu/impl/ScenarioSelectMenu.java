package de.idiotischer.bob.render.menu.impl;

import de.idiotischer.bob.BOB;
import de.idiotischer.bob.render.menu.Menu;
import de.idiotischer.bob.render.menu.components.ButtonComp;
import de.idiotischer.bob.render.menu.components.ScrollContainer;
import de.idiotischer.bob.scenario.Scenario;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ScenarioSelectMenu implements Menu {

    private final JPanel parent;
    private final int layoutScaleX;
    private final int layoutScaleY;
    private Set<Scenario> scenarios;
    private final ScrollContainer scroller;

    public ScenarioSelectMenu(JPanel panel, int layoutScaleX, int layoutScaleY) {
        this.parent = panel;

        scroller = new ScrollContainer(panel, new Color(200, 200, 200, 180), true);

        this.layoutScaleX = layoutScaleX;
        this.layoutScaleY = layoutScaleY;

        reload();

        int x = parent.getWidth() / 2 - (layoutScaleX / 2) + 20;
        int y = parent.getHeight() / 2 - (layoutScaleY / 2) + 20;
        int width = layoutScaleX - 40;
        int height = layoutScaleY - 40;

        scroller.setBounds(new Rectangle(x, y, width, height));
    }

    @Override
    public void paint(Graphics g) {
        g.setColor(Color.DARK_GRAY);

        Graphics2D g2 = (Graphics2D) g;
        //center logic so für alles übernehmen
        int x = parent.getWidth() / 2 - (layoutScaleX / 2);
        int y = parent.getHeight() / 2 - (layoutScaleY / 2);

        g2.setStroke(new BasicStroke(16));

        g.setColor(Color.DARK_GRAY.darker());
        g2.drawRoundRect(x, y, layoutScaleX, layoutScaleY,32,32);

        g.setColor(Color.DARK_GRAY);
        g.fillRoundRect(x, y, layoutScaleX, layoutScaleY,32,32);

        scroller.paint(g2);
    }

    @Override
    public void keyPress(KeyEvent e) {
        if(e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            BOB.getInstance().getMapRenderer().getMenuPanel().setInScenarioSelect(false);
        }
    }

    @Override
    public void mouseClick(MouseEvent e, int x, int y) {
        scroller.mouseClick(e, x, y);
    }

    @Override
    public void mouseRelease(MouseEvent e, int x, int y) {
        scroller.mouseRelease(e, x, y);
    }

    @Override
    public void mouseMove(MouseEvent e, int x, int y) {
        scroller.mouseMove(e, x, y);
    }

    @Override
    public void mouseScroll(MouseWheelEvent e, int x, int y) {
        scroller.mouseScroll(e, x, y);
    }

    public void reload() {
        this.scenarios = BOB.getInstance().getScenarioManager().getScenarios();

        List<ButtonComp> buttons = new ArrayList<>();
        for (Scenario s : scenarios) {
            ButtonComp b = new ButtonComp(s.getName(),
                    Color.WHITE,
                    Color.BLACK,
                    false,
                    0, 0,
                    layoutScaleX - 40, 40,
                    16, 16,
                    2, Color.LIGHT_GRAY,
                    Color.DARK_GRAY,
                    true, (b1) -> {System.out.println("clicked settings");});
            b.setPanel(parent);
            buttons.add(b);
        }

        scroller.setChildren(new ArrayList<>(buttons));
    }

    public Set<Scenario> getScenarios() {
        return scenarios;
    }
}
