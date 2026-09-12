package de.idiotischer.bob.render.menu.impl;

import de.idiotischer.bob.BOB;
import de.idiotischer.bob.render.menu.components.ModernScrollBarUI;
import de.idiotischer.bob.render.menu.components.button.BOBButton;

import javax.swing.*;
import java.awt.*;

public class SettingsMenu extends JPanel {

    private final JButton graphicsButton;
    private final JButton audioButton;
    private final JButton accessibilityButton;
    private final JButton generalButton;

    private final int layoutScaleX = 850;
    private final int layoutScaleY = 500;

    private final CardLayout cardLayout;
    private final JPanel settingsPanel;

    public SettingsMenu() {
        setLayout(new BorderLayout());
        setOpaque(false);

        this.setPreferredSize(new Dimension(layoutScaleX, layoutScaleY));

        Color dark = BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().palette().darkColor();
        Color lighter = dark.brighter();
        Color textColor = Color.WHITE;

        graphicsButton = createThemedButton("Graphics", lighter, textColor);
        audioButton = createThemedButton("Audio", lighter, textColor);
        accessibilityButton = createThemedButton("Accessibility", lighter, textColor);
        generalButton = createThemedButton("General", lighter, textColor);

        JPanel categoryPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 20));
        categoryPanel.setOpaque(false);

        categoryPanel.add(graphicsButton);
        categoryPanel.add(audioButton);
        categoryPanel.add(accessibilityButton);
        categoryPanel.add(generalButton);

        cardLayout = new CardLayout();
        settingsPanel = new JPanel(cardLayout);
        settingsPanel.setOpaque(false);

        settingsPanel.add(createSettingsPage(createToggleButton("VSync", true), createToggleButton("Show FPS", false)), "Graphics");
        settingsPanel.add(createSettingsPage(createToggleButton("Total", true), createToggleButton("Music", true)), "Audio");
        settingsPanel.add(createSettingsPage(createToggleButton("High Contrast", false), createToggleButton("Colorblind Mode", false), createToggleButton("Reduce Motion", false), createToggleButton("Screen Flash", true)), "Accessibility");
        settingsPanel.add(createSettingsPage(createToggleButton("Show Tutorials", true), createToggleButton("Auto Save", true), createToggleButton("Show Popups", true), createToggleButton("Debug Mode", false)), "General");

        graphicsButton.addActionListener(e -> cardLayout.show(settingsPanel, "Graphics"));
        audioButton.addActionListener(e -> cardLayout.show(settingsPanel, "Audio"));
        accessibilityButton.addActionListener(e -> cardLayout.show(settingsPanel, "Accessibility"));
        generalButton.addActionListener(e -> cardLayout.show(settingsPanel, "General"));

        JButton backBtn = createButton("Back", 120, 40);
        backBtn.setPreferredSize(new Dimension(120, 40));
        backBtn.addActionListener(e -> {
            BOB.getInstance().getMainRenderer().getMenuPanel().setSettingsMenu(false);
        });

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 40, 10));
        bottomPanel.setOpaque(false);
        bottomPanel.add(backBtn);

        add(categoryPanel, BorderLayout.NORTH);
        add(settingsPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        cardLayout.show(settingsPanel, "Graphics");
    }

    private JScrollPane createSettingsPage(JComponent... components) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);

        for (JComponent component : components) {
            component.setAlignmentX(Component.CENTER_ALIGNMENT);

            panel.add(component);
            panel.add(Box.createVerticalStrut(10));
        }

        JScrollPane scrollPane = new JScrollPane(panel);

        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 30, 10, 30));

        scrollPane.getVerticalScrollBar().setUI(new ModernScrollBarUI());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        return scrollPane;
    }


    private JToggleButton createToggleButton(String text, boolean enabled) {
        JToggleButton button = new JToggleButton() {

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();

                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(isSelected() ? new Color(60, 150, 80) : new Color(100, 100, 100));

                g2.fillRect(0, 0, getWidth(), getHeight());

                g2.dispose();

                super.paintComponent(g);
            }
        };

        button.setPreferredSize(new Dimension(300, 40));
        button.setMinimumSize(new Dimension(300, 40));
        button.setMaximumSize(new Dimension(300, 40));

        button.setFocusPainted(false);
        button.setBorderPainted(false);

        button.setContentAreaFilled(false);
        button.setOpaque(false);

        button.setFont(new Font("SansSerif", Font.BOLD, 14));
        button.setForeground(Color.WHITE);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.setSelected(enabled);
        updateToggleButton(button, text);

        button.addActionListener(e -> {
            updateToggleButton(button, text);
            button.repaint();
        });

        return button;
    }


    private void updateToggleButton(JToggleButton button, String text) {
        boolean active = button.isSelected();

        button.setText(text + " [" + (active ? "ON" : "OFF") + "]");

        button.setBackground(active ? new Color(60, 150, 80) : new Color(100, 100, 100));
    }


    private JButton createThemedButton(String text, Color bgColor, Color fgColor) {
        JButton button = new JButton(text);
        button.setBackground(bgColor);
        button.setForeground(fgColor);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setFont(new Font("SansSerif", Font.BOLD, 14));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color dark = BOB.getInstance().getScenarioSceneLoader().getCurrentScenario().getTheme().palette().darkColor();

        g2.setColor(dark);
        g2.fillRoundRect(4, 4, getWidth() - 8, getHeight() - 8, 30, 30);

        g2.setStroke(new BasicStroke(8));
        g2.setColor(dark.darker());
        g2.drawRoundRect(4, 4, getWidth() - 8, getHeight() - 8, 30, 30);

        g2.dispose();
    }

    private BOBButton createButton(String text, int width, int height) {
        BOBButton btn = new BOBButton(text, BOB.getInstance().getSettingsTheme().button().textColor(), BOB.getInstance().getSettingsTheme().button().bgColor(), BOB.getInstance().getSettingsTheme().button().borderColor(), BOB.getInstance().getSettingsTheme().button().borderColorHover(), 16, 5);
        btn.setPreferredSize(new Dimension(width, height));
        btn.setFocusable(false);
        return btn;
    }
}
