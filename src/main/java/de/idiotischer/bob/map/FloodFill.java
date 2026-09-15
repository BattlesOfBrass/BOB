package de.idiotischer.bob.map;

import de.idiotischer.bob.BOB;
import de.idiotischer.bob.util.ImageUtil;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

//TODO: when tiles are fully implemented in the json use them instead of comparign colors
public class FloodFill {

    public static void fillAll(BufferedImage surface, BufferedImage ref, List<Point> points, Color newColor) {
        Set<Color> takenColors = new HashSet<>(BOB.getInstance().getScenarioSceneLoader().getTakenColors());

        points.forEach(point -> fill(surface, point.x, point.y, ref, newColor, takenColors));

        BOB.getInstance().getMainRenderer().syncBuffers();
    }

    public static void fill(BufferedImage surface, int x, int y, BufferedImage refMap, Color newColor, Set<Color> takenColors) {
        int width = surface.getWidth();
        int height = surface.getHeight();

        if (x < 0 || y < 0 || x >= width || y >= height) return;

        int oldRGB = ImageUtil.get(refMap, x, y);

        int newRGB = newColor.getRGB();

        if (oldRGB == newRGB) return;

        boolean[][] visited = new boolean[width][height];
        Deque<int[]> stack = new ArrayDeque<>();
        stack.push(new int[]{x, y});

        while (!stack.isEmpty()) {
            int[] p = stack.pop();
            int px = p[0];
            int py = p[1];

            if (px < 0 || py < 0 || px >= width || py >= height) continue;
            if (visited[px][py]) continue;

            int currentRGB = ImageUtil.get(refMap, px, py);
            Color currentColor = new Color(currentRGB, true);

            visited[px][py] = true;

            if (takenColors.contains(currentColor)) {
                continue;
            }

            if (currentRGB != oldRGB) continue;

            ImageUtil.set(surface, px, py, newRGB);

            stack.push(new int[]{px + 1, py});
            stack.push(new int[]{px - 1, py});
            stack.push(new int[]{px, py + 1});
            stack.push(new int[]{px, py - 1});
        }

        BOB.getInstance().getMainRenderer().syncBuffers();
    }

    public static boolean anyDifferent(List<Color> rgb, Color old, Set<Color> takenColors) {
        return rgb.stream().anyMatch(c -> different(c, old, takenColors));
    }

    public static boolean different(Color rgb, Color old, Set<Color> takenColors) {
        if (!takenColors.contains(rgb)) return true;
        return rgb != old;
    }
}