
package de.idiotischer.bob.render.menu.impl;

import de.idiotischer.bob.BOB;
import de.idiotischer.bob.game.GameState;
import de.idiotischer.bob.networking.packet.impl.pp.RequestPacket;
import de.idiotischer.bob.networking.packet.impl.pp.Type;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FocusesUI extends JPanel {

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

        g2.dispose();
    }
}
