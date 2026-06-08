package de.idiotischer.bob.render.menu.impl;

import de.idiotischer.bob.BOB;
import de.idiotischer.bob.render.menu.components.button.BOBButton;

import javax.swing.*;
import java.awt.*;

public class ESCMenu extends JPanel {

    public ESCMenu() {
        this.setOpaque(false);

        this.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.insets = new Insets(10, 0, 10, 0);

        JButton continueBtn = createButton("Continue");
        continueBtn.addActionListener(e -> BOB.getInstance().getMainRenderer().getGamePanel().setEscMenu(false));

        JButton menuBtn = createButton("Main Menu");
        menuBtn.addActionListener(e -> BOB.getInstance().getMainRenderer().setMainMenu(true));

        JButton settingsBtn = createButton("Settings");

        this.add(continueBtn, gbc);
        this.add(settingsBtn, gbc);
        this.add(menuBtn, gbc);
    }

    private JButton createButton(String text) {
        //JButton btn = new JButton(text);
        JButton btn = new BOBButton(text, Color.WHITE, Color.BLACK, Color.DARK_GRAY.darker(),Color.GRAY,16, 5);
        btn.setPreferredSize(new Dimension(150, 50));
        btn.setFocusable(false);
        return btn;
    }
}