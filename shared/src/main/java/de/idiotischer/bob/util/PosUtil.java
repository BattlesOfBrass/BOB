package de.idiotischer.bob.util;

import de.idiotischer.bob.scenario.Scenario;
import de.idiotischer.bob.tile.Tile;
import de.idiotischer.bob.tile.TileResolver;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

//TODO: wenn das dann über tiles läuft wird der flood algorithmus angepasst und dann cachen wa ditte solange dasselbe szenario aktiv ist
public class PosUtil {

    private static Scenario lastUsed;
    private static Map<Tile, List<Point>> cachedPoints = new HashMap<>();

    //weil wir nen sos like game machen brauchen wir halt keine logic für einzelne pixel
    //WICHTIG: das bleibt bitte mit color, nicht mit tiles, das würde auch nicht wirklich gehen
    public static List<Point> getPossiblePos(
            Set<Integer> unusableRgb,
            BufferedImage surface,
            int startX,
            int startY
    ) {
        int width = surface.getWidth();
        int height = surface.getHeight();

        boolean[][] visited = new boolean[width][height];

        Deque<int[]> stack = new ArrayDeque<>();

        List<Point> result = new ArrayList<>();

        if (startX < 0 || startY < 0 || startX >= width || startY >= height) {
            return result;
        }

        int startRgb = surface.getRGB(startX, startY);
        stack.push(new int[]{startX, startY});

        while (!stack.isEmpty()) {
            int[] p = stack.pop();
            int x = p[0];
            int y = p[1];

            if (x < 0 || y < 0 || x >= width || y >= height) continue;

            if (visited[x][y]) continue;
            visited[x][y] = true;

            int rgb = surface.getRGB(x, y);

            if (unusableRgb.contains(rgb)) {
                continue;
            }

            result.add(new Point(x, y));

            stack.push(new int[]{x + 1, y});
            stack.push(new int[]{x - 1, y});
            stack.push(new int[]{x, y + 1});
            stack.push(new int[]{x, y - 1});
        }

        return result;
    }

    public static java.util.List<Point> getPossibleBDPos(List<Color> unusables, BufferedImage surface, int x, int y) {
        if (lastUsed == null && !cachedPoints.isEmpty()/*&& !TileManager.getTileAt(x,y bzw den tile als lokales objekt)*/)
            cachedPoints.clear();
        if (cachedPoints.isEmpty()) {/* am ende füllen */}

        int width = surface.getWidth();
        int height = surface.getHeight();

        boolean[][] visited = new boolean[width][height];
        Stack<int[]> stack = new Stack<>();
        List<Point> posList = new ArrayList<>();

        int oldRGB = surface.getRGB(x, y);
        stack.push(new int[]{x, y});

        while (!stack.isEmpty()) {
            int[] point = stack.pop();
            int px = point[0], py = point[1];

            if (px < 0 || py < 0 || px >= width || py >= height) continue;

            if (visited[px][py]) continue;
            visited[px][py] = true;

            int currentRGB = surface.getRGB(px, py);
            //if (currentRGB != oldRGB) continue;
            if (unusables.stream().anyMatch(unusable -> unusable.getRGB() == currentRGB)) {
                posList.add(new Point(px, py));
                continue;
            }

            stack.push(new int[]{px + 1, py});
            stack.push(new int[]{px - 1, py});
            stack.push(new int[]{px, py + 1});
            stack.push(new int[]{px, py - 1});
        }

        return posList;
    }

    public static java.util.List<Point> getPossibleOuterBDPos(List<Color> unusables, TileResolver r, BufferedImage surface, int x, int y) {
        if (lastUsed == null && !cachedPoints.isEmpty()/*&& !TileManager.getTileAt(x,y bzw den tile als lokales objekt)*/)
            cachedPoints.clear();
        if (cachedPoints.isEmpty()) {/* am ende füllen */}

        int width = surface.getWidth();
        int height = surface.getHeight();

        boolean[][] visited = new boolean[width][height];
        Stack<int[]> stack = new Stack<>();
        List<Point> posList = new ArrayList<>();

        int oldRGB = surface.getRGB(x, y);
        stack.push(new int[]{x, y});

        while (!stack.isEmpty()) {
            int[] point = stack.pop();
            int px = point[0], py = point[1];

            if (px < 0 || py < 0 || px >= width || py >= height) continue;

            if (visited[px][py]) continue;
            visited[px][py] = true;

            int currentRGB = surface.getRGB(px, py);
            //if (currentRGB != oldRGB) continue;
            if (unusables.stream().anyMatch(unusable -> unusable.getRGB() == currentRGB)) {
                Tile s1 = r.fromPos(px + 1, py);
                Tile s2 = r.fromPos(px - 1, py);
                Tile s3 = r.fromPos(px, py + 1);
                Tile s4 = r.fromPos(px, py - 1);

                if(s4 != null || s1 != null || s2 != null || s3 != null) {
                    posList.add(new Point(px, py));
                }
                continue;
            }

            stack.push(new int[]{px + 1, py});
            stack.push(new int[]{px - 1, py});
            stack.push(new int[]{px, py + 1});
            stack.push(new int[]{px, py - 1});
        }

        return posList;
    }



    public static boolean different(int rgb, int oldRGB, List<Color> takenColors) {
        /*maybe für coolore border hier dann nochmal denselben check instanziieren*/
        return rgb != oldRGB && !takenColors.contains(new Color(rgb, true));
    }

    public List<Point> getPossiblePos(List<Color> unusables, @NotNull TileResolver resolver, BufferedImage surface, int x, int y) {
        Tile tile = resolver.fromPos(x, y);

        if (surface == null) return Collections.emptyList();

        Set<Point> result = new HashSet<>();

        if (tile.getPoints() == null) return Collections.emptyList();

        for (Point p : tile.getPoints()) {
            List<Point> possible = PosUtil.getPossiblePos(unusables.stream().map(Color::getRGB).collect(Collectors.toSet()), surface, p.x, p.y);
            result.addAll(possible);
        }

        return new ArrayList<>(result);
    }

    public static int round(double d){
        double dAbs = Math.abs(d);
        int i = (int) dAbs;
        double result = dAbs - (double) i;
        if(result<0.5){
            return d<0 ? -i : i;
        }else{
            return d<0 ? -(i+1) : i+1;
        }
    }
}
