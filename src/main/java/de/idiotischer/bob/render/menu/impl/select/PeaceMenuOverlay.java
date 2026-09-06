package de.idiotischer.bob.render.menu.impl.select;

import de.idiotischer.bob.BOB;
import de.idiotischer.bob.SharedCore;
import de.idiotischer.bob.conference.PeaceConference;
import de.idiotischer.bob.conference.TakeTileType;
import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.country.CountryManager;
import de.idiotischer.bob.render.menu.components.ModernScrollBarUI;
import de.idiotischer.bob.render.menu.components.button.BOBButton;
import de.idiotischer.bob.tile.Tile;

import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PeaceMenuOverlay extends JPanel {

    private UUID peace = null;
    private BOBButton selectedButton;

    private JPanel countryPanel;

    private BOBButton sendDemandsBtn;
    private BOBButton quitConferenceBtn;

    private JPanel winnerCountryPanel;
    private JScrollPane winnerScrollPane;

    private Country selectedWinnerCountry;
    private BOBButton selectedWinnerButton;


    private final Set<Tile> selectedTiles = new java.util.HashSet<>();
    private static boolean waiting;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Peace Menu Overlay");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(800, 600);
            frame.setLocationRelativeTo(null);

            PeaceConference c = new PeaceConference(new SharedCore(), UUID.randomUUID(), new CountryManager(), Map.of(), Map.of(), Set.of(new Country("hahha", "C1", Color.BLUE, false, false)), Set.of(new Country("hahha", "C2", Color.BLUE, false, false)));

            var o = new PeaceMenuOverlay();
            o.setPeace(c.getUUID());
            frame.add(o);

            frame.setVisible(true);
        });
    }

    public PeaceMenuOverlay() {
        setOpaque(false);
        setLayout(null);
        setFocusable(false);
        setRequestFocusEnabled(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();

        try {
            g2.setColor(Color.DARK_GRAY);
            g2.fillRect(0, 0, getWidth(), 40);

            g2.setStroke(new BasicStroke(12));
            g2.setColor(Color.DARK_GRAY.darker());
            g2.drawRect(0, 0, getWidth(), 40);

            g2.setColor(Color.DARK_GRAY);
            g2.fillRect(0, 40, 320, getHeight() - 40);

            g2.setColor(Color.DARK_GRAY.darker());
            g2.drawRect(0, 40, 320, getHeight() - 40);

            g2.setColor(Color.DARK_GRAY);
            g2.fillRect(getWidth() - 320, 40, 320, getHeight() - 40);

            g2.setColor(Color.DARK_GRAY.darker());
            g2.drawRect(getWidth() - 320, 40, 320, getHeight() - 40);
        } finally {
            g2.dispose();
        }
    }

    @Override
    public void doLayout() {
        super.doLayout();

        if (countryPanel != null) countryPanel.setBounds(10, 90, 300, Math.max(0, getHeight() - 100));

        if (winnerScrollPane != null) winnerScrollPane.setBounds(getWidth() - 310, 90, 290, Math.max(0, getHeight() - 250));

        if (sendDemandsBtn != null && quitConferenceBtn != null) {
            int rightButtonWidth = 180;
            int rightButtonHeight = 35;
            int rightMargin = 50;
            int bottomMargin = 50;
            int buttonGap = 5;

            sendDemandsBtn.setBounds(getWidth() - rightMargin - rightButtonWidth, getHeight() - bottomMargin - rightButtonHeight * 2 - buttonGap, rightButtonWidth, rightButtonHeight);
            quitConferenceBtn.setBounds(getWidth() - rightMargin - rightButtonWidth, getHeight() - bottomMargin - rightButtonHeight, rightButtonWidth, rightButtonHeight);
        }
    }


    public void setPeace(UUID peace) {
        this.peace = peace;

        start();
    }

    public void start() {
        selectedTiles.clear();
        removeAll();

        addTileTypeButtons();
        buildCountryPanel();
        addConferenceButtons();
        buildWinnerCountryPanel();

        revalidate();
        repaint();
    }

    private void buildCountryPanel() {
        countryPanel = new JPanel();
        countryPanel.setLayout(new BoxLayout(countryPanel, BoxLayout.Y_AXIS));
        countryPanel.setOpaque(false);
        countryPanel.setBounds(10, 90, 300, Math.max(0, getHeight() - 100));

        BOBButton countriesButton = new BOBButton("Countries       ▾", Color.WHITE, Color.BLACK, Color.DARK_GRAY.darker(), Color.GRAY, 14, 5);
        countriesButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        countriesButton.setMaximumSize(new Dimension(300, 35));

        JPanel countriesPanel = new JPanel();
        countriesPanel.setLayout(new BoxLayout(countriesPanel, BoxLayout.Y_AXIS));
        countriesPanel.setOpaque(false);
        countriesPanel.setVisible(false);

        countriesButton.addActionListener(e -> {
            if(waiting) return;

            countriesPanel.setVisible(!countriesPanel.isVisible());

            countriesButton.setText(countriesPanel.isVisible() ? "Countries       ▴" : "Countries       ▾");

            countriesPanel.getParent().revalidate();
            countriesPanel.getParent().repaint();
        });

        PeaceConference conf = BOB.getInstance().getPeaceHelper().getBy(peace);
        for (Country country : conf.getDefeated()) {
            BOBButton countryButton = new BOBButton(country.countryName() + "       ▾", Color.WHITE, Color.BLACK, Color.DARK_GRAY.darker(), Color.GRAY, 13, 5);

            countryButton.setAlignmentX(Component.LEFT_ALIGNMENT);
            countryButton.setMaximumSize(new Dimension(285, 35));

            JPanel tilePanel = new JPanel();
            tilePanel.setLayout(new BoxLayout(tilePanel, BoxLayout.Y_AXIS));
            tilePanel.setOpaque(false);
            tilePanel.setVisible(false);

            for (Tile tile : conf.getTakableTiles().stream().filter(t -> t.getOwner().getAbbreviation().equals(country.getAbbreviation())).toList()) {
                BOBButton tileButton = new BOBButton(tile.getAbbreviation(), tile.getVictoryPoints() + " | " + tile.getName(), Color.WHITE, Color.BLACK, Color.DARK_GRAY.darker(), Color.GRAY, 12, 5);

                tileButton.setAlignmentX(Component.LEFT_ALIGNMENT);
                tileButton.setMaximumSize(new Dimension(270, 30));

                tileButton.addActionListener(e -> {
                    if(waiting) return;

                    boolean toggled = !tileButton.isToggled();
                    tileButton.setToggled(toggled);

                    tileButton.setText(tile.getVictoryPoints() + " | " + tile.getName() + (toggled ? " x" : "  "));

                    if (toggled) selectedTiles.add(tile);
                    else selectedTiles.remove(tile);
                });


                tilePanel.add(tileButton);
            }

            countryButton.addActionListener(e -> {
                if(waiting) return;

                boolean visible = !tilePanel.isVisible();
                tilePanel.setVisible(visible);

                countryButton.setText(country.countryName() + (visible ? "       ▴" : "       ▾"));

                countryPanel.revalidate();
                countryPanel.repaint();
            });

            countriesPanel.add(countryButton);
            countriesPanel.add(tilePanel);
            countriesPanel.add(Box.createVerticalStrut(5));
        }


        countryPanel.add(countriesButton);
        countryPanel.add(countriesPanel);

        add(countryPanel);
    }

    private void buildWinnerCountryPanel() {
        winnerCountryPanel = new JPanel();
        winnerCountryPanel.setLayout(new BoxLayout(winnerCountryPanel, BoxLayout.Y_AXIS));
        winnerCountryPanel.setOpaque(false);
        winnerCountryPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        PeaceConference conf = BOB.getInstance().getPeaceHelper().getBy(peace);
        Country playerCountry = BOB.getInstance().getPlayer().country();

        for (Country country : conf.getWinners()) {
            BOBButton countryButton = new BOBButton(country.countryName(), Color.WHITE, Color.BLACK, Color.DARK_GRAY.darker(), Color.GRAY, 13, 5);

            countryButton.setAlignmentX(Component.LEFT_ALIGNMENT);
            countryButton.setMaximumSize(new Dimension(280, 35));

            countryButton.addActionListener(e -> {
                if (waiting) return;

                selectWinnerCountry(country, countryButton);
            });

            winnerCountryPanel.add(countryButton);
            winnerCountryPanel.add(Box.createVerticalStrut(5));

            if (country.getAbbreviation().equals(playerCountry.getAbbreviation())) selectWinnerCountry(country, countryButton);
        }

        winnerScrollPane = new JScrollPane(winnerCountryPanel);
        winnerScrollPane.setOpaque(false);
        winnerScrollPane.getViewport().setOpaque(false);
        winnerScrollPane.setBorder(null);
        winnerScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        winnerScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        winnerScrollPane.getVerticalScrollBar().setUI(new ModernScrollBarUI());
        winnerScrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(14, 0));
        winnerScrollPane.getVerticalScrollBar().setOpaque(false);
        winnerScrollPane.getVerticalScrollBar().setUnitIncrement(16);

        add(winnerScrollPane);
    }

    private void selectWinnerCountry(Country country, BOBButton button) {
        if (selectedWinnerButton != null) selectedWinnerButton.setSelected(false);

        selectedWinnerCountry = country;
        selectedWinnerButton = button;

        selectedWinnerButton.setSelected(true);
    }

    private void addTileTypeButtons() {
        int buttonX = 20;
        int buttonY = 55;
        int buttonWidth = 65;
        int buttonHeight = 30;
        int gap = 15;

        for (int i = 0; i < TakeTileType.values().length; i++) {
            TakeTileType tileType = TakeTileType.values()[i];

            BOBButton button = new BOBButton(tileType.name(), tileType.getDefaultCharr(), Color.WHITE, Color.BLACK, Color.DARK_GRAY.darker(), Color.GRAY, 16, 5);

            button.setBounds(buttonX + i * (buttonWidth + gap), buttonY, buttonWidth, buttonHeight);

            button.addActionListener(e -> {
                if (waiting) return;

                if (selectedButton != null) selectedButton.setSelected(false);

                selectedButton = button;
                selectedButton.setSelected(true);
            });

            add(button);

            if (tileType == TakeTileType.TAKE) {
                selectedButton = button;
                selectedButton.setSelected(true);
            }
        }
    }


    private void addConferenceButtons() {
        sendDemandsBtn = new BOBButton("Send Demands", Color.WHITE, Color.BLACK, Color.DARK_GRAY.darker(), Color.GRAY, 14, 5);
        quitConferenceBtn = new BOBButton("Quit Conference", Color.WHITE, Color.BLACK, Color.DARK_GRAY.darker(), Color.GRAY, 14, 5);

        add(sendDemandsBtn);
        add(quitConferenceBtn);

        int rightButtonWidth = 180;
        int rightButtonHeight = 35;
        int rightMargin = 50;
        int bottomMargin = 50;
        int buttonGap = 5;

        sendDemandsBtn.setBounds(getWidth() - rightMargin - rightButtonWidth, getHeight() - bottomMargin - rightButtonHeight * 2 - buttonGap, rightButtonWidth, rightButtonHeight);
        quitConferenceBtn.setBounds(getWidth() - rightMargin - rightButtonWidth, getHeight() - bottomMargin - rightButtonHeight, rightButtonWidth, rightButtonHeight);

        PeaceConference conf = BOB.getInstance().getPeaceHelper().getBy(peace);
        quitConferenceBtn.addActionListener(e -> BOB.getInstance().getPeaceHelper().quitConference(peace));
        sendDemandsBtn.addActionListener(e -> {
            if (waiting) return;

            if (selectedButton == null) return;
            if (selectedWinnerCountry == null) return;

            if (conf.hasDispute(BOB.getInstance().getPlayer().country())) addDisputedPopup();
            else BOB.getInstance().getPeaceHelper().sendDemands(peace, TakeTileType.valueOf(selectedButton.getId()), selectedTiles, selectedWinnerCountry);

            addWaitingPopup();

            waiting = true;

            start();
        });

    }

    private void addDisputedPopup() {
    }

    private void addWaitingPopup() {
    }

    public static void removeWaitingPopup() {
    }

    public static void removeDisputedPopup() {
    }

    public UUID getPeace() {
        return peace;
    }

    public static boolean isWaiting() {
        return waiting;
    }

    public static void setWaiting(boolean waiting) {
        PeaceMenuOverlay.waiting = waiting;
    }
}
