package de.idiotischer.bob.util;

import de.idiotischer.bob.scenario.Scenario;
import de.idiotischer.bob.tile.Tile;
import de.idiotischer.bob.tile.TileResolver;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

//TODO: wenn das dann über tiles läuft wird der flood algorithmus angepasst und dann cachen wa ditte solange dasselbe szenario aktiv ist
public class PosUtil {

    private static Scenario lastUsed;
    private static final Map<Tile, List<Point>> cachedPoints = new HashMap<>();

    //weil wir nen sos like game machen brauchen wir halt keine logic für einzelne pixel
    //WICHTIG: das bleibt bitte mit color, nicht mit tiles, das würde auch nicht wirklich gehen
    //old comemnt

    private static final class Chunk {
        int[] visited = new int[0];
        int[] stack = new int[1024];
        int stamp = 0;
    }

    private static final ThreadLocal<Chunk> CHUNK = ThreadLocal.withInitial(Chunk::new);

    public static List<Point> getPossiblePos(Set<Integer> unusableRgb, BufferedImage surface, int startX, int startY) {
        return getPossiblePos(unusableRgb, surface, List.of(new Point(startX, startY)));
    }

    public static List<Point> getPossiblePos(Set<Integer> unusableRgb, BufferedImage surface, Collection<Point> starts) {
        int width = surface.getWidth();
        int height = surface.getHeight();
        int area = width * height;

        List<Point> result = new ArrayList<>();

        Chunk s = CHUNK.get();

        if (s.visited.length < area) {
            s.visited = new int[area];
            s.stamp = 0;
        }

        if (++s.stamp == Integer.MAX_VALUE) {
            Arrays.fill(s.visited, 0);
            s.stamp = 1;
        }

        int[] visited = s.visited;
        int stamp = s.stamp;
        int[] stack = s.stack;
        int size = 0;

        int type = surface.getType();
        int[] data = null;
        int alpha = 0;

        if (type == BufferedImage.TYPE_INT_ARGB || type == BufferedImage.TYPE_INT_RGB) {
            int[] raw = ((DataBufferInt) surface.getRaster().getDataBuffer()).getData();

            if (raw.length == area) {
                data = raw;
                alpha = type == BufferedImage.TYPE_INT_RGB ? 0xFF000000 : 0;
            }
        }

        int lastRgb = ~0;
        boolean lastUnusable = false;

        for (Point start : starts) {
            if (start.x < 0 || start.y < 0 || start.x >= width || start.y >= height) continue;

            int startIdx = start.y * width + start.x;

            if (visited[startIdx] == stamp) continue;

            visited[startIdx] = stamp;
            stack[size++] = startIdx;

            while (size > 0) {
                int idx = stack[--size];

                int x = idx % width;
                int y = idx / width;

                int rgb = data != null ? data[idx] | alpha : surface.getRGB(x, y);

                if (rgb != lastRgb) {
                    lastRgb = rgb;
                    lastUnusable = unusableRgb.contains(rgb);
                }

                if (lastUnusable) continue;

                result.add(new Point(x, y));

                if (size + 4 > stack.length) stack = Arrays.copyOf(stack, stack.length * 2);

                int nextIdx;

                if (x + 1 < width) {
                    nextIdx = idx + 1;
                    if (visited[nextIdx] != stamp) {
                        visited[nextIdx] = stamp;
                        stack[size++] = nextIdx;
                    }
                }
                if (x > 0) {
                    nextIdx = idx - 1;
                    if (visited[nextIdx] != stamp) {
                        visited[nextIdx] = stamp;
                        stack[size++] = nextIdx;
                    }
                }
                if (y + 1 < height) {
                    nextIdx = idx + width;
                    if (visited[nextIdx] != stamp) {
                        visited[nextIdx] = stamp;
                        stack[size++] = nextIdx;
                    }
                }
                if (y > 0) {
                    nextIdx = idx - width;
                    if (visited[nextIdx] != stamp) {
                        visited[nextIdx] = stamp;
                        stack[size++] = nextIdx;
                    }
                }
            }
        }

        s.stack = stack;
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
        if (surface == null) return Collections.emptyList();

        Tile tile = resolver.fromPos(x, y);
        if (tile == null || tile.getPoints() == null || tile.getPoints().isEmpty()) {
            return Collections.emptyList();
        }

        // 1. Hoist the Set creation outside the loop once
        Set<Integer> unusableRgbSet = unusables.stream()
                .map(Color::getRGB)
                .collect(Collectors.toSet());

        // 2. Pass all points directly to the collection-based flood-fill in one go
        return PosUtil.getPossiblePos(unusableRgbSet, surface, tile.getPoints());
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
