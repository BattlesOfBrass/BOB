package de.idiotischer.bob.render.menu.impl.select;

import de.idiotischer.bob.BOB;
import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.render.menu.components.ModernScrollBarUI;
import de.idiotischer.bob.render.menu.components.button.BOBButton;
import de.idiotischer.bob.render.menu.components.button.BOBImageButton;
import de.idiotischer.bob.scenario.Scenario;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class CountrySelectMenu extends JPanel {

    private final Scenario scenario;
    private Country selectedCountry;
    private final JPanel featuredPanel;
    private final JPanel listPanel;
    private final ButtonGroup group = new ButtonGroup();

    private final int layoutScaleX = 850;
    private final int layoutScaleY = 500;

    public CountrySelectMenu(Scenario scenario) {
        this.scenario = scenario;
        this.setOpaque(false);
        this.setLayout(null);
        this.setPreferredSize(new Dimension(layoutScaleX, layoutScaleY));

        featuredPanel = new JPanel(new GridLayout(1, 6, 15, 0));
        featuredPanel.setOpaque(false);
        featuredPanel.setBounds(40, 40, layoutScaleX - 80, 180);

        listPanel = new JPanel(new FlowLayout(FlowLayout.LEFT/*CENTER*/, 10, 10));
        listPanel.setOpaque(false);

        JScrollPane scroller = new JScrollPane(listPanel);
        scroller.setOpaque(false);
        scroller.getViewport().setOpaque(false);
        scroller.setBorder(null);

        scroller.getHorizontalScrollBar().setUI(new ModernScrollBarUI());
        scroller.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 14));
        scroller.getHorizontalScrollBar().setOpaque(false);

        scroller.setBounds(40, 280, layoutScaleX - 80, 100);

        int bottomY = layoutScaleY - 60;

        JButton backBtn = createButton("Back", 120, 40);
        backBtn.setBounds(40, bottomY, 120, 40);
        backBtn.addActionListener(e ->
                BOB.getInstance().getMainRenderer().getMenuPanel()
                        .setScenarioSelectMenu(new ScenarioSelectMenu(scenario))
        );

        JButton startBtn = createButton("Start Game", 140, 40);
        startBtn.setBounds(layoutScaleX - 180, bottomY, 140, 40);
        startBtn.addActionListener(e -> {
            if (selectedCountry != null) {
                BOB.getInstance().getScenarioSceneLoader().load(scenario, true);
                BOB.getInstance().getPlayer().country(selectedCountry);
            }
        });

        this.add(featuredPanel);
        this.add(scroller);
        this.add(backBtn);
        this.add(startBtn);

        reload();
    }

    private void reload() {
        featuredPanel.removeAll();
        listPanel.removeAll();

        List<Country> featured = BOB.getInstance().getCountryManager().getOnSelectScreen();
        List<Country> all = BOB.getInstance().getCountryManager().getCountries();

        if (selectedCountry == null && !featured.isEmpty()) {
            selectedCountry = featured.get(ThreadLocalRandom.current().nextInt(featured.size()));
        }

        for (Country c : featured) {
            BOBImageButton btn = new BOBImageButton(
                    c.getAbbreviation(),
                    c.countryName().length() > 9 ? c.getAbbreviation() : c.countryName(),
                    c.getFlagImage(),
                    Color.WHITE,
                    Color.DARK_GRAY,
                    Color.DARK_GRAY.darker(),
                    Color.LIGHT_GRAY,
                    16,
                    5
            );

            btn.setPreferredSize(new Dimension(130, 160));
            btn.setFocusable(false);

            configureButton(btn, c);

            if (c.equals(selectedCountry)) btn.setSelected(true);
            featuredPanel.add(btn);
        }

        for (Country c : all) {
            if (featured.contains(c)) continue;

            BOBButton btn = createButton(c.getAbbreviation(), 100, 40);

            configureButton(btn, c);

            if (c.equals(selectedCountry)) btn.setSelected(true);
            listPanel.add(btn);
        }

        revalidate();
        //repaint();
    }

    private void configureButton(AbstractButton btn, Country c) {
        group.add(btn);
        btn.addActionListener(e -> {
            java.util.Enumeration<AbstractButton> en = group.getElements();
            while (en.hasMoreElements()) {
                en.nextElement().setSelected(false);
            }

            selectedCountry = c;
            btn.setSelected(true);
            //repaint();
        });
    }

    private BOBButton createButton(String text, int width, int height) {
        BOBButton btn = new BOBButton(
                text,
                Color.WHITE,
                Color.BLACK,
                Color.DARK_GRAY.darker(),
                Color.LIGHT_GRAY,
                16,
                5
        );
        btn.setPreferredSize(new Dimension(width, height));
        btn.setFocusable(false);
        return btn;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(Color.DARK_GRAY);
        g2.fillRoundRect(4, 4, getWidth() - 8, getHeight() - 8, 30, 30);

        g2.setStroke(new BasicStroke(8));
        g2.setColor(Color.DARK_GRAY.darker());
        g2.drawRoundRect(4, 4, getWidth() - 8, getHeight() - 8, 30, 30);

        g2.dispose();
    }
}