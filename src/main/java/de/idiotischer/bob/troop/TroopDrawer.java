package de.idiotischer.bob.troop;

import java.awt.*;
import java.util.List;

//maybe move to render
public class TroopDrawer {

    public static void drawTroops(Graphics2D g2, List<TroopStack> troopStacks) {
        for (TroopStack troops : troopStacks) {
            if(!troops.isVisible()) return;
        }

    }

    public static double getScaleForView() {
        return 1;
    }
}
