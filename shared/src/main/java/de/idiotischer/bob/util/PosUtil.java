package de.idiotischer.bob.util;

import de.idiotischer.bob.scenario.Scenario;
import de.idiotischer.bob.state.State;
import de.idiotischer.bob.state.StateResolver;
import it.unimi.dsi.fastutil.Pair;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

//TODO: wenn das dann über states läuft wird der flood algorithmus angepasst und dann cachen wa ditte solange dasselbe szenario aktiv ist
public class PosUtil {

    private static Scenario lastUsed;
    private static Map<State, List<Point>> cachedPoints = new HashMap<>();

    //weil wir nen sos like game machen brauchen wir halt keine logic für einzelne pixel
    //WICHTIG: das bleibt bitte mit color, nicht mit states, das würde auch nicht wirklich gehen
    public static java.util.List<Point> getPossiblePos(List<Color> unusables, BufferedImage surface, int x, int y) {
        if (lastUsed == null && !cachedPoints.isEmpty()/*&& !StateManager.getStateAt(x,y bzw den state als lokales objekt)*/)
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



                continue;
            }

            posList.add(new Point(px, py));

            stack.push(new int[]{px + 1, py});
            stack.push(new int[]{px - 1, py});
            stack.push(new int[]{px, py + 1});
            stack.push(new int[]{px, py - 1});
        }

        return posList;
    }

    public static Pair<java.util.List<Point>,java.util.List<Point>> getPossibleBorderPos(List<Color> unusables, BufferedImage surface, int x, int y, StateResolver stateResolver) {
        if (lastUsed == null && !cachedPoints.isEmpty()/*&& !StateManager.getStateAt(x,y bzw den state als lokales objekt)*/)
            cachedPoints.clear();
        if (cachedPoints.isEmpty()) {/* am ende füllen */}

        int width = surface.getWidth();
        int height = surface.getHeight();

        State origin = stateResolver.fromPos(x, y);

        boolean[][] visited = new boolean[width][height];
        Stack<int[]> stack = new Stack<>();
        List<Point> bdPosList = new ArrayList<>();
        List<Point> innerBDPosList = new ArrayList<>();

        if(origin == null) return Pair.of(bdPosList,innerBDPosList);

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
                //TODO: omptimize the constructions of the if statemebnt
                State up = stateResolver.fromPos(px, py + 1);
                State down = stateResolver.fromPos(px, py + 1);
                State left = stateResolver.fromPos(px - 1, py);
                State right = stateResolver.fromPos(px + 1, py);

                if(up != null || down != null || left != null || right != null) {
                    innerBDPosList.add(new Point(px, py));
                } else {
                    bdPosList.add(new Point(px, py));
                }

                stack.push(new int[]{px + 1, py});
                stack.push(new int[]{px - 1, py});
                stack.push(new int[]{px, py + 1});
                stack.push(new int[]{px, py - 1});
            }
        }

        return Pair.of(bdPosList, innerBDPosList);
    }

    public static boolean different(int rgb, int oldRGB, List<Color> takenColors) {
        /*maybe für coolore border hier dann nochmal denselben check instanziieren*/
        return rgb != oldRGB && !takenColors.contains(new Color(rgb, true));
    }

    public List<Point> getPossiblePos(List<Color> unusables, @NotNull StateResolver resolver, BufferedImage surface, int x, int y) {
        State state = resolver.fromPos(x, y);

        if (surface == null) return Collections.emptyList();

        Set<Point> result = new HashSet<>();

        if (state.getPoints() == null) return Collections.emptyList();

        for (Point p : state.getPoints()) {
            List<Point> possible = PosUtil.getPossiblePos(unusables, surface, p.x, p.y);
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
