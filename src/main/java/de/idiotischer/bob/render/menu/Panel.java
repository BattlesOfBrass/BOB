package de.idiotischer.bob.render.menu;

import org.jetbrains.annotations.ApiStatus;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

@ApiStatus.Obsolete
public interface Panel {

    @ApiStatus.Obsolete
    default void mouseClick(MouseEvent e, int x, int y) {

    }

    @ApiStatus.Obsolete
    default void mouseRelease(MouseEvent e, int x, int y) {

    }

    @ApiStatus.Obsolete
    default void mouseMove(MouseEvent e, int x, int y) {

    }
    @ApiStatus.Obsolete
    default void mouseScroll(MouseWheelEvent e, int x, int y) {

    }

    @ApiStatus.Obsolete
    default void keyPress(KeyEvent e) {

    }

    @ApiStatus.Obsolete
    default void keyRelease(KeyEvent e) {

    }
}
