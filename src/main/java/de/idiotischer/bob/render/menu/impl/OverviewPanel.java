package de.idiotischer.bob.render.menu.impl;

import de.idiotischer.bob.BOB;
import de.idiotischer.bob.tile.Tile;

import javax.swing.*;
import java.awt.*;

public class OverviewPanel extends JPanel {

    private final JLabel label;
    private Tile currentTile;

    public OverviewPanel() {
        setOpaque(false);

        label = new JLabel();
        label.setForeground(Color.WHITE);

        add(label);
    }

    public void setTile(Tile tile) {
        this.currentTile = tile;
        updateContent();
    }

    private void updateContent() {
        if (currentTile == null || currentTile.getController() == null || currentTile.getController().getPlayer() == null) {
            label.setText("");
            return;
        }

        if (currentTile.getController().getPlayer().uuid() ==
                BOB.getInstance().getPlayer().uuid()) {
            label.setText("Your country overview");
        } else {
            label.setText("Foreign country overview");
        }
    }
}