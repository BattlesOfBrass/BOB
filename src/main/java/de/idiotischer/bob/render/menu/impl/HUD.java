package de.idiotischer.bob.render.menu.impl;

import de.idiotischer.bob.BOB;
import javax.swing.*;
import java.awt.*;

public class HUD extends JPanel {
    private final JLabel countryLabel;

    public HUD() {
        this.setOpaque(false);
        this.setLayout(new FlowLayout(FlowLayout.LEFT));

        countryLabel = new JLabel("Current country: ---");
        countryLabel.setForeground(Color.GREEN);
        countryLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));

        this.add(countryLabel);
    }

    public void updateHUD() {
        String name = BOB.getInstance().getPlayer().country().countryName();
        countryLabel.setText("Current country: " + name);
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        updateHUD();
    }
}