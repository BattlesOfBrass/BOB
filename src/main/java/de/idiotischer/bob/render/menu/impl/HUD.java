package de.idiotischer.bob.render.menu.impl;

import de.idiotischer.bob.BOB;
import de.idiotischer.bob.render.menu.components.HUDTopBar;
import de.idiotischer.bob.render.menu.components.ModernTabbedPane;
import de.idiotischer.bob.state.State;

import javax.swing.*;
import java.awt.*;

public class HUD extends JPanel {
    private boolean isVisible = false;
    private State currentState = null;
    private final HUDTopBar topBar;

    private final JPanel sidePanel;
    private final JTabbedPane tabbedPane;
    private int panelWidth = 300;

    public HUD() {
        setOpaque(false);
        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));

        topPanel.setOpaque(false);

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
        String name = BOB.getInstance().getPlayer().country().countryName();
        topBar.setCountryName(name);
    }

    @Override
    public void paint(Graphics g) {
        updateHUD();
        super.paint(g);
    }

    private JPanel createOverviewPanel() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);

        JLabel label = new JLabel();
        label.setForeground(Color.WHITE);

        if (currentState != null) {
            if (currentState.getController().getPlayer().uuid() == BOB.getInstance().getPlayer().uuid()) {
                label.setText("Your country overview");
            } else {
                label.setText("Foreign country overview");
            }
        }

        panel.add(label);
        return panel;
    }

    private JPanel createIndustryPanel() {
        JPanel panel = new JPanel();
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
        if(!visible) setState(null);
        sidePanel.setVisible(visible);
    }

    public void setState(State state) {
        this.currentState = state;
    }

    public State getState() {
        return currentState;
    }
}