package de.idiotischer.bob.render.menu.components;

import javax.swing.text.*;

public class OnlyNumberFilter extends DocumentFilter {

    @Override
    public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
            throws BadLocationException {

        if (string == null) return;

        if (string.matches("\\d+")) {
            super.insertString(fb, offset, string, attr);
        }
    }

    @Override
    public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
            throws BadLocationException {

        if (text == null) return;

        if (text.matches("\\d+")) {
            super.replace(fb, offset, length, text, attrs);
        }
    }
}