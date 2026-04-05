package de.idiotischer.bob.render.menu.impl.select;

import de.idiotischer.bob.BOB;
import de.idiotischer.bob.render.menu.components.ModernScrollBarUI;
import de.idiotischer.bob.render.menu.components.button.BOBButton;
import de.idiotischer.bob.scenario.Scenario;
import de.idiotischer.bob.util.ImageUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class ScenarioSelectMenu extends JPanel {

    private final int layoutScaleX = 850;
    private final int layoutScaleY = 500;
    private Scenario selectedScenario;
    private final JPanel listPanel;

    public ScenarioSelectMenu(Scenario selected) {
        this.selectedScenario = selected;
        this.setOpaque(false);
        this.setLayout(null);
        this.setPreferredSize(new Dimension(layoutScaleX, layoutScaleY));

        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setOpaque(false);

        JScrollPane scroller = new JScrollPane(listPanel);
        scroller.setOpaque(false);
        scroller.getViewport().setOpaque(false);
        scroller.setBorder(null);
        scroller.getVerticalScrollBar().setUI(new ModernScrollBarUI());
        scroller.getVerticalScrollBar().setPreferredSize(new Dimension(10, 0));

        scroller.setBounds(40, 40, 350, 360);
        this.add(scroller);

        int bottomY = layoutScaleY - 60;

        JButton backBtn = createButton("Back to Menu", 180, 40);
        backBtn.setBounds(40, bottomY, 180, 40);
        backBtn.addActionListener(e -> BOB.getInstance().getMainRenderer().getMenuPanel().setInScenarioSelect(false));
        this.add(backBtn);

        JButton placeholderBtn = createButton("Placeholder", 140, 40);
        placeholderBtn.setBounds(355, bottomY, 140, 40);
        this.add(placeholderBtn);

        JButton nextBtn = createButton("Next", 120, 40);
        nextBtn.setBounds(layoutScaleX - 160, bottomY, 120, 40);
        nextBtn.addActionListener(e -> {
            if (selectedScenario != null) {
                BOB.getInstance().getMainRenderer().getMenuPanel().setScenarioSelectMenu(new CountrySelectMenu(selectedScenario));
            }
        });
        this.add(nextBtn);

        reloadList();
    }

    private void reloadList() {
        listPanel.removeAll();
        ButtonGroup group = new ButtonGroup();

        for (Scenario s : BOB.getInstance().getScenarioManager().getScenarios()) {
            BOBButton sBtn = (BOBButton) createButton(s.getName(), 320, 50);

            sBtn.setMaximumSize(new Dimension(320, 50));
            sBtn.setAlignmentX(Component.LEFT_ALIGNMENT);

            if (selectedScenario != null && s.getName().equals(selectedScenario.getName())) {
                sBtn.setSelected(true);
            }

            sBtn.addActionListener(e -> {
                java.util.Enumeration<AbstractButton> en = group.getElements();
                while (en.hasMoreElements()) {
                    en.nextElement().setSelected(false);
                }

                selectedScenario = s;
                sBtn.setSelected(true);

                //repaint();
            });

            group.add(sBtn);
            listPanel.add(sBtn);
            listPanel.add(Box.createVerticalStrut(10));
        }
        listPanel.revalidate();
        //listPanel.repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(Color.DARK_GRAY);
        g2.fillRoundRect(4, 4, getWidth() - 8, getHeight() - 8, 30, 30);

        g2.setStroke(new BasicStroke(8));
        g2.setColor(Color.DARK_GRAY.darker());
        g2.drawRoundRect(4, 4, getWidth() - 8, getHeight() - 8, 30, 30);

        if (selectedScenario != null && selectedScenario.getMapImage() != null) {
            int imgFrameWidth = 400;
            int imgFrameX = layoutScaleX - imgFrameWidth - 40;

            g2.setStroke(new BasicStroke(4));
            g2.setColor(Color.DARK_GRAY.darker());
            g2.drawRoundRect(imgFrameX, 40, imgFrameWidth, 360, 24, 24);

            BufferedImage mapImg = selectedScenario.getMapImage();
            int drawW = imgFrameWidth - 20;
            int drawH = (int)(drawW * ((double)mapImg.getHeight() / mapImg.getWidth()));

            if(drawH > 340) drawH = 340;

            g2.setStroke(new BasicStroke(5));
            g2.drawRoundRect(imgFrameX + 10, 50, drawW, drawH, 24, 24);
            g2.drawImage(ImageUtil.makeRoundedCorner(mapImg, 100), imgFrameX + 10, 50, drawW, drawH, null);
        }
    }

    private JButton createButton(String text, int width, int height) {
        BOBButton btn = new BOBButton(text,
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
}