package de.idiotischer.bob.render.menu.impl.select;

import de.idiotischer.bob.BOB;
import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.render.menu.Component;
import de.idiotischer.bob.render.menu.components.button.*;
import de.idiotischer.bob.render.menu.components.button.ButtonGroup;
import de.idiotischer.bob.scenario.Scenario;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class CountrySelectMenu extends SelectMenu {

    private Scenario selectedScenario;
    private Country selectedCountry;

    private ButtonRow row;

    private final List<IButtonComp> iButtonComps = new ArrayList<>();
    private final List<Component> other = new ArrayList<>();

    private final ButtonGroup buttonGroup;

    private final ButtonComp startButton = new ButtonComp(
            "Start", Color.WHITE, Color.DARK_GRAY.darker(), true,
            320, -208, 95, 28,
            16, 16, 15,
            Color.DARK_GRAY.brighter(), Color.BLACK,
            true, (b) -> {

        if (selectedScenario == null || selectedCountry == null) return;

        BOB.getInstance().getScenarioSceneLoader().load(selectedScenario, true);
        BOB.getInstance().getPlayer().country(selectedCountry);
    });

    private final ButtonComp backMenuButton = new ButtonComp(
            "Back to Menu", Color.WHITE, Color.DARK_GRAY.darker(), true,
            0, -210, 138, 28,
            16, 16, 15,
            Color.DARK_GRAY.brighter(), Color.BLACK,
            true, (b) -> {
        BOB.getInstance().getMainRenderer().getMenuPanel().setInScenarioSelect(false);
    });

    private final ButtonComp backButton = new ButtonComp(
            "Scenario", Color.WHITE, Color.DARK_GRAY.darker(), true,
            -320, -208, 120, 28,
            16, 16, 15,
            Color.DARK_GRAY.brighter(), Color.BLACK,
            true, (b) -> {
        if (selectedScenario == null) return;

        BOB.getInstance().getMainRenderer().getMenuPanel()
                .setScenarioSelectMenu(new ScenarioSelectMenu(selectedScenario, parent, layoutScaleX, layoutScaleY));
    });

    public CountrySelectMenu(Scenario selected, JPanel panel, int layoutScaleX, int layoutScaleY) {
        super(panel, layoutScaleX, layoutScaleY);

        this.selectedScenario = selected;

        other.add(startButton);
        other.add(backMenuButton);
        other.add(backButton);

        buttonGroup = new ButtonGroup((pair) -> {
            IButtonComp btn = pair.left();
            if (btn == null) return;

            String id = btn.getId();
            if (id == null || id.isEmpty()) return;

            selectedCountry = BOB.getInstance().getCountries().getCountry(id);
        });

        reload();
    }

    public void reload() {
        iButtonComps.clear();

        for (int i = 0; i < 6; i++) {
            iButtonComps.add(createButton());
        }

        List<Country> cs = BOB.getInstance().getCountries().getOnSelectScreen();

        int limit = Math.min(iButtonComps.size(), cs.size());

        for (int i = 0; i < limit; i++) {
            IButtonComp btn = iButtonComps.get(i);
            Country c = cs.get(i);

            if (!(btn instanceof ImageButtonComp comp)) continue;

            if(c == null) continue;

            comp.setId(c.getAbbreviation());

            //eh nur placeholder
            if(c.countryName().length() > 11) comp.setText(c.getAbbreviation());
            else comp.setText(c.countryName());

            if (c.getFlagImage() != null) {
                comp.setImage(c.getFlagImage());
            } else {
                comp.setImage(null);
            }
        }

        IButtonComp btn = iButtonComps.get(ThreadLocalRandom.current().nextInt(iButtonComps.size()));

        buttonGroup.select(btn);

        buttonGroup.set(iButtonComps);

        if (row == null) row = new ButtonRow(this.parent, Color.WHITE, true);
        row.setSpacing(10);

        row.setChildren(iButtonComps);
    }

    public IButtonComp createButton() {
        ImageButtonComp b = new ImageButtonComp(
                "",
                "",
                Color.WHITE,
                Color.BLACK,
                false,
                -335, 85,
                120, 200,
                16, 16,
                3, Color.LIGHT_GRAY,
                null,
                true,
                buttonGroup,
                (_) -> {}
        );

        b.setUseImgHeight(false);
        b.setImgHeight(70);
        b.setImgWidth(110);
        b.setImgOffsetX(5);
        b.setImgOffsetY(5);

        b.setPanel(parent);
        return b;
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int x = parent.getWidth() / 2 - (layoutScaleX / 2);
        int y = parent.getHeight() / 2 - (layoutScaleY / 2);

        g2.setStroke(new BasicStroke(8));
        g2.setColor(Color.DARK_GRAY.darker());

        int yMod = -235;

        g2.drawLine(x,y - yMod,x + layoutScaleX,y - yMod);

        row.paint(g2);

        other.forEach(component -> {
            if (component instanceof IButtonComp c) {
                c.setPanel(parent);
                c.paint(g);
            }
        });
    }

    @Override
    public void mouseClick(MouseEvent e, int x, int y) {
        row.mouseClick(e, x, y);
        other.forEach(component -> component.mouseClick(e, x, y));
    }

    @Override
    public void mouseRelease(MouseEvent e, int x, int y) {
        row.mouseRelease(e, x, y);
        other.forEach(component -> component.mouseRelease(e, x, y));
    }

    @Override
    public void mouseMove(MouseEvent e, int x, int y) {
        row.mouseMove(e, x, y);
        other.forEach(component -> component.mouseMove(e, x, y));
    }

    @Override
    public void mouseScroll(MouseWheelEvent e, int x, int y) {
        row.mouseScroll(e, x, y);
        other.forEach(component -> component.mouseScroll(e, x, y));
    }

    @Override
    public void keyPress(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            BOB.getInstance().getMainRenderer().getMenuPanel().setInScenarioSelect(false);
        }
    }
}