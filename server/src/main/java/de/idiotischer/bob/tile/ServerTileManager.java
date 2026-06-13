package de.idiotischer.bob.tile;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import de.idiotischer.bob.Server;
import de.idiotischer.bob.SharedCore;
import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.networking.packet.impl.TilesSyncPacket;
import de.idiotischer.bob.util.PosUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

//TODO: optimize with parralel streaming and stuff
public class ServerTileManager implements TileResolver {

    private final Set<Tile> tileSet = new HashSet<>();

    private final Map<Tile, Set<Point>> cache = new HashMap<>();

    private final ExecutorService cacheExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private boolean cached;


    public ServerTileManager() {
        //reload();
    }

    public void reload() {
        tileSet.clear();
        cache.clear();

        try (JsonReader reader = new JsonReader(
                Files.newBufferedReader(Server.getInstance().getScenarioSceneLoader().getCurrentScenario().getTilesConfig())
        )) {

            JsonElement root = SharedCore.GSON.fromJson(reader, JsonElement.class);

            root.getAsJsonObject().entrySet().forEach(entry -> {
                String abbreviation = entry.getKey();
                JsonObject tileElement = entry.getValue().getAsJsonObject();

                String controllerString = tileElement.get("controller").getAsString();
                Country country = Server.getInstance()
                        .getCountryManager()
                        .fromAbbreviation(controllerString);

                String name = tileElement.get("name").getAsString();

                List<Point> points = new ArrayList<>();
                JsonElement locationsElement = tileElement.get("locations");

                if (locationsElement != null && locationsElement.isJsonArray()) {
                    for (JsonElement el : locationsElement.getAsJsonArray()) {
                        String[] coords = el.getAsString().split("[,;]");
                        coords[0] = coords[0].trim();
                        coords[1] = coords[1].trim();
                        points.add(new Point(Integer.parseInt(coords[0]), Integer.parseInt(coords[1])));
                    }
                } else if (locationsElement != null && locationsElement.isJsonPrimitive()) {
                    String[] coords = locationsElement.getAsString().split("[,;]");
                    coords[0] = coords[0].trim();
                    coords[1] = coords[1].trim();
                    points.add(new Point(Integer.parseInt(coords[0]), Integer.parseInt(coords[1])));
                } else {
                    points.add(new Point(
                            tileElement.get("x").getAsInt(),
                            tileElement.get("y").getAsInt()
                    ));
                }

                Tile tile = new Tile(
                        Server.getInstance().getCore(),
                        abbreviation,
                        name,
                        points,
                        country
                );

                registerTile(tile);

                if (Server.getInstance().isDebug()) {
                    System.out.println("registered tile: " + tile.getName()
                            + " (" + tile.getAbbreviation() + ") points: "
                            + tile.getPoints()
                            + " controller: "
                            + (tile.getController() == null ? "none" : tile.getController().getAbbreviation())
                    );
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }

        //Server.getInstance().getSendTool().broadcast(
        //        Server.getInstance().getServerSocket().getClients(),
        //        TilesSyncPacket.fromTiles(tileSet)
        //);
    }


    public Tile registerTile(Tile tile) {
        cache.remove(tile);
        tileSet.remove(tile);
        tileSet.add(tile);

        if(Server.getInstance().getConfig().isCacheOnRegister()) cache(tile, tile.getPoints());

        return tile;
    }

    private void cache(Tile tile, List<Point> points) {
        cache.remove(tile);

        cacheExecutor.submit(() -> {
            if (Server.getInstance().isDebug()) System.out.println(tile.getAbbreviation() + " caching started!");

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

            if (Server.getInstance().isDebug()) System.out.println(tile.getAbbreviation() + " caching finished!");
        });
    }

    public Set<Tile> findNeighbors(Tile tile) {
        Set<Point> pixels = cache.get(tile);
        if (pixels == null) {
            cache(tile, tile.getPoints());
            return Set.of();
        }

        BufferedImage map = Server.getInstance().getScenarioSceneLoader().getMap();
        int black = Color.BLACK.getRGB();

        int maxBorderWidth = 1; //later fetch from some kind of config

        Set<Tile> neighbors = new HashSet<>();
        int width = map.getWidth();
        int height = map.getHeight();

        //int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {-1, -1}, {-1, 1}, {1, -1}};

        for (Point p : pixels) {
            for (int[] d : dirs) {

                for (int distance = 1; distance <= maxBorderWidth + 1; distance++) {
                    int x = p.x + d[0] * distance;
                    int y = p.y + d[1] * distance;

                    if (x < 0 || y < 0 || x >= width || y >= height) {
                        break;
                    }

                    int rgb = map.getRGB(x, y);

                    if (distance <= maxBorderWidth) {
                        if (rgb != black) {
                            break;
                        }
                    } else {
                        Tile other = getTileAt(x, y);
                        if (other != null && other != tile) {
                            neighbors.add(other);
                        }
                    }
                }
            }
        }

        return neighbors;
    }


    public List<String> getTiles() {
        return tileSet.stream().map(Tile::toString).collect(Collectors.toList());
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

    public Set<Tile> getTileSet() {
        return Collections.unmodifiableSet(tileSet);
    }

    public List<Tile> getTileList() {
        return Collections.unmodifiableList(new ArrayList<>(tileSet));
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

    public void clearTiles() {
        tileSet.clear();
    }
}