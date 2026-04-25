package de.idiotischer.bob.render.menu.impl;

import de.idiotischer.bob.BOB;
import de.idiotischer.bob.state.State;

import javax.swing.*;
import java.awt.*;

public class OverviewPanel extends JPanel {

    private final JLabel label;
    private State currentState;

    public OverviewPanel() {
        setOpaque(false);

        label = new JLabel();
        label.setForeground(Color.WHITE);

        add(label);
    }

    public void setState(State state) {
        this.currentState = state;
        updateContent();
    }

    private void updateContent() {
        if (currentState == null || currentState.getController() == null || currentState.getController().getPlayer() == null) {
            label.setText("");
            return;
        }

        if (currentState.getController().getPlayer().uuid() ==
                BOB.getInstance().getPlayer().uuid()) {
            label.setText("Your country overview");
        } else {
            label.setText("Foreign country overview");
        }
    }
}