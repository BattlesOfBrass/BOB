package de.idiotischer.bob.render.menu.impl;

import de.idiotischer.bob.BOB;
import de.idiotischer.bob.render.menu.Menu;
import de.idiotischer.bob.render.menu.components.button.BOBButton;

import javax.swing.*;
import java.awt.*;

public class StartMenu extends JPanel implements Menu {

    //ButtonComp start = new ButtonComp("Select Start", Color.WHITE, Color.DARK_GRAY,true,0,100,150,50, 16,16, 15, Color.DARK_GRAY.brighter(), Color.BLACK, true,(b) -> {
    //    System.out.println("clicked mm");
    //    BOB.getInstance().getMainRenderer().getMenuPanel().setInScenarioSelect(true);
    //});

    //ButtonComp multiplayer = new ButtonComp("Multiplayer", Color.WHITE, Color.DARK_GRAY,true,0,0,150,50, 16,16, 15, Color.DARK_GRAY.brighter(), Color.BLACK, true,(b) -> {
    //    System.out.println("clicked settings");
    //});

    //ButtonComp settings = new ButtonComp("Settings", Color.WHITE, Color.DARK_GRAY,true,0,-100,150,50, 16,16, 15, Color.DARK_GRAY.brighter(), Color.BLACK, true,(b) -> {
    //    System.out.println("clicked settings");
    //});

    //ButtonComp quit = new ButtonComp("Quit", Color.WHITE, Color.DARK_GRAY,true,0,-200,150,50, 16,16, 15, Color.DARK_GRAY.brighter(), Color.BLACK, true,(b) -> {
    //    System.out.println("clicked continue");
    //    System.exit(0);
    //});

    public StartMenu() {
        this.setOpaque(false);

        this.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 0, 10, 0);

        JButton startBtn = createButton("Select Start");
        startBtn.addActionListener(e -> {
            BOB.getInstance().getMainRenderer().getMenuPanel().setInScenarioSelect(true);
        });

        JButton multiplayerBtn = createButton("Multiplayer");
        multiplayerBtn.setEnabled(false);

        JButton settingsBtn = createButton("Settings");

        JButton quitBtn = createButton("Quit");
        quitBtn.addActionListener(e -> System.exit(0));

        this.add(startBtn, gbc);
        this.add(multiplayerBtn, gbc);
        this.add(settingsBtn, gbc);
        this.add(quitBtn, gbc);
    }

    private JButton createButton(String text) {
        JButton btn = new BOBButton(text,
                Color.WHITE,
                Color.BLACK,
                Color.DARK_GRAY.brighter(),
                Color.LIGHT_GRAY,
                16,
                5
        );
        btn.setPreferredSize(new Dimension(200, 50));
        btn.setFocusable(false);
        return btn;
    }
}
