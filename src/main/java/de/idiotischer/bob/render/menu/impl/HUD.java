package de.idiotischer.bob.render.menu.impl;

import com.aspose.psd.internal.bl.B;
import de.idiotischer.bob.BOB;
import de.idiotischer.bob.networking.packet.impl.pp.RequestPacket;
import de.idiotischer.bob.networking.packet.impl.pp.Type;
import de.idiotischer.bob.render.menu.components.HUDTopBar;
import de.idiotischer.bob.render.menu.components.ModernTabbedPane;
import de.idiotischer.bob.render.menu.components.button.BOBButton;
import de.idiotischer.bob.tile.Tile;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class HUD extends JPanel {
    private boolean isVisible = false;
    private Tile currentTile = null;
    private final HUDTopBar topBar;

    private final JPanel sidePanel;
    private final JTabbedPane tabbedPane;
    private int panelWidth = 300;
    private boolean showingOwn;

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

        sidePanel = new JPanel();
        sidePanel.setBackground(Color.DARK_GRAY);
        sidePanel.setLayout(new BorderLayout());
        sidePanel.setPreferredSize(new Dimension(panelWidth, 0));

        tabbedPane = new ModernTabbedPane();
        tabbedPane.addTab("Overview", createOverviewPanel());
        tabbedPane.addTab("Industry", createIndustryPanel());

        sidePanel.add(tabbedPane, BorderLayout.CENTER);

        sidePanel.setVisible(false);

        add(topPanel, BorderLayout.NORTH);
        add(sidePanel, BorderLayout.WEST);
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

        JLabel label = new JLabel();
        label.setForeground(Color.WHITE);

        if (currentTile != null) {
            if (currentTile.getController().getPlayer().uuid() == BOB.getInstance().getPlayer().uuid()) {
                label.setText("Your country overview");
            } else {
                label.setText("Foreign country overview");
            }
        }

        panel.add(label);
        return panel;
    }

    private void updateTabs() {
        TabMode newMode;

        if (currentTile == null) {
            newMode = TabMode.NONE;
        } else {
            boolean ownCountry = currentTile.getController() != null && currentTile.getController().getPlayer() != null && currentTile.getController() == BOB.getInstance().getPlayer().country()/*currentTile.getController().getPlayer().uuid() == BOB.getInstance().getPlayer().uuid()*/;
            newMode = ownCountry ? TabMode.OWN : TabMode.FOREIGN;
        }

        if (newMode == currentMode) {
            return;
        }

        currentMode = newMode;

        tabbedPane.removeAll();

        switch (currentMode) {
            case OWN -> {
                tabbedPane.addTab("Overview", createOverviewPanel());
                tabbedPane.addTab("Industry", createIndustryPanel());
                tabbedPane.addTab("Deployment", createDeploymentPanel());
            }
            case FOREIGN -> {
                tabbedPane.addTab("Diplomacy", createForeignOverviewPanel());
            }
            case NONE -> {}
        }

        tabbedPane.revalidate();
        tabbedPane.repaint();
    }

    private Component createDeploymentPanel() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setFocusable(false);

        JLabel label = new JLabel("Deployment");
        label.setForeground(Color.WHITE);

        panel.add(label);
        return panel;
    }

    //TODO: request whether we are at war with the controller of the clicked tile and than remove or add the declare war button depending on this fact
    private Component createForeignOverviewPanel() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setFocusable(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JPanel flagPanel = new JPanel(new BorderLayout());
        flagPanel.setOpaque(false);
        flagPanel.setMaximumSize(new Dimension(140, 90));
        flagPanel.setPreferredSize(new Dimension(140, 90));
        flagPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        flagPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 180, 180), 2),
                BorderFactory.createEmptyBorder(3, 3, 3, 3)
        ));

        JLabel flagLabel = new JLabel();
        flagLabel.setHorizontalAlignment(SwingConstants.CENTER);

        if (currentMode != null &&
                currentTile != null &&
                currentTile.getController() != null) {

            BufferedImage flagIcon = currentTile.getController().getFlagImage();
            if (flagIcon != null) {
                Image scaled = flagIcon.getScaledInstance(130, 80, Image.SCALE_SMOOTH);
                flagLabel.setIcon(new ImageIcon(scaled));
            }
        }

        flagPanel.add(flagLabel, BorderLayout.CENTER);

        JLabel countryName = new JLabel(
                currentTile == null ? "None" : currentTile.getController().countryName()
        );
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

        JButton declareWarButton = new BOBButton(
                "Declare War",
                Color.WHITE,
                Color.BLACK,
                Color.DARK_GRAY.darker(),
                Color.LIGHT_GRAY,
                16,
                5
        );

        declareWarButton.setFocusable(false);
        declareWarButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        declareWarButton.addActionListener(e -> {
            if (currentTile == null || currentTile.getController() == null) {
                return;
            }

            RequestPacket pack = new RequestPacket(Type.START_WAR, currentTile.getAbbreviation() + ";" + currentTile.getController().getAbbreviation() + ";false");

            BOB.getInstance().getSendTool().send(BOB.getInstance().getClient().getChannel(), pack);

            //TODO: add war declaration request (with warhelper)
        });

        panel.add(Box.createVerticalStrut(10));
        panel.add(flagPanel);
        panel.add(Box.createVerticalStrut(5));
        panel.add(namePanel);
        panel.add(Box.createVerticalStrut(30));
        panel.add(declareWarButton);

        return panel;
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
        this.isVisible = visible;

        if (!visible) {
            setTile(null);
        }

        sidePanel.setVisible(visible);

        revalidate();
        repaint();
    }

    public void setTile(Tile tile) {
        if (this.currentTile == tile) {
            return;
        }

        this.currentTile = tile;

        currentMode = TabMode.NONE; //hacky but works
        updateTabs();

        revalidate();
        repaint();
    }

    public Tile getTile() {
        return currentTile;
    }

    private enum TabMode {
        NONE,
        OWN,
        FOREIGN
    }
}