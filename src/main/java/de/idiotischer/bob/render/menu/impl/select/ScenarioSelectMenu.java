package de.idiotischer.bob.render.menu.impl.select;

import de.idiotischer.bob.BOB;
import de.idiotischer.bob.render.MenuPanel;
import de.idiotischer.bob.render.menu.components.ModernScrollBarUI;
import de.idiotischer.bob.render.menu.components.button.BOBButton;
import de.idiotischer.bob.scenario.Scenario;
import de.idiotischer.bob.util.Helper;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

//TODO: make this redraw the image in chunks and multiple draw cycles yk (like 8 chunks or smth)
public class ScenarioSelectMenu extends JPanel {

    private final int layoutScaleX = 850;
    private final int layoutScaleY = 500;
    private Scenario selectedScenario;
    private final JPanel listPanel;

    private Scenario lastCachedScenario;
    private BufferedImage lastSourceMapImg;
    private BufferedImage lastSourceBgImg;
    private int lastCachedDrawW = -1;
    private int lastCachedDrawH = -1;
    private BufferedImage cachedBackgroundImage;
    private BufferedImage cachedMapImage;

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
        scroller.getVerticalScrollBar().setUnitIncrement(16);

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
                BOB.getInstance().getMainRenderer().getMenuPanel().setScenarioSelectMenu(new CountrySelectMenu("Start Game", selectedScenario, (b1) -> {
                    BOB.getInstance().getMainRenderer().getMenuPanel().setScenarioSelectMenu(new ScenarioSelectMenu(b1.getScenario()));
                }, (menu) -> {
                    if (menu.getSelectedCountry() != null) {
                        BOB.getInstance().getScenarioSceneLoader().requestScenarioLoad(menu.getScenario());
                        BOB.getInstance().getPlayer().country(menu.getSelectedCountry());
                    }
                }));
            }
        });
        this.add(nextBtn);

        reloadList();
    }

    private void reloadList() {
        listPanel.removeAll();
        ButtonGroup group = new ButtonGroup();

        for (Scenario s : BOB.getInstance().getScenarioManager().getScenariosSorted()) {
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
                Helper.loadPreview(selectedScenario, true);
                sBtn.setSelected(true);
            });

            group.add(sBtn);
            listPanel.add(sBtn);
            listPanel.add(Box.createVerticalStrut(10));
        }
        listPanel.revalidate();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color dark = BOB.getInstance().getSettingsTheme().palette().darkColor();

        g2.setColor(dark);
        g2.fillRoundRect(4, 4, getWidth() - 8, getHeight() - 8, 30, 30);

        g2.setStroke(new BasicStroke(8));
        g2.setColor(dark.darker());
        g2.drawRoundRect(4, 4, getWidth() - 8, getHeight() - 8, 30, 30);

        if (selectedScenario != null && selectedScenario.getMapImage() != null) {
            int imgFrameWidth = 400;
            int imgFrameX = layoutScaleX - imgFrameWidth - 40;

            g2.setStroke(new BasicStroke(4));
            g2.setColor(dark.darker());
            g2.drawRoundRect(imgFrameX, 40, imgFrameWidth, 360, 24, 24);

            MenuPanel p = BOB.getInstance().getMainRenderer().getMenuPanel();

            BufferedImage mapImg = p.getFrame() == null ? selectedScenario.getMapImage() : p.getFrame();
            BufferedImage bg = selectedScenario.getBackgroundImage();

            int w = Math.min(mapImg.getWidth(), bg.getWidth());
            int h = Math.min(mapImg.getHeight(), bg.getHeight());

            BufferedImage backgroundImage = bg.getSubimage(0, 0, w, h);

            int drawW = imgFrameWidth - 20;
            int drawH = (int)(drawW * ((double)mapImg.getHeight() / mapImg.getWidth()));

            if(drawH > 340) drawH = 340;

            g2.setStroke(new BasicStroke(6));
            g2.drawRoundRect(imgFrameX + 10, 50, drawW, drawH, 24, 24);

            if (selectedScenario != lastCachedScenario || mapImg != lastSourceMapImg || backgroundImage != lastSourceBgImg || drawW != lastCachedDrawW || drawH != lastCachedDrawH) {
                lastCachedScenario = selectedScenario;
                lastSourceMapImg = mapImg;
                lastSourceBgImg = backgroundImage;
                lastCachedDrawW = drawW;
                lastCachedDrawH = drawH;

                cachedBackgroundImage = createImage(backgroundImage, drawW, drawH, 24);
                cachedMapImage = createImage(mapImg, drawW, drawH, 24);
            }

            g2.drawImage(cachedBackgroundImage, imgFrameX + 10, 50, drawW, drawH, null);
            g2.drawImage(cachedMapImage, imgFrameX + 10, 50, drawW, drawH, null);
        }
    }

    private static BufferedImage createImage(BufferedImage image, int targetW, int targetH, int cornerRadius) {
        BufferedImage output = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = output.createGraphics();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        g2.drawImage(image, 0, 0, targetW, targetH, null);

        BufferedImage mask = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gMask = mask.createGraphics();
        gMask.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        gMask.setColor(Color.WHITE);
        gMask.fill(new RoundRectangle2D.Float(0, 0, targetW, targetH, cornerRadius, cornerRadius));
        gMask.dispose();

        g2.setComposite(AlphaComposite.DstIn);
        g2.drawImage(mask, 0, 0, null);

        g2.dispose();
        return output;
    }

    private JButton createButton(String text, int width, int height) {
        BOBButton btn = new BOBButton(text, BOB.getInstance().getSettingsTheme().button().textColor(), BOB.getInstance().getSettingsTheme().button().bgColor(), BOB.getInstance().getSettingsTheme().button().borderColor(), BOB.getInstance().getSettingsTheme().button().borderColorHover(),16, 5);
        btn.setPreferredSize(new Dimension(width, height));
        btn.setFocusable(false);
        return btn;
    }
}