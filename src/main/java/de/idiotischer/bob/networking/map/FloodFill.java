package de.idiotischer.bob.networking.map;

import de.idiotischer.bob.networking.BOB;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

public class FloodFill {

    public static void fill(BufferedImage surface, int x, int y, Color newColor) {
        //x = Math.round(x);
        //y = Math.round(y);

        Stack<int[]> stack = new Stack<>();
        stack.push(new int[]{x, y});

        if (x > 0 && y > 0 && x < surface.getWidth() && y < surface.getHeight()) {
            Color oldColor = new Color(surface.getRGB(x, y), true);
            if (oldColor.equals(newColor)) return;

            for(Point p : getPossiblePos(surface,x,y)) {
                int px = (int) p.getX(), py = (int) p.getY();

                Color currentColor = new Color(surface.getRGB(px, py), true);

                if(BOB.getInstance().getScenarioSceneLoader().getTakenColors().stream().anyMatch(c -> c.equals(currentColor))) {
                    //border füllen wenn nikkis von uns daneben yay
                }

                if (!currentColor.equals(oldColor)) continue;

                surface.setRGB(px, py, newColor.getRGB());
            }
        }

    }

    public static List<Point> getPossiblePos(BufferedImage surface, int x, int y) {
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
            if (currentRGB != oldRGB) continue;

            posList.add(new Point(px, py));

            stack.push(new int[]{px + 1, py});
            stack.push(new int[]{px - 1, py});
            stack.push(new int[]{px, py + 1});
            stack.push(new int[]{px, py - 1});
        }

        return posList;
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
                if (BOB.getInstance().getCountries().colorToCountry(refColor.getRed(), refColor.getGreen(), refColor.getBlue()) == null) {
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
                if (BOB.getInstance().getCountries().colorToCountry(refC.getRed(), refC.getGreen(), refC.getBlue()) == null) {
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
                if (BOB.getInstance().getCountries().colorToCountry(currentColor.getRed(), currentColor.getGreen(), currentColor.getBlue()) == null) {
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
                } else if (BOB.getInstance().getCountries().colorToCountry(c.getRed(), c.getGreen(), c.getBlue()) == null) {
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
                        if (BOB.getInstance().getCountries().colorToCountry(neighbor.getRed(), neighbor.getGreen(), neighbor.getBlue()) == null) {
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
