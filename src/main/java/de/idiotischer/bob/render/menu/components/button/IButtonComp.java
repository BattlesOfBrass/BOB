package de.idiotischer.bob.render.menu.components.button;

import de.idiotischer.bob.render.menu.Component;

public interface IButtonComp extends Component {
    boolean isSelected();
    void setSelected(boolean selected);
    String getId();
}
