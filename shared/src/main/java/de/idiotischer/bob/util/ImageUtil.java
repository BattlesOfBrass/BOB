package de.idiotischer.bob.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.VolatileImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class ImageUtil {
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

    public static VolatileImage btv(BufferedImage image) {
        if (image == null) return null;

        GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();

        VolatileImage volatileImage = gc.createCompatibleVolatileImage(image.getWidth(), image.getHeight(), Transparency.TRANSLUCENT);

        Graphics2D g2 = volatileImage.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);

        g2.drawImage(image, 0, 0, null);
        g2.dispose();

        return volatileImage;
    }
    private static BufferedImage toIntARGB(BufferedImage img) {
        if (img.getType() == BufferedImage.TYPE_INT_ARGB) {
            return img;
        }

        BufferedImage converted = new BufferedImage(
                img.getWidth(), img.getHeight(),
                BufferedImage.TYPE_INT_ARGB
        );

        converted.getGraphics().drawImage(img, 0, 0, null);
        return converted;
    }

    public static boolean isSame(BufferedImage img1, BufferedImage img2) {
        if (img1.getWidth() != img2.getWidth() || img1.getHeight() != img2.getHeight()) {
            return false;
        }

        img1 = toIntARGB(img1);
        img2 = toIntARGB(img2);

        return java.util.Arrays.equals(
                ((DataBufferInt) img1.getRaster().getDataBuffer()).getData(),
                ((DataBufferInt) img2.getRaster().getDataBuffer()).getData()
        );
    }

    public static void set(BufferedImage image, int x, int y, int rgb) {
        int[] pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        pixels[y * image.getWidth() + x] = rgb;
    }

    //TODO: optimize too but the way from the set method break the get for some reason so we cant use that
    public static int get(BufferedImage image, int x, int y) {
        return image.getRGB(x, y);
    }

    //public static boolean isSame(BufferedImage img1, BufferedImage img2) throws IOException {
    //    if (img1.getWidth() != img2.getWidth() || img1.getHeight() != img2.getHeight()) {
    //        return false;
    //    }
//
    //    for (int y = 0; y < img1.getHeight(); y++) {
    //        for (int x = 0; x < img1.getWidth(); x++) {
    //            if (img1.getRGB(x, y) != img2.getRGB(x, y)) {
    //                return false;
    //            }
    //        }
    //    }
//
    //    return true;
    //}

    private BufferedImage scaleFit(BufferedImage src, int targetHeight) {
        double scale = (double) targetHeight / src.getHeight();
        int newWidth = (int) (src.getWidth() * scale);

        Image scaled = src.getScaledInstance(newWidth, targetHeight, Image.SCALE_SMOOTH);

        BufferedImage out = new BufferedImage(newWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2d = out.createGraphics();
        g2d.drawImage(scaled, 0, 0, null);
        g2d.dispose();

        return out;
    }

    public static BufferedImage makeRoundedCorner(BufferedImage image, int cornerRadius) {
        int w = image.getWidth();
        int h = image.getHeight();

        BufferedImage output = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = output.createGraphics();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.drawImage(image, 0, 0, null);

        BufferedImage mask = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gMask = mask.createGraphics();

        gMask.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        gMask.setColor(Color.WHITE);
        gMask.fill(new RoundRectangle2D.Float(0, 0, w, h, cornerRadius, cornerRadius));
        gMask.dispose();

        g2.setComposite(AlphaComposite.DstIn);
        g2.drawImage(mask, 0, 0, null);

        g2.dispose();
        return output;
    }

    public static BufferedImage deepCopyAndFit(BufferedImage source, int targetHeight) {
        double scale = (double) targetHeight / source.getHeight();
        int newWidth = (int) (source.getWidth() * scale);

        BufferedImage copy = new BufferedImage(newWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g = copy.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.drawImage(source, 0, 0, newWidth, targetHeight, null);
        g.dispose();

        return copy;
    }

    public static BufferedImage deepCopy(BufferedImage source) {
        BufferedImage copy = new BufferedImage(
                source.getWidth(),
                source.getHeight(),
                BufferedImage.TYPE_INT_ARGB
        );

        Graphics2D g = copy.createGraphics();
        g.drawImage(source, 0, 0, null);
        g.dispose();

        return copy;
    }
    //public static int scaleUniform(JPanel p, int designW, int designH, int value) {
    //    double scaleX = (double) p.getWidth() / designW;
    //    double scaleY = (double) p.getHeight() / designH;
    //    return (int) (value * Math.min(scaleX, scaleY));
    //}
//
    //public static int scaleUniformWidth(JPanel p, int designW, int designH, int originalWidth) {
    //    double scaleX = (double) p.getWidth() / designW;
    //    double scaleY = (double) p.getHeight() / designH;
    //    double scale = Math.min(scaleX, scaleY);
//
    //    return (int) (originalWidth * scale);
    //}
//
    //public static int scaleUniformHeight(JPanel p, int designW, int designH, int originalHeight) {
    //    double scaleX = (double) p.getWidth() / designW;
    //    double scaleY = (double) p.getHeight() / designH;
    //    double scale = Math.min(scaleX, scaleY);
//
    //    return (int) (originalHeight * scale);
    //}


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

    public static void writeImage(ByteBuffer buffer, BufferedImage image) throws IOException {
        if (image == null) {
            buffer.putInt(0);
            return;
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);

        byte[] imageData = baos.toByteArray();

        buffer.putInt(imageData.length);
        buffer.put(imageData);
    }

    public static BufferedImage readImage(ByteBuffer buffer) {
        int imageLength = buffer.getInt();

        if (imageLength <= 0) {
            return null;
        }

        byte[] imageData = new byte[imageLength];
        buffer.get(imageData);

        try {
            return ImageIO.read(new ByteArrayInputStream(imageData));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static VolatileImage createLowMipmap(VolatileImage source, GraphicsConfiguration config) {
        BufferedImage mipmap = config.createCompatibleImage(source.getWidth(), source.getHeight(), source.getTransparency());
        Graphics2D g = mipmap.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(source, 0, 0, source.getWidth(), source.getHeight(), 0, 0, source.getWidth(), source.getHeight(), null);
        g.dispose();

        return ImageUtil.btv(mipmap);
    }

    public static BufferedImage createLowMipmap(BufferedImage source, GraphicsConfiguration config) {
        BufferedImage mipmap = config.createCompatibleImage(source.getWidth(), source.getHeight(), source.getTransparency());
        Graphics2D g = mipmap.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(source, 0, 0, source.getWidth(), source.getHeight(), 0, 0, source.getWidth(), source.getHeight(), null);
        g.dispose();

        return mipmap;
    }
}
