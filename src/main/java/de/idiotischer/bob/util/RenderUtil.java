package de.idiotischer.bob.util;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

public class RenderUtil {
    public static BufferedImage makeRoundedCorner(Image image, int width, int height, int cornerRadius) {
        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = output.createGraphics();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        g2.setComposite(AlphaComposite.Src);
        g2.setColor(Color.WHITE);
        g2.fill(new RoundRectangle2D.Float(0, 0, width, height, cornerRadius, cornerRadius));

        g2.setComposite(AlphaComposite.SrcAtop);

        g2.drawImage(image, 0, 0, width, height, null);

        g2.dispose();
        return output;
    }

    public static BufferedImage makeRoundedCorner(BufferedImage image, int cornerRadius) {
        int w = image.getWidth();
        int h = image.getHeight();
        BufferedImage output = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2 = output.createGraphics();

        g2.setComposite(AlphaComposite.Src);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); //this doesnt cause the low quality, prob just the small scale
        g2.setColor(Color.WHITE);
        g2.fill(new RoundRectangle2D.Float(0, 0, w, h, cornerRadius, cornerRadius));

        g2.setComposite(AlphaComposite.SrcAtop);
        g2.drawImage(image, 0, 0, null);

        g2.dispose();

        return output;
    }


    // THESE WORK

    public static int scaleUniform(JPanel p, int designW, int designH, int value) {
        double scaleX = (double) p.getWidth() / designW;
        double scaleY = (double) p.getHeight() / designH;
        return (int) (value * Math.min(scaleX, scaleY));
    }

    public static int scaleUniformWidth(JPanel p, int designW, int designH, int originalWidth) {
        double scaleX = (double) p.getWidth() / designW;
        double scaleY = (double) p.getHeight() / designH;
        double scale = Math.min(scaleX, scaleY);

        return (int) (originalWidth * scale);
    }

    public static int scaleUniformHeight(JPanel p, int designW, int designH, int originalHeight) {
        double scaleX = (double) p.getWidth() / designW;
        double scaleY = (double) p.getHeight() / designH;
        double scale = Math.min(scaleX, scaleY);

        return (int) (originalHeight * scale);
    }


    //private static final int BASE_WIDTH = 1920;
    //private static final int BASE_HEIGHT = 1080;

    //public static int scaleWidth(JPanel p, int originalWidth) {
    //    double scale = (double) p.getWidth() / BASE_WIDTH;
    //    return (int) (originalWidth * scale);
    //}

    //public static int scaleHeight(JPanel p, int originalHeight) {
    //    double scale = (double) p.getHeight() / BASE_HEIGHT;
    //    return (int) (originalHeight * scale);
    //}

    //public static int scaleUniform(JPanel p, int originalSize) {
    //    double scaleX = (double) p.getWidth() / BASE_WIDTH;
    //    double scaleY = (double) p.getHeight() / BASE_HEIGHT;
    //    double scale = Math.min(scaleX, scaleY);
    //    return (int) (originalSize * scale);
    //}
}
