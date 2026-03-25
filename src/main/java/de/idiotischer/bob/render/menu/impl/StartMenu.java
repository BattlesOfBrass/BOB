package de.idiotischer.bob.render.menu.impl;

import de.idiotischer.bob.BOB;
import de.idiotischer.bob.render.menu.Component;
import de.idiotischer.bob.render.menu.Menu;
import de.idiotischer.bob.render.menu.components.button.ButtonComp;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class StartMenu implements Menu {

    ButtonComp start = new ButtonComp("Select Start", Color.WHITE, Color.DARK_GRAY,true,0,100,150,50, 16,16, 15, Color.DARK_GRAY.brighter(), Color.BLACK, true,(b) -> {
        System.out.println("clicked mm");
        BOB.getInstance().getMainRenderer().getMenuPanel().setInScenarioSelect(true);
    });

    ButtonComp multiplayer = new ButtonComp("Multiplayer", Color.WHITE, Color.DARK_GRAY,true,0,0,150,50, 16,16, 15, Color.DARK_GRAY.brighter(), Color.BLACK, true,(b) -> {
        System.out.println("clicked settings");
    });

    ButtonComp settings = new ButtonComp("Settings", Color.WHITE, Color.DARK_GRAY,true,0,-100,150,50, 16,16, 15, Color.DARK_GRAY.brighter(), Color.BLACK, true,(b) -> {
        System.out.println("clicked settings");
    });

    ButtonComp quit = new ButtonComp("Quit", Color.WHITE, Color.DARK_GRAY,true,0,-200,150,50, 16,16, 15, Color.DARK_GRAY.brighter(), Color.BLACK, true,(b) -> {
        System.out.println("clicked continue");
        System.exit(0);
    });

    private final List<Component> components = new ArrayList<>();

    public StartMenu(JPanel panel) {
        components.add(start);
        components.add(settings);
        components.add(quit);
        components.add(multiplayer);

        components.forEach(component -> {
            if(component instanceof ButtonComp b) {
                b.setPanel(panel);
            }
        });
    }

    @Override
    public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        components.forEach(component -> component.paint(g));
    }

    @Override
    public void mouseRelease(MouseEvent e, int x, int y) {
        components.forEach(component -> component.mouseRelease(e, x, y));
    }

    @Override
    public void mouseMove(MouseEvent e, int x, int y) {
        components.forEach(component -> component.mouseMove(e, x, y));
    }

    @Override
    public void mouseClick(MouseEvent e, int x, int y) {
        components.forEach(component -> component.mouseClick(e, x, y));
    }
}
