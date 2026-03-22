package de.idiotischer.bob.render.menu;

import java.awt.*;
import java.awt.event.MouseEvent;

public interface Component {
    void paint(Graphics g);

    default void mouseClick(MouseEvent e, int x, int y) {

    }
    default void mouseRelease(MouseEvent e, int x, int y) {

    }
    default void mouseMove(MouseEvent e, int x, int y) {

    }
}
