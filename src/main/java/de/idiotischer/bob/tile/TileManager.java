package de.idiotischer.bob.tile;

import de.idiotischer.bob.BOB;
import de.idiotischer.bob.Server;
import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.networking.packet.impl.pp.RequestPacket;
import de.idiotischer.bob.networking.packet.impl.pp.Type;
import de.idiotischer.bob.render.menu.impl.select.ScenarioSelectMenu;
import de.idiotischer.bob.util.PosUtil;
import it.unimi.dsi.fastutil.Pair;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class TileManager implements TileResolver {

    private final Set<Tile> tileSet = new HashSet<>();
    private CompletableFuture<Void> awaitingFuture;
    private final Map<Tile, Set<Point>> cache = new ConcurrentHashMap<>();

    private final ExecutorService cacheExecutor = Executors.newCachedThreadPool();

    //MUSS nach CountryManager initialisiert werden sonst BOOM
    public TileManager() {
        //reload();
    }

    public CompletableFuture<Void> reload() {
        awaitingFuture = new CompletableFuture<>();

        tileSet.clear();
        cache.clear();

        BOB.getInstance().getSendTool().send(BOB.getInstance().getClient().getChannel(), new RequestPacket(Type.TILES_SYNC,""));

        return awaitingFuture;
    }

    public boolean has(String tileAbbreviation) {
        return getTileSet().stream().map(Tile::getAbbreviation).collect(Collectors.toSet()).contains(tileAbbreviation);
    }

    public boolean has(Tile tile) {
        return has(tile.getAbbreviation());
    }

    public void finishReload() {
        BOB.getInstance().getStateManager().reload();

        if(awaitingFuture == null || awaitingFuture.isDone()) return;
        awaitingFuture.complete(null);
    }

    public Tile registerTile(Tile tile) {
        cache.remove(tile);
        tileSet.remove(tile);
        tileSet.add(tile);

        if(BOB.getInstance().getConfig().isCacheOnRegister()) cache(tile, tile.getPoints());

        return tile;
    }

    private void cache(Tile tile, List<Point> points) {
        cache.remove(tile);

        cacheExecutor.submit(() -> {
            if (BOB.getInstance().isDebug()) System.out.println(tile.getAbbreviation() + " caching started!");

            Set<Point> pointsSet = Collections.newSetFromMap(new ConcurrentHashMap<>());

            List<Color> takenColors = Server.getInstance().getScenarioSceneLoader().getTakenColors();
            BufferedImage logicMap = Server.getInstance().getScenarioSceneLoader().getMap();

            points.parallelStream().forEach(basePoint -> {
                pointsSet.add(basePoint);
                List<Point> expanded = PosUtil.getPossiblePos(takenColors.stream().map(Color::getRGB).collect(Collectors.toSet()), logicMap, basePoint.x, basePoint.y);
                pointsSet.addAll(expanded);
            });

            synchronized (cache) {
                cache.put(tile, pointsSet);
            }

            if (BOB.getInstance().isDebug()) System.out.println(tile.getAbbreviation() + " caching finished!");
        });
    }

    public Set<Tile> findNeighbors(Tile tile) {
        Set<Point> pixels = cache.get(tile);
        if (pixels == null) {
            cache(tile, tile.getPoints());
            return Set.of();
        }

        BufferedImage map = Server.getInstance().getScenarioSceneLoader().getMap();

        int width = map.getWidth();
        int height = map.getHeight();

        int maxBorderThickness = 1; //TODO: make this configurable in a map config

        Set<Integer> borderColors = Server.getInstance().getScenarioSceneLoader().getBorderColors().stream().map(Color::getRGB).collect(Collectors.toSet());

        Set<Tile> neighbors = new HashSet<>();

        int[][] dirs = {
                {-1, 0}, {-1, 1}, {0, 1}, {1, 1},
                {1, 0}, {1, -1}, {0, -1}, {-1, -1}
        };

        for (Point p : pixels) {

            for (int[] dir : dirs) {

                boolean enteredBorder = false;

                for (int distance = 1; distance <= maxBorderThickness + 1/*without +1 it doesnt work for some reason but with it this is great*/ /*+ 20*/; distance++) {

                    int x = p.x + dir[0] * distance;
                    int y = p.y + dir[1] * distance;

                    if (x < 0 || y < 0 || x >= width || y >= height) {
                        break;
                    }

                    int rgb = map.getRGB(x, y);

                    if (borderColors.contains(rgb)) {
                        enteredBorder = true;
                        continue;
                    }

                    Tile other = getTileAt(x, y);

                    if (other == null) {
                        break;
                    }

                    if (other == tile) {
                        break;
                    }

                    if (enteredBorder) {
                        neighbors.add(other);
                    }

                    break;
                }
            }
        }

        return neighbors;
    }

    public List<Pair<String, String>> getTiles(Country country) {
        return tileSet.stream().filter(t -> Objects.equals(t.getController().getAbbreviation(), country.getAbbreviation())).map(t -> Pair.of(t.getAbbreviation(), t.getName())).collect(Collectors.toList());
    }

    public List<String> getTilesControlled(Country country) {
        return tileSet.stream().filter(t -> Objects.equals(t.getController().getAbbreviation(), country.getAbbreviation())).map(Tile::getName).collect(Collectors.toList());
    }

    public List<String> getTiles() {
        return tileSet.stream().map(Tile::getName).collect(Collectors.toList());
    }

    //public Tile getTileAt(int x, int y) {
    //    List<Point> points = PosUtil.getPossiblePos(BOB.getInstance().getMainRenderer().getMap(), x, y);
    //    Map<Point, Tile> tilePoints = getTileSet().stream()
    //            .filter(Objects::nonNull)
    //            .collect(Collectors.toMap(
    //                    s -> new Point(s.getX(), s.getY()),
    //                    Function.identity(),
    //                    (a, b) -> a
    //            ));
    //    Optional<Point> point = points.stream().filter(tilePoints::containsKey).findFirst();

    //    return tilePoints.get(point.orElse(null));
    //}

    //TODO: optimize
    public Tile getTileAt(int x, int y) {
        Point click = new Point(x, y);

        for (Tile tile : tileSet) {
            if (tile == null) continue;

            Set<Point> expandedPoints = cache.get(tile);

            if (expandedPoints != null) {
                if (expandedPoints.contains(click)) return tile;
            } else {
                cache(tile, tile.getPoints());
                if (tile.getPoints().contains(click)) return tile;
            }
        }
        return null;
    }

    public void colorAllDefault() {
        getTileSet().forEach(this::colorTileDefault);
    }

    public void colorTileDefault(Tile tile) {
        if(tile.getController() == null) return;

        SwingUtilities.invokeLater(() -> {
            recolorTile(tile);
        });
    }

    public void changeTile(Tile tile, Country newOwner) {
        if(newOwner == null) return;
        SwingUtilities.invokeLater(() -> {
            recolorTile(tile);
        });
    }

    public Set<Tile> getTileSet() {
        return Collections.unmodifiableSet(tileSet);
    }

    public List<Tile> getTileList() {
        return Collections.unmodifiableList(new ArrayList<>(tileSet));
    }

    public static void recolorTile(Tile tile) {
        List<Color> taken = BOB.getInstance().getScenarioSceneLoader().getTakenColors();

        tile.getPoints().forEach(pos -> {
            //PosUtil.getPossibleBDPos(taken, BOB.getInstance().getMainRenderer().getLogicMap(), pos.x, pos.y).forEach(px -> {
            //    BOB.getInstance().getMainRenderer().getVisualBorderOverlay().setRGB(px.x,px.y, tile.getController().countryColor().darker().getRGB());
            //});
            PosUtil.getPossiblePos(taken.stream().map(Color::getRGB).collect(Collectors.toSet()), BOB.getInstance().getMainRenderer().getLogicMap(), pos.x, pos.y).forEach(px -> {
                BOB.getInstance().getMainRenderer().getLogicMap().setRGB(px.x,px.y, tile.getController().countryColor().getRGB());
            });

            BOB.getInstance().getMainRenderer().getLogicMap().setRGB(pos.x,pos.y, tile.getController().countryColor().getRGB());
            BOB.getInstance().getMainRenderer().syncBuffers();
        });
    }

    public static void recolorTile(Tile tile, Color color) {
        List<Color> taken = BOB.getInstance().getScenarioSceneLoader().getTakenColors();

        tile.getPoints().forEach(pos -> {
            PosUtil.getPossiblePos(taken.stream().map(Color::getRGB).collect(Collectors.toSet()), BOB.getInstance().getMainRenderer().getLogicMap(), pos.x, pos.y).forEach(px -> {
                BOB.getInstance().getMainRenderer().getLogicMap().setRGB(px.x,px.y, color.getRGB());
            });

            BOB.getInstance().getMainRenderer().getLogicMap().setRGB(pos.x,pos.y, color.getRGB());

            BOB.getInstance().getMainRenderer().syncBuffers();
        });
    }

    //private boolean isPointInTile(java.awt.Point p, List<java.awt.Point> polygon) {
    //    boolean result = false;

    //    for (int i = 0, j = polygon.size() - 1; i < polygon.size(); j = i++) {

    //        int xi = polygon.get(i).x;
    //        int yi = polygon.get(i).y;

    //        int xj = polygon.get(j).x;
    //        int yj = polygon.get(j).y;

    //        boolean intersect = ((yi > p.y) != (yj > p.y)) &&
    //                (p.x < (double) (xj - xi) * (p.y - yi) / (double) (yj - yi) + xi);

    //        if (intersect) {
    //            result = !result;
    //        }
    //    }

    //    return result;
    //}

    @Override
    public Tile byAbbreviation(String abbreviation) {
        return tileSet.stream().filter(s -> s.getAbbreviation().equals(abbreviation)).findFirst().orElse(null);
    }

    @Override
    public Tile fromPos(int x, int y) {
        return getTileAt(x,y);
    }

    public CompletableFuture<Void> getAwaitingFuture() {
        return awaitingFuture;
    }

    public void clearTiles() {
        tileSet.clear();
    }
}