package de.idiotischer.bob.render.menu;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

public interface Panel {
    default void mouseClick(MouseEvent e, int x, int y) {

    }
    default void mouseRelease(MouseEvent e, int x, int y) {

    }
    default void mouseMove(MouseEvent e, int x, int y) {

    }
    default void mouseScroll(MouseWheelEvent e, int x, int y) {

    }

    default void keyPress(KeyEvent e) {

    }

    default void keyRelease(KeyEvent e) {

    }
}
