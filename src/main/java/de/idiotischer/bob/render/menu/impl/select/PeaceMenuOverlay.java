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
import java.util.ArrayList;
import java.util.List;
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

    private JScrollPane countryScrollPane;

    private final Set<Tile> selectedTiles = new java.util.HashSet<>();
    private static boolean waiting;

    private static JPanel waitingPopupOverlay;
    private static JPanel disputedPopupOverlay;

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

        Color dark = BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().palette().darkColor();

        try {
            g2.setColor(dark);
            g2.fillRect(0, 0, getWidth(), 40);

            g2.setStroke(new BasicStroke(12));
            g2.setColor(dark.darker());
            g2.drawRect(0, 0, getWidth(), 40);

            g2.setColor(dark);
            g2.fillRect(0, 40, 320, getHeight() - 40);

            g2.setColor(dark.darker());
            g2.drawRect(0, 40, 320, getHeight() - 40);

            g2.setColor(dark);
            g2.fillRect(getWidth() - 320, 40, 320, getHeight() - 40);

            g2.setColor(dark.darker());
            g2.drawRect(getWidth() - 320, 40, 320, getHeight() - 40);
        } finally {
            g2.dispose();
        }
    }

    @Override
    public void doLayout() {
        super.doLayout();

        if (countryScrollPane != null) countryScrollPane.setBounds(10, 90, 300, Math.max(0, getHeight() - 100));

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
        countryPanel = new JPanel() {
            @Override
            public Dimension getPreferredSize() {
                Dimension size = super.getPreferredSize();
                return new Dimension(290, size.height);
            }
        };
        countryPanel.setLayout(new BoxLayout(countryPanel, BoxLayout.Y_AXIS));
        countryPanel.setOpaque(false);
        countryPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        BOBButton countriesButton = new BOBButton("Countries       ▾", BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().button().textColor(), BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().button().bgColor(), BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().button().borderColor(), BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().button().borderColorHover(), 14, 5);
        countriesButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        countriesButton.setMaximumSize(new Dimension(290, 35));

        JPanel countriesPanel = new JPanel();
        countriesPanel.setLayout(new BoxLayout(countriesPanel, BoxLayout.Y_AXIS));
        countriesPanel.setOpaque(false);
        countriesPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        countriesPanel.setVisible(false);

        countriesButton.addActionListener(e -> {
            if (waiting) return;

            countriesPanel.setVisible(!countriesPanel.isVisible());
            countriesButton.setText(countriesPanel.isVisible() ? "Countries       ▴" : "Countries       ▾");

            countryPanel.revalidate();
            countryPanel.repaint();
        });

        PeaceConference conf = BOB.getInstance().getPeaceHelper().getBy(peace);
        for (Country country : conf.getDefeated()) {
            JPanel countryWrapper = new JPanel();
            countryWrapper.setLayout(new BoxLayout(countryWrapper, BoxLayout.X_AXIS));
            countryWrapper.setOpaque(false);
            countryWrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
            countryWrapper.add(Box.createRigidArea(new Dimension(10, 0)));

            BOBButton countryButton = new BOBButton(country.countryName() + "       ▾", BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().button().textColor(), BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().button().bgColor(), BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().button().borderColor(), BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().button().borderColorHover(), 13, 5);
            countryButton.setAlignmentX(Component.LEFT_ALIGNMENT);
            countryButton.setMaximumSize(new Dimension(270, 35));
            countryWrapper.add(countryButton);

            JPanel tilePanel = new JPanel();
            tilePanel.setLayout(new BoxLayout(tilePanel, BoxLayout.Y_AXIS));
            tilePanel.setOpaque(false);
            tilePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            tilePanel.setVisible(false);

            java.util.List<Tile> countryTiles = conf.getTakableTiles().stream().filter(t -> t.getOwner().getAbbreviation().equals(country.getAbbreviation())).toList();

            List<BOBButton> tileButtons = new ArrayList<>();

            if (!countryTiles.isEmpty()) {
                JPanel selectAllWrapper = new JPanel();
                selectAllWrapper.setLayout(new BoxLayout(selectAllWrapper, BoxLayout.X_AXIS));
                selectAllWrapper.setOpaque(false);
                selectAllWrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
                selectAllWrapper.add(Box.createRigidArea(new Dimension(25, 0))); // 25px Indentation

                BOBButton selectAllButton = new BOBButton("Select All", BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().button().textColor(), BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().button().bgColor(), BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().button().borderColor(), BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().button().borderColorHover(), 12, 5);
                selectAllButton.setAlignmentX(Component.LEFT_ALIGNMENT);
                selectAllButton.setMaximumSize(new Dimension(245, 30));

                selectAllButton.addActionListener(e -> {
                    if (waiting) return;

                    boolean selectAll = tileButtons.stream().anyMatch(btn -> !btn.isToggled());

                    for (int i = 0; i < countryTiles.size(); i++) {
                        Tile tile = countryTiles.get(i);
                        BOBButton tileBtn = tileButtons.get(i);

                        tileBtn.setToggled(selectAll);
                        tileBtn.setText(tile.getVictoryPoints() + " | " + tile.getName() + (selectAll ? " x" : "  "));

                        if (selectAll) {
                            selectedTiles.add(tile);
                        } else {
                            selectedTiles.remove(tile);
                        }
                    }
                });

                selectAllWrapper.add(selectAllButton);
                tilePanel.add(selectAllWrapper);
                tilePanel.add(Box.createVerticalStrut(3));
            }

            for (Tile tile : countryTiles) {
                JPanel tileWrapper = new JPanel();
                tileWrapper.setLayout(new BoxLayout(tileWrapper, BoxLayout.X_AXIS));
                tileWrapper.setOpaque(false);
                tileWrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
                tileWrapper.add(Box.createRigidArea(new Dimension(25, 0))); // 25px Indentation

                BOBButton tileButton = new BOBButton(tile.getAbbreviation(), tile.getVictoryPoints() + " | " + tile.getName(), BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().button().textColor(), BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().button().bgColor(), BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().button().borderColor(), BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().button().borderColorHover(), 12, 5);
                tileButton.setAlignmentX(Component.LEFT_ALIGNMENT);
                tileButton.setMaximumSize(new Dimension(245, 30));

                tileButton.addActionListener(e -> {
                    if (waiting) return;

                    boolean toggled = !tileButton.isToggled();
                    tileButton.setToggled(toggled);

                    tileButton.setText(tile.getVictoryPoints() + " | " + tile.getName() + (toggled ? " x" : "  "));

                    if (toggled) selectedTiles.add(tile);
                    else selectedTiles.remove(tile);
                });

                tileButtons.add(tileButton);
                tileWrapper.add(tileButton);

                tilePanel.add(tileWrapper);
                tilePanel.add(Box.createVerticalStrut(2));
            }

            countryButton.addActionListener(e -> {
                if (waiting) return;

                boolean visible = !tilePanel.isVisible();
                tilePanel.setVisible(visible);

                countryButton.setText(country.countryName() + (visible ? "       ▴" : "       ▾"));

                countryPanel.revalidate();
                countryPanel.repaint();
            });

            countriesPanel.add(countryWrapper);
            countriesPanel.add(tilePanel);
            countriesPanel.add(Box.createVerticalStrut(5));
        }

        countryPanel.add(countriesButton);
        countryPanel.add(countriesPanel);

        countryScrollPane = new JScrollPane(countryPanel);
        countryScrollPane.setOpaque(false);
        countryScrollPane.getViewport().setOpaque(false);
        countryScrollPane.setBorder(null);
        countryScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        countryScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        countryScrollPane.getVerticalScrollBar().setUI(new ModernScrollBarUI());
        countryScrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(14, 0));
        countryScrollPane.getVerticalScrollBar().setOpaque(false);
        countryScrollPane.getVerticalScrollBar().setUnitIncrement(16);

        add(countryScrollPane);
    }

    private void buildWinnerCountryPanel() {
        winnerCountryPanel = new JPanel();
        winnerCountryPanel.setLayout(new BoxLayout(winnerCountryPanel, BoxLayout.Y_AXIS));
        winnerCountryPanel.setOpaque(false);
        winnerCountryPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        PeaceConference conf = BOB.getInstance().getPeaceHelper().getBy(peace);
        Country playerCountry = BOB.getInstance().getPlayer().country();

        for (Country country : conf.getWinners()) {
            BOBButton countryButton = new BOBButton(country.countryName(), BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().button().textColor(), BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().button().bgColor(), BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().button().borderColor(), BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().button().borderColorHover(), 13, 5);

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

            BOBButton button = new BOBButton(tileType.name(), tileType.getDefaultCharr(), BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().button().textColor(), BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().button().bgColor(), BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().button().borderColor(), BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().button().borderColorHover(), 16, 5);

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
        sendDemandsBtn = new BOBButton("Send Demands", BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().button().textColor(), BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().button().bgColor(), BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().button().borderColor(), BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().button().borderColorHover(), 14, 5);
        quitConferenceBtn = new BOBButton("Quit Conference", BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().button().textColor(), BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().button().bgColor(), BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().button().borderColor(), BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().button().borderColorHover(), 14, 5);

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
        removeDisputedPopup();

        disputedPopupOverlay = createHoi4PopupOverlay("DISPUTED CLAIMS", "Another victor has submitted overlapping demands for these territories. Resolve claims or increase offer point weight before submitting.", "Acknowledge", e -> removeDisputedPopup());

        add(disputedPopupOverlay);
        setComponentZOrder(disputedPopupOverlay, 0);
        revalidate();
        repaint();
    }

    private void addWaitingPopup() {
        removeWaitingPopup();

        waitingPopupOverlay = createHoi4PopupOverlay("CONFERENCE IN PROGRESS", "Waiting for other victorious nations to submit their demands...", null, null);

        add(waitingPopupOverlay);
        setComponentZOrder(waitingPopupOverlay, 0);
        revalidate();
        repaint();
    }

    public static void removeWaitingPopup() {
        if (waitingPopupOverlay != null && waitingPopupOverlay.getParent() != null) {
            Container parent = waitingPopupOverlay.getParent();
            parent.remove(waitingPopupOverlay);
            waitingPopupOverlay = null;
            parent.revalidate();
            parent.repaint();
        }
    }

    public static void removeDisputedPopup() {
        if (disputedPopupOverlay != null && disputedPopupOverlay.getParent() != null) {
            Container parent = disputedPopupOverlay.getParent();
            parent.remove(disputedPopupOverlay);
            disputedPopupOverlay = null;
            parent.revalidate();
            parent.repaint();
        }
    }

    private JPanel createHoi4PopupOverlay(String title, String message, String buttonText, java.awt.event.ActionListener buttonListener) {
        JPanel overlay = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(0, 0, 0, 160));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        overlay.setOpaque(false);
        overlay.setBounds(0, 0, getWidth(), getHeight());

        JPanel dialog = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(new Color(35, 38, 41));
                g2.fillRect(0, 0, getWidth(), getHeight());

                g2.setColor(new Color(25, 27, 29));
                g2.fillRect(0, 0, getWidth(), 36);

                g2.setColor(new Color(110, 115, 120));
                g2.setStroke(new BasicStroke(2));
                g2.drawRect(1, 1, getWidth() - 3, getHeight() - 3);

                g2.setColor(new Color(55, 60, 65));
                g2.drawRect(4, 4, getWidth() - 9, getHeight() - 9);

                g2.setColor(new Color(90, 95, 100));
                g2.drawLine(4, 36, getWidth() - 5, 36);

                g2.dispose();
            }
        };

        dialog.setLayout(new BorderLayout());
        dialog.setPreferredSize(new Dimension(420, 200));
        dialog.setOpaque(false);
        dialog.setBorder(BorderFactory.createEmptyBorder(8, 12, 12, 12));

        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        titleLabel.setForeground(new Color(220, 220, 220));
        titleLabel.setPreferredSize(new Dimension(400, 28));

        JTextArea messageArea = new JTextArea(message);
        messageArea.setFont(new Font("SansSerif", Font.PLAIN, 12));
        messageArea.setForeground(new Color(190, 195, 200));
        messageArea.setOpaque(false);
        messageArea.setEditable(false);
        messageArea.setFocusable(false);
        messageArea.setWrapStyleWord(true);
        messageArea.setLineWrap(true);
        messageArea.setBorder(BorderFactory.createEmptyBorder(15, 10, 10, 10));

        dialog.add(titleLabel, BorderLayout.NORTH);
        dialog.add(messageArea, BorderLayout.CENTER);

        if (buttonText != null && buttonListener != null) {
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            buttonPanel.setOpaque(false);

            BOBButton okBtn = new BOBButton(buttonText, BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().button().textColor(), BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().button().bgColor(), BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().button().borderColor(), BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().button().borderColorHover(), 13, 5);
            okBtn.setPreferredSize(new Dimension(140, 32));
            okBtn.addActionListener(buttonListener);

            buttonPanel.add(okBtn);
            dialog.add(buttonPanel, BorderLayout.SOUTH);
        }

        overlay.add(dialog);
        return overlay;
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
