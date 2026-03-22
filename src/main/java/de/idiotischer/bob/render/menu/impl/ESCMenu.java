package de.idiotischer.bob.render.menu.impl;

import de.idiotischer.bob.BOB;
import de.idiotischer.bob.render.menu.Menu;
import de.idiotischer.bob.render.menu.menuComponent.ButtonComp;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class ESCMenu implements Menu {


    ButtonComp continueButton = new ButtonComp("Continue", Color.WHITE, Color.DARK_GRAY,0,100,150,50, 24,24, 20, Color.DARK_GRAY.brighter(), Color.BLACK, true,(b) -> {
        System.out.println("clicked continue");
        BOB.getInstance().getMapRenderer().getGamePanel().setEscMenu(false);
    });

    ButtonComp menu = new ButtonComp("Main Menu", Color.WHITE, Color.DARK_GRAY,0,-100,150,50, 24,24, 20, Color.DARK_GRAY.brighter(), Color.BLACK, true,(b) -> {
        System.out.println("clicked mm");
        BOB.getInstance().getMapRenderer().setMainMenu(true);
    });

    ButtonComp settings = new ButtonComp("Settings", Color.WHITE, Color.DARK_GRAY,0,0,150,50, 24,24, 20, Color.DARK_GRAY.brighter(), Color.BLACK, true,(b) -> {
        System.out.println("clicked settings");
    });

    List<ButtonComp> buttons = new ArrayList<>();

    public ESCMenu() {
        buttons.add(continueButton);
        buttons.add(menu);
        buttons.add(settings);
    }

    @Override
    public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        buttons.forEach(b -> b.paint(g2));
    }

    @Override
    public void mouseClick(MouseEvent e, int x, int y) {
        buttons.forEach(b ->b.mouseClick(e, x, y));
    }

    @Override
    public void mouseMove(MouseEvent e, int x, int y) {
        buttons.forEach(b ->b.mouseMove(e, x, y));
    }

    @Override
    public void mouseRelease(MouseEvent e, int x, int y) {
        buttons.forEach(b ->b.mouseRelease(e, x, y));
    }
}
