package de.idiotischer.bob.render.menu.components.button;

import it.unimi.dsi.fastutil.Pair;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class ButtonGroup {
    private final Set<IButtonComp> buttons = new HashSet<>();
    private final Consumer<Pair<IButtonComp, IButtonComp>> onSelect;
    private IButtonComp selectedButton = null;

    public ButtonGroup() {
        this.onSelect = null;
    }

    public ButtonGroup(Consumer<Pair<IButtonComp, IButtonComp>> selected) {
        this.onSelect = selected;
    }

    public void add(IButtonComp button) {
        buttons.add(button);
    }

    public void select(IButtonComp button) {
        IButtonComp old = this.selectedButton;

        if (old != null) {
            old.setSelected(false);
        }

        selectedButton = button;

        if (selectedButton != null) {
            selectedButton.setSelected(true);
        }

        if(onSelect != null) onSelect.accept(Pair.of(selectedButton, old));
    }

    public IButtonComp getSelected() {
        return selectedButton;
    }
}