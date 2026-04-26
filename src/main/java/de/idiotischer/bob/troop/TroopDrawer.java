package de.idiotischer.bob.troop;

import de.idiotischer.bob.BOB;

import java.awt.*;
import java.util.List;

//maybe move to render
public class TroopDrawer {

    public static void drawTroops(Graphics2D g2, List<TroopStack> troopStacks) {

        double zoom = BOB.getInstance()
                .getMainRenderer()
                .getCamera()
                .getZoom();

        for (TroopStack troops : troopStacks) {

            if (troops.getOwner() == null) continue;
            if (!troops.isVisible()) continue;

            List<Point> points = troops.getState().getPoints();
            if (points == null || points.isEmpty()) continue;

            Point p = points.getFirst();
            if (p == null) continue;

            int width = (int) Math.round(30 * zoom);
            int height = (int) Math.round(10 * zoom);

            drawBG(g2, troops, p, width, height, zoom);
            drawContents(g2, troops, p, width, height);
        }
    }

    public static void drawBG(Graphics2D g2, TroopStack troopStack, Point p,
                              int width, int height, double zoom) {

        Color c = troopStack.getController().countryColor();

        float stroke = (float) (4 * zoom);

        Stroke oldStroke = g2.getStroke();

        g2.setColor(Color.DARK_GRAY);
        g2.fillRect(p.x, p.y, width, height);

        g2.setColor(c == null ? Color.GREEN : c);
        g2.setStroke(new BasicStroke(stroke));
        g2.drawRect(p.x, p.y, width, height);

        g2.setStroke(oldStroke);
    }

    public static void drawContents(Graphics2D g2, TroopStack troopStack,
                                    Point p, int width, int height) {

        String size = String.valueOf(troopStack.troops().size());

        FontMetrics fm = g2.getFontMetrics();

        int x = p.x + 5;
        int y = p.y + (height - fm.getHeight()) / 2 + fm.getAscent();

        g2.drawString(size, x, y);
    }

    public static void drawExtra(Graphics2D g2, TroopStack troopStack) {

    }
}
