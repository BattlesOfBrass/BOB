package de.idiotischer.bob.render.menu.components;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

public class LengthLimitFilter extends DocumentFilter {

    private final int limit;

    public LengthLimitFilter(int limit) {
        this.limit = limit;
    }

    @Override
    public void insertString(FilterBypass fb, int offset, String str, AttributeSet attr) throws BadLocationException {
        if (str == null) return;

        if ((fb.getDocument().getLength() + str.length()) <= limit) {
            super.insertString(fb, offset, str, attr);
        }
    }
}
