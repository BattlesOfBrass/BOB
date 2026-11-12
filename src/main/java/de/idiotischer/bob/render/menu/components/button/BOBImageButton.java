package de.idiotischer.bob.render.menu.components.button;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.lang.ref.SoftReference;

public class BOBImageButton extends JButton {
    private final BufferedImage originalImage;
    private final Color bgColor;
    private final Color textColor;
    private final Color borderColor;
    private final Color borderColorHover;
    private final int arcWidth;
    private final int arcHeight;
    private final int borderWidth;
    private final String id;

    private SoftReference<BufferedImage> cachedScaledImage;
    private int cachedDrawW = -1;
    private int cachedDrawH = -1;

    public BOBImageButton(String id, String text, BufferedImage image, Color textColor, Color bgColor, Color borderColor, Color borderColorHover, int arc, int borderWidth) {
        super(text);
        this.id = id;
        this.originalImage = image;
        this.textColor = textColor;
        this.bgColor = bgColor;
        this.borderColor = borderColor;
        this.borderColorHover = borderColorHover;
        this.arcWidth = arc;
        this.arcHeight = arc;
        this.borderWidth = borderWidth;

        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setForeground(textColor);
        setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        int width = getWidth();
        int height = getHeight();
        ButtonModel model = getModel();

        Color drawBg = bgColor;
        Color drawBorder = borderColor;

        if (isSelected()) {
            drawBg = bgColor.darker();
            drawBorder = Color.WHITE;
        }
        else if (model.isPressed()) drawBg = bgColor.darker();
        else if (model.isRollover()) {
            drawBg = bgColor.brighter();
            drawBorder = borderColorHover;
        }

        g2.setColor(drawBg);
        g2.fillRoundRect(borderWidth/2, borderWidth/2, width - borderWidth, height - borderWidth, arcWidth, arcHeight);

        if (originalImage != null) {
            FontMetrics fm = g2.getFontMetrics();
            int padding = borderWidth + 8;

            int availableW = width - (padding * 2);
            int availableH = height - (padding * 2) - fm.getHeight();

            double ratio = Math.min((double) availableW / originalImage.getWidth(), (double) availableH / originalImage.getHeight());
            int drawW = (int) (originalImage.getWidth() * ratio);
            int drawH = (int) (originalImage.getHeight() * ratio);

            int imgX = (width - drawW) / 2;
            int imgY = padding + (availableH - drawH) / 2;

            BufferedImage scaledImg = (cachedScaledImage != null) ? cachedScaledImage.get() : null;
            if (scaledImg == null || cachedDrawW != drawW || cachedDrawH != drawH) {
                scaledImg = new BufferedImage(drawW, drawH, BufferedImage.TYPE_INT_ARGB);
                Graphics2D gImg = scaledImg.createGraphics();
                gImg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                gImg.drawImage(originalImage, 0, 0, drawW, drawH, null);
                gImg.dispose();

                cachedScaledImage = new SoftReference<>(scaledImg);
                cachedDrawW = drawW;
                cachedDrawH = drawH;
            }

            Shape oldClip = g2.getClip();
            RoundRectangle2D imageClip = new RoundRectangle2D.Float(imgX, imgY, drawW, drawH, arcWidth / 2f, arcHeight / 2f);
            g2.clip(imageClip);

            g2.drawImage(scaledImg, imgX, imgY, null);

            g2.setClip(oldClip);
        }

        g2.setStroke(new BasicStroke(borderWidth));
        g2.setColor(drawBorder);
        g2.drawRoundRect(borderWidth/2, borderWidth/2, width - borderWidth, height - borderWidth, arcWidth, arcHeight);

        FontMetrics fm = g2.getFontMetrics();
        int textX = (width - fm.stringWidth(getText())) / 2;
        int textY = height - fm.getDescent() - 8;

        g2.setColor(textColor);
        g2.drawString(getText(), textX, textY);

        g2.dispose();
    }

    public String getId() {
        return id;
    }
}