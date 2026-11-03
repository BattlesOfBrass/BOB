package de.idiotischer.bob.render.menu.impl;

import de.idiotischer.bob.BOB;
import de.idiotischer.bob.networking.packet.impl.pp.RequestPacket;
import de.idiotischer.bob.networking.packet.impl.pp.Type;
import de.idiotischer.bob.render.menu.components.*;
import de.idiotischer.bob.render.menu.components.button.BOBButton;
import de.idiotischer.bob.tile.Tile;
import de.idiotischer.bob.troop.TrainEntry;
import de.idiotischer.bob.util.MilAccessRules;
import it.unimi.dsi.fastutil.Pair;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.StrokeBorder;
import javax.swing.text.DefaultFormatter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class HUD extends JPanel {
    private boolean isVisible = false;
    private Tile currentTile = null;
    private final HUDTopBar topBar;
    private final FocusesUI focusesUI;

    private final JPanel sidePanel;
    private final JTabbedPane tabbedPane;
    private final JButton closeBtn;
    private int panelWidth = 300;
    private boolean showingOwn;
    private JPanel trainingList;

    private TabMode currentMode = TabMode.NONE;

    public HUD() {
        setOpaque(false);
        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));

        topPanel.setOpaque(false);
        setFocusable(false);
        setRequestFocusEnabled(false);

        topBar = new HUDTopBar();
        topPanel.add(topBar);

        focusesUI = new FocusesUI();
        focusesUI.setVisible(false);

        Color dark = BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().palette().darkColor();

        JPanel verticalLine = new JPanel();
        verticalLine.setBackground(BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().palette().darkColor().darker());
        verticalLine.setOpaque(true);
        verticalLine.setMaximumSize(new Dimension(4, getHeight() + 1));
        verticalLine.setPreferredSize(new Dimension(4, getHeight() + 1));
        verticalLine.setMinimumSize(new Dimension(4, getHeight() + 1));
        verticalLine.setAlignmentX(Component.RIGHT_ALIGNMENT);
        verticalLine.setFocusable(false);

        sidePanel = new JPanel();
        sidePanel.setBackground(dark);
        sidePanel.setLayout(new BorderLayout());
        sidePanel.setPreferredSize(new Dimension(panelWidth, 0));

        tabbedPane = new ModernTabbedPane();
        tabbedPane.addTab("Overview", createOverviewPanel());
        tabbedPane.addTab("Industry", createIndustryPanel());

        tabbedPane.setFocusable(false);

        sidePanel.add(verticalLine, BorderLayout.EAST);
        sidePanel.add(tabbedPane, BorderLayout.CENTER);

        closeBtn = new JButton("×");
        closeBtn.setFocusable(false);
        closeBtn.setBorderPainted(false);
        closeBtn.setContentAreaFilled(false);
        closeBtn.setOpaque(false);
        closeBtn.setForeground(Color.WHITE);
        closeBtn.setFont(closeBtn.getFont().deriveFont(Font.BOLD, 24f));
        closeBtn.setMargin(new Insets(0, 0, 0, 0));

        closeBtn.setPreferredSize(new Dimension(20, 20));
        closeBtn.setMinimumSize(new Dimension(20, 20));
        closeBtn.setMaximumSize(new Dimension(20, 20));

        closeBtn.addActionListener(e -> visible(false));
        closeBtn.addActionListener(e -> visible(false));

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setOpaque(false);

        JPanel closePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        closePanel.setOpaque(false);
        closePanel.add(closeBtn);

        contentPanel.add(closePanel, BorderLayout.NORTH);
        contentPanel.add(tabbedPane, BorderLayout.CENTER);

        sidePanel.add(contentPanel, BorderLayout.CENTER);
        sidePanel.add(verticalLine, BorderLayout.EAST);

        sidePanel.setVisible(false);

        add(topPanel, BorderLayout.NORTH);
        add(sidePanel, BorderLayout.WEST);
        add(focusesUI, BorderLayout.CENTER);
    }

    public void updateHUD() {
        if(BOB.getInstance().getPlayer() == null) return;
        if(BOB.getInstance().getPlayer().country() == null) return;
        String name = BOB.getInstance().getPlayer().country().countryName();
        topBar.setCountryName(name);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        updateHUD();
    }

    private JPanel createOverviewPanel() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setFocusable(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JPanel horizontalLine1 = new JPanel();
        horizontalLine1.setBackground(BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().palette().darkColor().darker());
        horizontalLine1.setOpaque(true);
        horizontalLine1.setMaximumSize(new Dimension(panelWidth, 4));
        horizontalLine1.setPreferredSize(new Dimension(panelWidth, 4));
        horizontalLine1.setMinimumSize(new Dimension(panelWidth, 4));
        horizontalLine1.setAlignmentX(Component.CENTER_ALIGNMENT);
        horizontalLine1.setFocusable(false);
        panel.add(horizontalLine1);

        JPanel flagPanel = new JPanel(new BorderLayout());
        flagPanel.setOpaque(false);
        flagPanel.setMaximumSize(new Dimension(140, 90));
        flagPanel.setPreferredSize(new Dimension(140, 90));
        flagPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        flagPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(180, 180, 180), 2), BorderFactory.createEmptyBorder(3, 3, 3, 3)));

        JLabel flagLabel = new JLabel();
        flagLabel.setHorizontalAlignment(SwingConstants.CENTER);

        if (currentMode != null && currentTile != null && currentTile.getController() != null) {
            BufferedImage flagIcon = currentTile.getController().getFlagImage(BOB.getInstance().getScenarioSceneLoader().getCurrentScenario());
            if (flagIcon != null) {
                Image scaled = flagIcon.getScaledInstance(130, 80, Image.SCALE_SMOOTH);
                flagLabel.setIcon(new ImageIcon(scaled));
            }
        }

        flagPanel.add(flagLabel, BorderLayout.CENTER);

        JLabel countryName = new JLabel(currentTile == null ? "None" : currentTile.getController().countryName());

        countryName.setForeground(Color.WHITE);
        countryName.setFont(countryName.getFont().deriveFont(Font.BOLD, 20f));
        countryName.setAlignmentX(Component.CENTER_ALIGNMENT);
        countryName.setHorizontalAlignment(SwingConstants.CENTER);
        countryName.setFocusable(false);

        JPanel namePanel = new JPanel();
        namePanel.setOpaque(false);
        namePanel.setLayout(new BoxLayout(namePanel, BoxLayout.X_AXIS));
        namePanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        namePanel.add(Box.createHorizontalGlue());
        namePanel.add(countryName);
        namePanel.add(Box.createHorizontalGlue());
        namePanel.setFocusable(false);

        JLabel ideo = new JLabel("Ideology: " + (currentTile == null ? "None" : currentTile.getController().getRulingIdeologyString()));
        ideo.setForeground(Color.WHITE);
        ideo.setFont(ideo.getFont().deriveFont(Font.BOLD, 15f));
        ideo.setAlignmentX(Component.CENTER_ALIGNMENT);
        ideo.setHorizontalAlignment(SwingConstants.CENTER);
        ideo.setFocusable(false);

        JPanel ideology = new JPanel();
        ideology.setOpaque(false);
        ideology.setLayout(new BoxLayout(ideology, BoxLayout.X_AXIS));
        ideology.setAlignmentX(Component.CENTER_ALIGNMENT);
        ideology.add(Box.createHorizontalGlue());
        ideology.add(ideo);
        ideology.add(Box.createHorizontalGlue());
        ideology.setFocusable(false);

        panel.add(Box.createVerticalStrut(10));
        panel.add(flagPanel);
        panel.add(Box.createVerticalStrut(5));
        panel.add(namePanel);
        panel.add(Box.createVerticalStrut(8));
        panel.add(ideology);
        panel.add(Box.createVerticalStrut(15));
        panel.add(createFocusButton());


        JPanel horizontalLine = new JPanel();
        horizontalLine.setBackground(BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().palette().darkColor().darker());
        horizontalLine.setOpaque(true);
        horizontalLine.setMaximumSize(new Dimension(panelWidth, 4));
        horizontalLine.setPreferredSize(new Dimension(panelWidth, 4));
        horizontalLine.setMinimumSize(new Dimension(panelWidth, 4));
        horizontalLine.setAlignmentX(Component.CENTER_ALIGNMENT);
        horizontalLine.setFocusable(false);

        panel.add(Box.createVerticalStrut(8));
        panel.add(horizontalLine);

        if(BOB.getInstance().getPlayer() != null && BOB.getInstance().getPlayer().country() != null && currentTile.getController() != null) {
            if(BOB.getInstance().getWarManager().isAtWar(currentTile.getController(), BOB.getInstance().getPlayer().country())) return panel;
            if(Objects.equals(BOB.getInstance().getPlayer().country().getAbbreviation(), currentTile.getController().getAbbreviation())) return panel;

            panel.add(Box.createVerticalStrut(30));
        }

        return panel;
    }
    public void updateTabsFr() {
        updateTabs();
        revalidate();
        repaint();
    }


    private void updateTabs() {
        TabMode newMode;

        if (currentTile == null) newMode = TabMode.NONE;
        else {
            boolean ownCountry = currentTile.getController() != null && Objects.equals(currentTile.getController().getAbbreviation(), BOB.getInstance().getPlayer().country().getAbbreviation())/*currentTile.getController().getPlayer().uuid() == BOB.getInstance().getPlayer().uuid()*/;
            newMode = ownCountry ? TabMode.OWN : TabMode.FOREIGN;
        }

        if (newMode == currentMode) return;

        currentMode = newMode;
        tabbedPane.removeAll();

        switch (currentMode) {
            case OWN -> {
                tabbedPane.addTab("Overview", createOverviewPanel());
                tabbedPane.addTab("Industry", createIndustryPanel());
                tabbedPane.addTab("Deployment", createDeploymentPanel());
            }
            case FOREIGN -> tabbedPane.addTab("Diplomacy", createForeignOverviewPanel());

            case NONE -> {}
        }

        tabbedPane.revalidate();
        tabbedPane.repaint();
    }

    private Component createDeploymentPanel() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setFocusable(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        Color dark = BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().palette().darkColor();
        Color light = BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().palette().lightColor();

        JLabel title = new JLabel("Deployment");
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));

        panel.add(Box.createVerticalStrut(10));
        panel.add(title);
        panel.add(Box.createVerticalStrut(15));

        JLabel recruitTitle = new JLabel("Recruitment");
        recruitTitle.setForeground(light);
        recruitTitle.setFont(recruitTitle.getFont().deriveFont(Font.BOLD, 16f));
        recruitTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        List<Pair<String, String>> strings = BOB.getInstance().getTileManager().getTiles(BOB.getInstance().getPlayer().country());

        JComboBox<Pair<String, String>> locationDropdown = new JComboBox<Pair<String, String>>(strings.toArray(new Pair[0]));

        locationDropdown.setUI(new ModernComboBoxUI());
        locationDropdown.setMaximumSize(new Dimension(200, 30));
        locationDropdown.setAlignmentX(Component.CENTER_ALIGNMENT);
        locationDropdown.setFocusable(false);

        locationDropdown.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                if (value instanceof Pair<?, ?> pair) label.setText(String.valueOf(pair.value()));

                return label;
            }
        });

        JLabel selectedLabel = new JLabel("Selected: " + (strings.isEmpty() ? "None" : strings.getFirst().value()));

        selectedLabel.setForeground(light);
        selectedLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        locationDropdown.addActionListener(e -> {
            Pair<String, String> selected = (Pair<String, String>) locationDropdown.getSelectedItem();

            if (selected != null) selectedLabel.setText("Selected: " + selected.value());
        });

        JSpinner divisionSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 5, 1));

        divisionSpinner.setMaximumSize(new Dimension(80, 30));
        divisionSpinner.setAlignmentX(Component.CENTER_ALIGNMENT);
        divisionSpinner.setUI(new ModernSpinnerUI());
        divisionSpinner.setFocusable(false);

        JButton spawnButton = new BOBButton("Train", BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().button().textColor(), BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().button().bgColor(), BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().button().borderColor(), BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().button().borderColorHover(), 16, 5);

        spawnButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        spawnButton.setFocusable(false);

        spawnButton.addActionListener(e -> {
            Pair<String, String> selected = (Pair<String, String>) locationDropdown.getSelectedItem();

            if (selected == null) return;

            int divisions = (Integer) divisionSpinner.getValue();

            RequestPacket packet = new RequestPacket(Type.TRAIN_TROOP, selected.key() + ";" + BOB.getInstance().getPlayer().country().getAbbreviation() + ";" + divisions);

            BOB.getInstance().getSendTool().send(BOB.getInstance().getClient().getChannel(), packet);
        });

        panel.add(recruitTitle);
        panel.add(Box.createVerticalStrut(10));
        panel.add(divisionSpinner);
        panel.add(Box.createVerticalStrut(10));
        panel.add(selectedLabel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(locationDropdown);
        panel.add(Box.createVerticalStrut(10));
        panel.add(spawnButton);
        panel.add(Box.createVerticalStrut(20));

        JPanel separator = new JPanel();
        separator.setBackground(dark.darker());
        separator.setMaximumSize(new Dimension(panelWidth, 3));
        separator.setPreferredSize(new Dimension(panelWidth, 3));
        separator.setMinimumSize(new Dimension(panelWidth, 3));

        panel.add(separator);
        panel.add(Box.createVerticalStrut(15));

        JLabel queueTitle = new JLabel("Training");
        queueTitle.setForeground(Color.WHITE);
        queueTitle.setFont(queueTitle.getFont().deriveFont(Font.BOLD, 17f));
        queueTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(queueTitle);
        panel.add(queueTitle);
        panel.add(Box.createVerticalStrut(10));

        trainingList = new JPanel();
        trainingList.setOpaque(false);
        trainingList.setLayout(new BoxLayout(trainingList, BoxLayout.Y_AXIS));

        JScrollPane scrollPane = new JScrollPane(trainingList);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(new RoundedBorder(10, 4,dark.brighter()), new EmptyBorder(5, 5, 5, 5)));
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);


        scrollPane.getVerticalScrollBar().setUI(new ModernScrollBarUI());
        scrollPane.getVerticalScrollBar().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        panel.add(scrollPane);

        return panel;
    }

    public void refreshTrainingList() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::refreshTrainingList);
            return;
        }

        if (trainingList == null) return;

        Set<TrainEntry> entries = BOB.getInstance().getTroopManager().getTraining();

        if (entries == null) return;

        if (entries.isEmpty()) {
            trainingList.removeAll();

            JLabel empty = new JLabel("No divisions training");
            empty.setForeground(Color.GRAY);
            empty.setAlignmentX(Component.CENTER_ALIGNMENT);

            trainingList.add(Box.createVerticalStrut(15));
            trainingList.add(empty);

            trainingList.revalidate();
            trainingList.repaint();
            return;
        }

        if (trainingList.getComponentCount() == 2 && !(trainingList.getComponent(0) instanceof JPanel)) trainingList.removeAll();

        Set<UUID> activeIds = entries.stream().map(TrainEntry::getUuid).collect(Collectors.toSet());

        for (Component component : trainingList.getComponents()) {
            if (!(component instanceof JPanel card)) continue;

            TrainEntry entry = (TrainEntry) card.getClientProperty("entry");

            if (entry != null && !activeIds.contains(entry.getUuid())) trainingList.remove(card);
        }

        for (TrainEntry entry : entries) {
            JPanel existingCard = null;

            for (Component component : trainingList.getComponents()) {
                if (!(component instanceof JPanel card)) continue;

                TrainEntry existing = (TrainEntry) card.getClientProperty("entry");

                if (existing != null && Objects.equals(existing.getUuid(), entry.getUuid())) {
                    existingCard = card;
                    break;
                }
            }

            if (existingCard == null) {
                trainingList.add(createTrainingEntry(entry));
                trainingList.add(Box.createVerticalStrut(10));
                continue;
            }

            JProgressBar progressBar = (JProgressBar) existingCard.getClientProperty("progressBar");
            JButton deployButton = (JButton) existingCard.getClientProperty("deployButton");

            int progress = entry.getExpIfDeployedNow(System.currentTimeMillis());

            progressBar.setValue(progress);
            progressBar.setString(progress + "%");

            deployButton.setVisible(progress >= 100);
        }

        trainingList.revalidate();
        trainingList.repaint();
    }

    private JPanel createTrainingEntry(TrainEntry entry) {
        Color dark = BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().palette().defaultColor();
        Color dark2 = BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().palette().darkColor();
        Color light = BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().palette().lightColor();

        JPanel card = new JPanel();
        card.setOpaque(true);
        card.setBackground(dark.darker());
        card.setBorder(BorderFactory.createCompoundBorder(new RoundedBorder(10, 4,dark.brighter()), new EmptyBorder(5, 5, 5, 5)));

        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setMaximumSize(new Dimension(panelWidth - 20, 160));
        card.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel name = new JLabel(entry.getTroopName());
        name.setForeground(Color.WHITE);
        name.setFont(name.getFont().deriveFont(Font.BOLD, 15f));
        name.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel template = new JLabel(entry.getTroopTemplate());
        template.setForeground(light);
        template.setFont(template.getFont().deriveFont(12f));
        template.setAlignmentX(Component.CENTER_ALIGNMENT);

        JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setValue(entry.getExpIfDeployedNow(System.currentTimeMillis()));
        progressBar.setStringPainted(true);
        progressBar.setString(progressBar.getValue() + "%");
        progressBar.setForeground(dark);
        progressBar.setBackground(dark2);
        progressBar.setBorderPainted(false);
        progressBar.setBorder(BorderFactory.createLineBorder(dark, 3));
        progressBar.setMaximumSize(new Dimension(panelWidth - 45, 20));
        progressBar.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton deployButton = new BOBButton("Deploy", BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().button().textColor(), BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().button().bgColor(), BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().button().borderColor(), BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().button().borderColorHover(), 14, 5);

        deployButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        deployButton.setFocusable(false);

        boolean finished = entry.getExpIfDeployedNow(System.currentTimeMillis()) >= 100;

        deployButton.setVisible(finished);

        deployButton.addActionListener(e -> {
            if (entry.getExpIfDeployedNow(System.currentTimeMillis()) < 100) return;

            RequestPacket packet = new RequestPacket(Type.DEPLOY_TROOP, entry.getUuid().toString());
            BOB.getInstance().getSendTool().send(BOB.getInstance().getClient().getChannel(), packet);
        });

        card.add(name);
        card.add(Box.createVerticalStrut(3));
        card.add(template);
        card.add(Box.createVerticalStrut(8));
        card.add(progressBar);
        card.add(Box.createVerticalStrut(8));
        card.add(deployButton);

        card.putClientProperty("entry", entry);
        card.putClientProperty("progressBar", progressBar);
        card.putClientProperty("deployButton", deployButton);

        return card;
    }

    //TODO: request whether we are at war with the controller of the clicked tile and than remove or add the declare war button depending on this fact
    private Component createForeignOverviewPanel() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setFocusable(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JPanel horizontalLine1 = new JPanel();
        horizontalLine1.setBackground(BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().palette().darkColor().darker());
        horizontalLine1.setOpaque(true);
        horizontalLine1.setMaximumSize(new Dimension(panelWidth, 4));
        horizontalLine1.setPreferredSize(new Dimension(panelWidth, 4));
        horizontalLine1.setMinimumSize(new Dimension(panelWidth, 4));
        horizontalLine1.setAlignmentX(Component.CENTER_ALIGNMENT);
        horizontalLine1.setFocusable(false);
        panel.add(horizontalLine1);

        JPanel flagPanel = new JPanel(new BorderLayout());
        flagPanel.setOpaque(false);
        flagPanel.setMaximumSize(new Dimension(140, 90));
        flagPanel.setPreferredSize(new Dimension(140, 90));
        flagPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        flagPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(180, 180, 180), 2), BorderFactory.createEmptyBorder(3, 3, 3, 3)));

        JLabel flagLabel = new JLabel();
        flagLabel.setHorizontalAlignment(SwingConstants.CENTER);

        if (currentMode != null && currentTile != null && currentTile.getController() != null) {
            BufferedImage flagIcon = currentTile.getController().getFlagImage(BOB.getInstance().getScenarioSceneLoader().getCurrentScenario());
            if (flagIcon != null) {
                Image scaled = flagIcon.getScaledInstance(130, 80, Image.SCALE_SMOOTH);
                flagLabel.setIcon(new ImageIcon(scaled));
            }
        }

        flagPanel.add(flagLabel, BorderLayout.CENTER);

        JLabel countryName = new JLabel(currentTile == null ? "None" : currentTile.getController().countryName());
        countryName.setForeground(Color.WHITE);
        countryName.setFont(countryName.getFont().deriveFont(Font.BOLD, 20f));
        countryName.setAlignmentX(Component.CENTER_ALIGNMENT);
        countryName.setHorizontalAlignment(SwingConstants.CENTER);
        countryName.setFocusable(false);

        JPanel namePanel = new JPanel();
        namePanel.setOpaque(false);
        namePanel.setLayout(new BoxLayout(namePanel, BoxLayout.X_AXIS));
        namePanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        namePanel.add(Box.createHorizontalGlue());
        namePanel.add(countryName);
        namePanel.add(Box.createHorizontalGlue());
        namePanel.setFocusable(false);

        JButton milAccess = new BOBButton("Military Access", BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().button().textColor(), BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().button().bgColor(), BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().button().borderColor(), BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().button().borderColorHover(), 16, 5);

        JButton declareWarButton = new BOBButton("Declare War", BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().button().textColor(), BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().button().bgColor(), BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().button().borderColor(), BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().button().borderColorHover(), 16, 5);

        declareWarButton.setFocusable(false);
        declareWarButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        declareWarButton.addActionListener(e -> {
            if (currentTile == null || currentTile.getController() == null) return;

            RequestPacket pack = new RequestPacket(Type.START_WAR, currentTile.getAbbreviation() + ";" + currentTile.getController().getAbbreviation() + ";false");

            BOB.getInstance().getSendTool().send(BOB.getInstance().getClient().getChannel(), pack);

            int index = panel.getComponentZOrder(declareWarButton);
            panel.remove(declareWarButton);

            JLabel warLabel = new JLabel("At war: pending");
            warLabel.setForeground(Color.WHITE);
            warLabel.setFont(warLabel.getFont().deriveFont(Font.BOLD, 16f));
            warLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            panel.add(warLabel, index);

            panel.revalidate();
            panel.repaint();
        });

        milAccess.setFocusable(false);
        milAccess.setAlignmentX(Component.CENTER_ALIGNMENT);

        milAccess.addActionListener(e -> {
            if (currentTile == null || currentTile.getController() == null) return;
            if (MilAccessRules.canHave(BOB.getInstance().getWarManager(), currentTile.getController(), BOB.getInstance().getPlayer().country())) {
                BOB.getInstance().getMainRenderer().getGamePanel().showGenericPopup("ACTION FAILED", "You cant request military access from ", null, currentTile.getController(),5000);
                return;
            }


            RequestPacket pack = new RequestPacket(Type.MIL_ACCESS, currentTile.getController().getAbbreviation());

            BOB.getInstance().getSendTool().send(BOB.getInstance().getClient().getChannel(), pack);

            int index = panel.getComponentZOrder(milAccess);
            panel.remove(milAccess);

            JLabel milAccessLabel = new JLabel("Military Access: pending");
            milAccessLabel.setForeground(Color.WHITE);
            milAccessLabel.setFont(milAccessLabel.getFont().deriveFont(Font.BOLD, 16f));
            milAccessLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            panel.add(milAccessLabel, index);

            panel.revalidate();
            panel.repaint();
        });

        JLabel ideo = new JLabel("Ideology: " + (currentTile == null ? "None" : currentTile.getController().getRulingIdeologyString()));
        ideo.setForeground(Color.WHITE);
        ideo.setFont(ideo.getFont().deriveFont(Font.BOLD, 15f));
        ideo.setAlignmentX(Component.CENTER_ALIGNMENT);
        ideo.setHorizontalAlignment(SwingConstants.CENTER);
        ideo.setFocusable(false);

        JPanel ideology = new JPanel();
        ideology.setOpaque(false);
        ideology.setLayout(new BoxLayout(ideology, BoxLayout.X_AXIS));
        ideology.setAlignmentX(Component.CENTER_ALIGNMENT);
        ideology.add(Box.createHorizontalGlue());
        ideology.add(ideo);
        ideology.add(Box.createHorizontalGlue());
        ideology.setFocusable(false);

        panel.add(Box.createVerticalStrut(10));
        panel.add(flagPanel);
        panel.add(Box.createVerticalStrut(5));
        panel.add(namePanel);
        panel.add(Box.createVerticalStrut(8));
        panel.add(ideology);

        JPanel horizontalLine = new JPanel();
        horizontalLine.setBackground(BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().palette().darkColor().darker());
        horizontalLine.setOpaque(true);
        horizontalLine.setMaximumSize(new Dimension(panelWidth, 4));
        horizontalLine.setPreferredSize(new Dimension(panelWidth, 4));
        horizontalLine.setMinimumSize(new Dimension(panelWidth, 4));
        horizontalLine.setAlignmentX(Component.CENTER_ALIGNMENT);
        horizontalLine.setFocusable(false);

        panel.add(Box.createVerticalStrut(8));
        panel.add(horizontalLine);

        if(BOB.getInstance().getPlayer() != null && BOB.getInstance().getPlayer().country() != null && currentTile.getController() != null) {
            boolean atWar = BOB.getInstance().getWarManager().isAtWar(currentTile.getController(), BOB.getInstance().getPlayer().country());
            boolean hasMilitaryAccess = currentTile.getController().hasCountryMilAccess(BOB.getInstance().getPlayer().country());

            if(Objects.equals(BOB.getInstance().getPlayer().country().getAbbreviation(), currentTile.getController().getAbbreviation())) return panel;

            panel.add(Box.createVerticalStrut(50));

            if(atWar) {
                JLabel warLabel = new JLabel("At war: yes");
                warLabel.setForeground(Color.WHITE);
                warLabel.setFont(warLabel.getFont().deriveFont(Font.BOLD, 16f));
                warLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                panel.add(warLabel);
            } else {
                panel.add(declareWarButton);
            }

            if(hasMilitaryAccess) {
                panel.add(Box.createVerticalStrut(15));
                JLabel milAccessLabel = new JLabel("Military Access: yes");
                milAccessLabel.setForeground(Color.WHITE);
                milAccessLabel.setFont(milAccessLabel.getFont().deriveFont(Font.BOLD, 16f));
                milAccessLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                panel.add(milAccessLabel);
            } else {
                panel.add(Box.createVerticalStrut(15));
                panel.add(milAccess);
            }
        }

        return panel;
    }

    private JButton createFocusButton() {
        JButton focusButton = new BOBButton("National Focus: 0 days", BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().button().textColor(), BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().button().bgColor(), BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().button().borderColor(), BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().button().borderColorHover(), 16, 5);

        focusButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        focusButton.setFocusable(false);

        focusButton.addActionListener(e -> {
            String abbr = "";
            if (currentTile == null || currentTile.getController() == null) {
                if(BOB.getInstance().getPlayer().country() != null) {
                    abbr = BOB.getInstance().getPlayer().country().getAbbreviation();
                }
            } else abbr = currentTile.getController().getAbbreviation();

            focusesUI.setCountryToView(abbr);
            focusesUI.setVisible(true);
            sidePanel.setVisible(false);

            revalidate();
            repaint();
        });

        return focusButton;
    }

    @Override
    public void setVisible(boolean visible) {
        if(!visible) focusesUI.setVisible(false);
    }

    private JPanel createIndustryPanel() {
        JPanel panel = new JPanel();
        panel.setFocusable(false);
        panel.setOpaque(false);

        JLabel label = new JLabel("Industry data");
        label.setForeground(Color.WHITE);

        panel.add(label);
        return panel;
    }

    public boolean visible() {
        return isVisible;
    }

    public void visible(boolean visible) {
        if(focusesUI.isVisible()) return;
        this.isVisible = visible;

        if (!visible) setTile(null);

        sidePanel.setVisible(visible);

        revalidate();
        repaint();
    }

    public void setTile(Tile tile) {
        if (this.currentTile == tile) return;

        this.currentTile = tile;

        currentMode = TabMode.NONE; //hacky but works
        updateTabs();

        revalidate();
        repaint();
    }

    public Tile getTile() {
        return currentTile;
    }

    public HUDTopBar getTopBar() {
        return topBar;
    }

    public FocusesUI getFocusUI() {
        return focusesUI;
    }

    private enum TabMode {
        NONE,
        OWN,
        FOREIGN
    }
}