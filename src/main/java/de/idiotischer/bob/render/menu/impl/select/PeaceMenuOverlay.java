package de.idiotischer.bob.render.menu.impl.select;

import de.idiotischer.bob.BOB;
import de.idiotischer.bob.conference.PeaceConference;
import de.idiotischer.bob.conference.TakeTileType;
import de.idiotischer.bob.render.menu.components.button.BOBButton;
import de.idiotischer.bob.tile.Tile;

import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.Set;

// The Overlay on RenderPanel that is shown during conferences
public class PeaceMenuOverlay extends JPanel {

    private PeaceConference peace = null;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Peace Menu Overlay");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(800, 600);
            frame.setLocationRelativeTo(null);

            frame.add(new PeaceMenuOverlay());

            frame.setVisible(true);
        });
    }

    private JButton selectedButton;

    public PeaceMenuOverlay() {
        setOpaque(false);
        setLayout(null);

        int buttonX = 20;
        int buttonY = 55;
        int buttonWidth = 65;
        int buttonHeight = 30;
        int gap = 5;

        for (int i = 0; i < TakeTileType.values().length; i++) {
            TakeTileType tileType = TakeTileType.values()[i];

            BOBButton button = new BOBButton(tileType.getDefaultCharr(), Color.WHITE, Color.BLACK, Color.DARK_GRAY.darker(), Color.GRAY, 16, 5);
            button.setBounds(buttonX + i * (buttonWidth + gap), buttonY, buttonWidth, buttonHeight);

            button.addActionListener(e -> {
                if (selectedButton != null) selectedButton.setSelected(false);
                selectedButton = button;
                selectedButton.setSelected(true);
            });

            add(button);
        }

        if(peace == null) return;

        JPanel countryPanel = new JPanel();
        countryPanel.setLayout(new BoxLayout(countryPanel, BoxLayout.Y_AXIS));
        countryPanel.setOpaque(false);
        countryPanel.setBounds(10, 90, 300, getHeight() - 100);

        for (var country : peace.getDefeated()) {
            JPanel countryEntry = new JPanel();
            countryEntry.setLayout(new BoxLayout(countryEntry, BoxLayout.Y_AXIS));
            countryEntry.setOpaque(false);

            JButton countryButton = new BOBButton(country.countryName(), Color.WHITE, Color.BLACK, Color.DARK_GRAY.darker(), Color.GRAY, 14, 5);

            JPanel tilePanel = new JPanel();
            tilePanel.setLayout(new BoxLayout(tilePanel, BoxLayout.Y_AXIS));
            tilePanel.setOpaque(false);
            tilePanel.setVisible(false);

            for (Tile tile : peace.getTakableTiles().stream().filter(t -> t.getOwner().getAbbreviation().equals(country.getAbbreviation())).toList()) {
                JButton tileButton = new BOBButton(tile.getName(), Color.WHITE, Color.BLACK, Color.DARK_GRAY.darker(), Color.GRAY, 12, 5);

                tileButton.setAlignmentX(Component.LEFT_ALIGNMENT);

                tileButton.addActionListener(e -> {
                    System.out.println("Selected tile: " + tile);
                });

                tilePanel.add(tileButton);
            }

            countryButton.setAlignmentX(Component.LEFT_ALIGNMENT);
            countryButton.addActionListener(e -> {
                tilePanel.setVisible(!tilePanel.isVisible());

                countryPanel.revalidate();
                countryPanel.repaint();
            });

            countryEntry.add(countryButton);
            countryEntry.add(tilePanel);

            countryPanel.add(countryEntry);
        }

        add(countryPanel);


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
            g2.fillRect(0, 40, 320, getHeight());

            g2.setColor(Color.DARK_GRAY.darker());
            g2.drawRect(0, 40, 320, getHeight() - 40);

            g2.setColor(Color.DARK_GRAY);
            g2.fillRect(getWidth() - 320, 40, 320, getHeight());

            g2.setColor(Color.DARK_GRAY.darker());
            g2.drawRect(getWidth() - 320, 40, 320, getHeight() - 40);
        } finally {
            g2.dispose();
        }
    }

    public void setPeace(PeaceConference peace) {
        this.peace = peace;
    }

    public PeaceConference getPeace() {
        return peace;
    }
}
