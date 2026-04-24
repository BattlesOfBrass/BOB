package de.idiotischer.bob.map;

import de.idiotischer.bob.BOB;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

//TODO: when states are fully implemented in the json use them instead of comparign colors
public class FloodFill {
    //private static final ExecutorService EXECUTOR_SERVICE = Executors.newCachedThreadPool();

    //public static void fillAsync(BufferedImage src, List<Point> points, Color color) {
    //    EXECUTOR_SERVICE.submit(() -> {
    //        for (Point p : points) {
    //            fill(src, p.x, p.y, color);
    //        }
    //    });
    //}

    //public static void fillAsync(BufferedImage src, int x, int y, Color color) {
    //    EXECUTOR_SERVICE.submit(() -> {
    //        fill(src, x, y, color);
    //    });
    //}

    public static void fillAll(BufferedImage surface, BufferedImage ref, List<Point> points, Color newColor) {
        Set<Color> takenColors = new HashSet<>(BOB.getInstance()
                .getScenarioSceneLoader()
                .getTakenColors());

        points.forEach(point -> {
            fill(surface, point.x, point.y, ref, newColor,takenColors);
        });

        BOB.getInstance().getMainRenderer().syncBuffers();
    }

    public static void fillAll(BufferedImage surface, List<Point> points, Color newColor) {
        fillAll(surface, surface, points, newColor);
    }

    public static void fill(BufferedImage surface, int x, int y, BufferedImage refMap, Color newColor, Set<Color> takenColors) {
        int width = surface.getWidth();
        int height = surface.getHeight();

        if (x < 0 || y < 0 || x >= width || y >= height) return;

        int oldRGB = refMap.getRGB(x, y);
        Color old = new Color(oldRGB, true);

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

            int currentRGB = refMap.getRGB(px, py);
            Color currentColor = new Color(currentRGB, true);

            visited[px][py] = true;

            if (takenColors.contains(currentColor)) {
                //List<Color> neighbors = new ArrayList<>();

                //if (py + 1 < height) neighbors.add(new Color(refMap.getRGB(px, py + 1), true));
                //if (py - 1 >= 0) neighbors.add(new Color(refMap.getRGB(px, py - 1), true));
                //if (px + 1 < width)  neighbors.add(new Color(refMap.getRGB(px + 1, py), true));

                //if(anyDifferent(neighbors, old, takenColors))
                //    surface.setRGB(px, py, old.darker().getRGB());
                //else
                //    //TODO: behvior is bissl ass
                //    surface.setRGB(px, py, BOB.getInstance().getScenarioSceneLoader().getBorderColors().getFirst().getRGB());

                continue;
            }

            if (currentRGB != oldRGB) continue;

            surface.setRGB(px, py, newRGB);

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
        if(!takenColors.contains(rgb)) return true;
        return rgb != old;
    }

    public static void fillBD(BufferedImage surface, int x, int y, BufferedImage refMap, Color newColor, Set<Color> takenColors) {
        int width = surface.getWidth();
        int height = surface.getHeight();

        if (x < 0 || y < 0 || x >= width || y >= height) return;

        int oldRGB = refMap.getRGB(x, y);
        int newRGB = newColor.getRGB();

        if (oldRGB == newRGB) return;

        boolean[][] visited = new boolean[width][height];
        Stack<int[]> stack = new Stack<>();
        stack.push(new int[]{x, y});

        while (!stack.isEmpty()) {
            int[] p = stack.pop();
            int px = p[0];
            int py = p[1];

            if (px < 0 || py < 0 || px >= width || py >= height) continue;
            if (visited[px][py]) continue;

            int currentRGB = refMap.getRGB(px, py);
            Color currentColor = new Color(currentRGB, true);

            if (!takenColors.contains(currentColor)) {
                continue;
            }

            visited[px][py] = true;

            boolean isBorder = false;

            int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
            for (int[] d : dirs) {
                int nx = px + d[0];
                int ny = py + d[1];

                if (nx < 0 || ny < 0 || nx >= width || ny >= height) {
                    break;
                }

                Color neighbor = new Color(refMap.getRGB(nx, ny), true);

                if(takenColors.contains(neighbor)) continue;
                if(neighbor.getRGB() == newRGB) continue;
                if(neighbor.getRGB() == oldRGB) continue;

                isBorder = true;
            }

            if (isBorder) {
                surface.setRGB(px, py, newRGB);
            }

            stack.push(new int[]{px + 1, py});
            stack.push(new int[]{px - 1, py});
            stack.push(new int[]{px, py + 1});
            stack.push(new int[]{px, py - 1});
        }

        BOB.getInstance().getMainRenderer().syncBuffers();
    }

    public static void fillBorder(BufferedImage surface, BufferedImage referenceSurface, int startX, int startY, Color borderColor) {
        Color oldColor = new Color(referenceSurface.getRGB(startX, startY), true);

        Stack<int[]> stack = new Stack<>();
        stack.push(new int[]{startX, startY});

        Set<String> visited = new HashSet<>();
        List<int[]> border = new ArrayList<>();

        int width = surface.getWidth();
        int height = surface.getHeight();

        while (!stack.isEmpty()) {
            int[] point = stack.pop();
            int x = point[0], y = point[1];
            String key = x + "," + y;
            if (visited.contains(key)) continue;
            visited.add(key);

            Color currentColor = new Color(referenceSurface.getRGB(x, y), true);
            if (!currentColor.equals(oldColor)) continue;

            boolean isBorder = false;

            int[][] neighbors = {{x + 1, y}, {x - 1, y}, {x, y + 1}, {x, y - 1}};
            for (int[] n : neighbors) {
                int nx = n[0], ny = n[1];
                if (nx < 0 || nx >= width || ny < 0 || ny >= height) {
                    isBorder = true;
                    surface.setRGB(nx, ny, borderColor.getRGB());
                    break;
                }
                Color neighborColor = new Color(referenceSurface.getRGB(nx, ny), true);
                if (!neighborColor.equals(oldColor)) {
                    isBorder = true;

                    //if border pixel != borders pixel that is differently colored

                    int[][] neighbors1 = {{nx + 1, ny}, {nx - 1, ny}, {nx, ny + 1}, {nx, ny - 1}};

                    //for(int[] neighbor : neighbors1) {
                    //    int nx1 = neighbor[0], ny1 = neighbor[1];
                    //    Color neighborColor1 = new Color(referenceSurface.getRGB(nx1, ny1), true);
                    //    if(!neighborColor1.equals(oldColor)) continue;
                    //    surface.setRGB(nx, ny, borderColor.getRGB());
                    //}

                    break;
                }
            }

            if (isBorder) {
                border.add(new int[]{x, y});
            }

            for (int[] n : neighbors) {
                int nx = n[0], ny = n[1];
                if (nx < 0 || nx >= width || ny < 0 || ny >= height) continue;
                String nKey = nx + "," + ny;
                if (!visited.contains(nKey)) {
                    stack.push(new int[]{nx, ny});
                }
            }
        }

        //for (int[] pixel : border) {
        //    int x = pixel[0], y = pixel[1];
        //    surface.setRGB(x, y, borderColor.getRGB());
        //}
    }

    //TODO: fix for less overhead
    public static void fillWithBorder(BufferedImage surface, BufferedImage referenceSurface, int x, int y, Color newColor) {
        //x = Math.round(x);
        //y = Math.round(y);

        Color oldColor = new Color(surface.getRGB(x, y), true);
        Color borderColor = new Color(
                (int)(newColor.getRed() / 1.4),
                (int)(newColor.getGreen() / 1.4),
                (int)(newColor.getBlue() / 1.4)
        );

        Stack<int[]> stack = new Stack<>();
        stack.push(new int[]{x, y});

        List<int[]> border = new ArrayList<>();
        Set<String> visited = new HashSet<>();

        while (!stack.isEmpty()) {
            int[] point = stack.pop();
            int px = point[0], py = point[1];
            String key = px + "," + py;
            if (visited.contains(key)) continue;
            visited.add(key);

            Color currentColor = new Color(surface.getRGB(px, py), true);
            if (!currentColor.equals(oldColor)) {
                Color refColor = new Color(referenceSurface.getRGB(px, py), true);
                if (BOB.getInstance().getCountryManager().colorToCountry(refColor.getRed(), refColor.getGreen(), refColor.getBlue()) == null) {
                    border.add(new int[]{px, py});
                }
                continue;
            }

            surface.setRGB(px, py, newColor.getRGB());

            stack.push(new int[]{px + 1, py});
            stack.push(new int[]{px - 1, py});
            stack.push(new int[]{px, py + 1});
            stack.push(new int[]{px, py - 1});

            int[][] extraPositions = {
                    {px + 1, py + 1}, {px - 1, py + 1},
                    {px + 1, py - 1}, {px - 1, py - 1}
            };
            for (int[] pos : extraPositions) {
                int ex = pos[0], ey = pos[1];
                if (ex < 0 || ex >= surface.getWidth() || ey < 0 || ey >= surface.getHeight()) continue;
                Color refC = new Color(referenceSurface.getRGB(ex, ey), true);
                if (BOB.getInstance().getCountryManager().colorToCountry(refC.getRed(), refC.getGreen(), refC.getBlue()) == null) {
                    stack.push(pos);
                }
            }
        }

        for (int[] pixel : border) {
            int px = pixel[0], py = pixel[1];
            Color refC = new Color(referenceSurface.getRGB(px, py), true);
            if (refC.equals(Color.BLACK)) {
                surface.setRGB(px, py, Color.BLACK.getRGB());
            } else if (refC.equals(new Color(126, 142, 158))) {
            } else {
                surface.setRGB(px, py, borderColor.getRGB());
            }
        }
    }

    public static void fillFixBorder(BufferedImage surface, int x, int y, Color newColor) {
        //x = Math.round(x);
        //y = Math.round(y);

        Color oldColor = new Color(surface.getRGB(x, y), true);
        Color borderColor = new Color(
                (int)(newColor.getRed() / 1.4),
                (int)(newColor.getGreen() / 1.4),
                (int)(newColor.getBlue() / 1.4)
        );

        Stack<int[]> stack = new Stack<>();
        stack.push(new int[]{x, y});
        List<int[]> border = new ArrayList<>();
        Set<String> visited = new HashSet<>();

        while (!stack.isEmpty()) {
            int[] point = stack.pop();
            int px = point[0], py = point[1];
            String key = px + "," + py;
            if (visited.contains(key)) continue;
            visited.add(key);

            Color currentColor = new Color(surface.getRGB(px, py), true);
            if (!currentColor.equals(oldColor)) {
                if (BOB.getInstance().getCountryManager().colorToCountry(currentColor.getRed(), currentColor.getGreen(), currentColor.getBlue()) == null) {
                    border.add(new int[]{px, py});
                }
                continue;
            }

            surface.setRGB(px, py, newColor.getRGB());

            stack.push(new int[]{px + 1, py});
            stack.push(new int[]{px - 1, py});
            stack.push(new int[]{px, py + 1});
            stack.push(new int[]{px, py - 1});

            int[][] extraPositions = {
                    {px + 1, py + 1}, {px - 1, py + 1},
                    {px + 1, py - 1}, {px - 1, py - 1}
            };

            for (int[] pos : extraPositions) {
                int ex = pos[0], ey = pos[1];
                if (ex < 0 || ex >= surface.getWidth() || ey < 0 || ey >= surface.getHeight()) continue;
                Color c = new Color(surface.getRGB(ex, ey), true);

                if (c.equals(Color.BLACK)) {
                    stack.push(pos);
                } else if (BOB.getInstance().getCountryManager().colorToCountry(c.getRed(), c.getGreen(), c.getBlue()) == null) {
                    if (!(c.getRed() == c.getGreen() && c.getGreen() == c.getBlue())) {
                        if (!c.equals(newColor) && !c.equals(oldColor) && !c.equals(new Color(126, 142, 158))) {
                            stack.push(pos);
                        }
                    }
                }
            }
        }

        BufferedImage mapCopy = new BufferedImage(surface.getWidth(), surface.getHeight(), surface.getType());

        for (int[] pixel : border) {
            int px = pixel[0], py = pixel[1];
            List<Color> colors = new ArrayList<>();

            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    int nx = px + i - 1;
                    int ny = py + j - 1;

                    if (nx < 0 || nx >= surface.getWidth() || ny < 0 || ny >= surface.getHeight()) {
                        colors.add(Color.BLACK);
                        continue;
                    }

                    Color neighbor = new Color(surface.getRGB(nx, ny), true);
                    if (!colors.contains(neighbor)) {
                        if (BOB.getInstance().getCountryManager().colorToCountry(neighbor.getRed(), neighbor.getGreen(), neighbor.getBlue()) == null) {
                            if (neighbor.equals(new Color(105, 118, 132)) || neighbor.equals(new Color(126, 142, 158))) {
                                colors.add(neighbor);
                            }
                        } else {
                            colors.add(neighbor);
                        }
                    }
                }
            }

            if (colors.size() == 1) {
                mapCopy.setRGB(px, py, borderColor.getRGB());
            } else {
                mapCopy.setRGB(px, py, Color.BLACK.getRGB());
            }
        }

        surface.getGraphics().drawImage(mapCopy, 0, 0, null);
    }

    public static BufferedImage fixBorders(BufferedImage map, List<Color> toChange, List<Color> toIgnore) {
        BufferedImage mapCopy = new BufferedImage(map.getWidth(), map.getHeight(), map.getType());

        int[][] toCheck = {
                {-1, -1}, {0, -1}, {1, -1},
                {-1, 0}, {1, 0},
                {-1, 1}, {0, 1}, {1, 1}
        };

        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                Color current = new Color(map.getRGB(x, y), true);

                if (!toChange.contains(current) || toIgnore.contains(current)) continue;

                List<Color> colors = new ArrayList<>();

                for (int[] offset : toCheck) {
                    int nx = x + offset[0], ny = y + offset[1];
                    if (nx < 0 || nx >= map.getWidth() || ny < 0 || ny >= map.getHeight()) {
                        colors.add(Color.BLACK);
                        continue;
                    }

                    Color neighbor = new Color(map.getRGB(nx, ny), true);
                    if (!colors.contains(neighbor) && !toChange.contains(neighbor)) {
                        colors.add(neighbor);
                    }
                }

                if (colors.size() == 1) {
                    Color c = colors.getFirst();
                    Color newC = new Color((int)(c.getRed() / 1.4), (int)(c.getGreen() / 1.4), (int)(c.getBlue() / 1.4));
                    mapCopy.setRGB(x, y, newC.getRGB());
                } else {
                    mapCopy.setRGB(x, y, current.getRGB());
                }
            }
        }

        return mapCopy;
    }
}
// private static final Queue<FloodTask> TASKS = new ConcurrentLinkedQueue<>();
//
//    private static class FloodTask {
//        BufferedImage surface;
//        int x, y;
//        Color newColor;
//
//        FloodTask(BufferedImage surface, int x, int y, Color newColor) {
//            this.surface = surface;
//            this.x = x;
//            this.y = y;
//            this.newColor = newColor;
//        }
//    }
//
//    public static void fill(BufferedImage surface, int x, int y, Color newColor) {
//        TASKS.add(new FloodTask(surface, x, y, newColor));
//    }
//
//    public static void fillAll(BufferedImage surface, List<Point> points, Color newColor) {
//        points.forEach(p -> fill(surface, p.x, p.y, newColor));
//    }
//
//    public static void processQueue(int maxTasksPerTick) {
//        int processed = 0;
//
//        while (processed < maxTasksPerTick) {
//            FloodTask task = TASKS.poll();
//            if (task == null) return;
//
//            executeFlood(task.surface, task.x, task.y, task.newColor);
//            processed++;
//        }
//    }
//
//    private static void executeFlood(BufferedImage surface, int x, int y, Color newColor) {
//        int width = surface.getWidth();
//        int height = surface.getHeight();
//
//        if (x < 0 || y < 0 || x >= width || y >= height) return;
//
//        int oldRGB = surface.getRGB(x, y);
//        int newRGB = newColor.getRGB();
//
//        if (oldRGB == newRGB) return;
//
//        boolean[][] visited = new boolean[width][height];
//        Queue<int[]> queue = new ArrayDeque<>();
//
//        queue.add(new int[]{x, y});
//
//        while (!queue.isEmpty()) {
//            int[] p = queue.poll();
//            int px = p[0];
//            int py = p[1];
//
//            if (px < 0 || py < 0 || px >= width || py >= height) continue;
//            if (visited[px][py]) continue;
//
//            int currentRGB = surface.getRGB(px, py);
//            Color currentColor = new Color(currentRGB, true);
//
//            if (BOB.getInstance()
//                    .getScenarioSceneLoader()
//                    .getTakenColors()
//                    .stream()
//                    .anyMatch(c -> c.equals(currentColor))) {
//                continue;
//            }
//
//            if (currentRGB != oldRGB) continue;
//
//            visited[px][py] = true;
//            surface.setRGB(px, py, newRGB);
//
//            queue.add(new int[]{px + 1, py});
//            queue.add(new int[]{px - 1, py});
//            queue.add(new int[]{px, py + 1});
//            queue.add(new int[]{px, py - 1});
//        }
//    }