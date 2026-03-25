package de.idiotischer.bob.render.menu.impl.select;

import de.idiotischer.bob.BOB;
import de.idiotischer.bob.render.menu.Component;
import de.idiotischer.bob.render.menu.components.button.ButtonComp;
import de.idiotischer.bob.render.menu.components.ScrollContainer;
import de.idiotischer.bob.render.menu.components.button.ButtonGroup;
import de.idiotischer.bob.render.menu.components.button.IButtonComp;
import de.idiotischer.bob.scenario.Scenario;
import de.idiotischer.bob.util.ImageUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ScenarioSelectMenu extends SelectMenu {

    private Set<Scenario> scenarios;
    private final ScrollContainer scroller;
    private Scenario selectedScenario = null;

    private final ButtonGroup scrollGroup = new ButtonGroup((pair) -> {
        IButtonComp newButton = pair.left();
        String id = newButton.getId();

        if(id.isEmpty()) return;

        Scenario scenario = BOB.getInstance().getScenarioManager().getScenario(id);

        if(scenario == null) return;

        selectedScenario = scenario;
    });

    private final ButtonComp nextMenuButton = new ButtonComp("Next", Color.WHITE, Color.DARK_GRAY.darker(),true,320,-208,95,28, 16,16, 15, Color.DARK_GRAY.brighter(), Color.BLACK, true, (b) -> {
        if(selectedScenario == null) return;

        BOB.getInstance().getScenarioSceneLoader().load(selectedScenario, true);
    });

    private final ButtonComp backMenuButton = new ButtonComp("Back to Menu", Color.WHITE, Color.DARK_GRAY.darker(),true,0,-210,138,28, 16,16, 15, Color.DARK_GRAY.brighter(), Color.BLACK, true, (b) -> {
        BOB.getInstance().getMainRenderer().getMenuPanel().setInScenarioSelect(false);
    });

    private final ButtonComp buttonSoThatItLooksBetter = new ButtonComp("Placeholder", Color.WHITE, Color.DARK_GRAY.darker(),true,-320,-208,120,28, 16,16, 15, Color.DARK_GRAY.brighter(), Color.BLACK, true, (b) -> {
        System.out.println("clicked placeholder");
    });

    List<Component> components = new ArrayList<>();

    public ScenarioSelectMenu(JPanel panel, int layoutScaleX, int layoutScaleY) {
        super(panel, layoutScaleX, layoutScaleY);

        scroller = new ScrollContainer(panel, new Color(200, 200, 200, 180), true);

        selectedScenario = BOB.getInstance().getScenarioSceneLoader().getCurrentScenario();

        reload();

        int x = parent.getWidth() / 2 - (layoutScaleX / 2) + 20;
        int y = parent.getHeight() / 2 - (layoutScaleY / 2) + 20;
        int width = layoutScaleX - 40;
        int height = layoutScaleY - 40;

        scroller.setBounds(new Rectangle(x, y, width, height));

        components.add(scroller);
        //button muss drüber deswegen nach scroller
        components.add(nextMenuButton);
        components.add(backMenuButton);
        components.add(buttonSoThatItLooksBetter);
    }

    public int getLayoutScaleX() {
        return layoutScaleX;
    }

    public int getLayoutScaleY() {
        return layoutScaleY;
    }

    public JPanel getParent() {
        return parent;
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);

        g.setColor(Color.DARK_GRAY);

        Graphics2D g2 = (Graphics2D) g;
        //center logic so für alles übernehmen
        int x = parent.getWidth() / 2 - (layoutScaleX / 2);
        int y = parent.getHeight() / 2 - (layoutScaleY / 2);

        g2.setStroke(new BasicStroke(16));

        components.forEach(component -> {
            if(component instanceof ButtonComp c) {
                c.setPanel(parent);
                c.paint(g);}
        });

        components.forEach(component -> {
            if(!(component instanceof ButtonComp)) {component.paint(g);}
        });

        drawImageFrame(g2);
    }

    public void drawImageFrame(Graphics2D g2) {
        int heightShrink = 40;

        Rectangle bounds = scroller.getActualBounds();

        int x = scroller.isCentered() ? (scroller.getPanel().getWidth() - bounds.width) / 2 : bounds.x;
        int y = scroller.isCentered() ? (scroller.getPanel().getHeight() - bounds.height) / 2 : bounds.y;

        int imgFrameWidth = 350;
        int frameX = x + bounds.width - imgFrameWidth - 2;
        int bufferX = heightShrink / 2;

        g2.setStroke(new BasicStroke(3)); //thickness of the img frame
        g2.drawRoundRect(
                frameX - bufferX,
                y + (heightShrink / 2),
                imgFrameWidth,
                bounds.height - heightShrink,
                24,
                24
        );

        if(selectedScenario == null) return;
        if(selectedScenario.getMapImage() == null) return;

        int width = selectedScenario.getMapImage().getWidth() / 4;
        int height = selectedScenario.getMapImage().getHeight() / 4;


        g2.setStroke(new BasicStroke(5)); //thickness of the img frame
        g2.drawRoundRect(x + 405, y + 40, width, height, 24, 24);

        g2.drawImage(ImageUtil.makeRoundedCorner(selectedScenario.getMapImage(), 100), x + 405, y + 40, width,height,null);
    }


    @Override
    public void keyPress(KeyEvent e) {
        if(e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            BOB.getInstance().getMainRenderer().getMenuPanel().setInScenarioSelect(false);
        }
    }

    @Override
    public void mouseClick(MouseEvent e, int x, int y) {
        components.forEach(component -> component.mouseClick(e, x, y));
    }

    @Override
    public void mouseRelease(MouseEvent e, int x, int y) {
        components.forEach(component -> component.mouseRelease(e, x, y));
    }

    @Override
    public void mouseMove(MouseEvent e, int x, int y) {
        components.forEach(component -> component.mouseMove(e, x, y));
    }

    @Override
    public void mouseScroll(MouseWheelEvent e, int x, int y) {
        components.forEach(component -> component.mouseScroll(e, x, y));
    }

    public void reload() {
        this.scenarios = BOB.getInstance().getScenarioManager().getScenarios();

        List<ButtonComp> buttons = new ArrayList<>();
        for (Scenario s : scenarios) {
            //TODO: make it so i dont need to trial and error with these values (bs but gonna leave this todo in anyways)
            ButtonComp b = new ButtonComp(
                    s.getAbbreviation(),
                    s.getName(),
                    Color.WHITE,
                    Color.BLACK,
                    false,
                    -200, 25,
                    300, 40,
                    16, 16,
                    3, Color.LIGHT_GRAY,
                    Color.DARK_GRAY,
                    true, scrollGroup, (_) -> {});
            b.setPanel(parent);
            buttons.add(b);

            if (s.equals(selectedScenario)) {
                scrollGroup.select(b);
            }
        }

        scroller.setChildren(new ArrayList<>(buttons));
    }

    public Set<Scenario> getScenarios() {
        return scenarios;
    }
}
