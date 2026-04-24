package de.idiotischer.bob.util;

import de.idiotischer.bob.BOB;
import de.idiotischer.bob.render.MainRenderer;
import de.idiotischer.bob.scenario.Scenario;

import javax.swing.*;
import java.io.IOException;

public class Helper {

    //TODO: this is really really unoptimized buuut bypass is faster
    //ig can cause bgus with multiplayer but if i do the order correctly in code it should always work
    public static void loadPreview(Scenario scenario, boolean bypass) {
        if(ImageUtil.isSame(scenario.getMapImage(), BOB.getInstance().getMainRenderer().getMap())) return;

        BOB.getInstance().getStateManager().setSwitchMM(false);

        BOB.getInstance().getScenarioSceneLoader().requestScenarioLoad(scenario);
    }

}
