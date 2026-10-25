package de.idiotischer.bob.tile;

import de.idiotischer.bob.SharedCore;
import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.country.CountryResolver;
import de.idiotischer.bob.networking.ChannelResolver;
import de.idiotischer.bob.networking.communication.SendTool;
import de.idiotischer.bob.networking.packet.impl.pp.ReplyPacket;
import de.idiotischer.bob.networking.packet.impl.pp.Type;
import de.idiotischer.bob.tile.event.TileChangedEvent;
import it.unimi.dsi.fastutil.Pair;
import org.jetbrains.annotations.NotNull;


//TODO: Point[] speichern können falls ein tile so weirde formen haben bei denen der nicht ganz zusammenhängt
import java.awt.*;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class Tile {

    private final String name;
    private final String abbreviation;
    private final SharedCore core;
    private final int victoryPoints;
    private final Set<Country> claims;
    private Country controller;
    private Country owner;
    private final List<Point> points;
    private final String cityName;
    private final boolean city;

    private Set<TileConnection> tileConnections;

    public Tile(SharedCore core,Set<TileConnection> tileConnections, Set<Country> claims, int victoryPoints, String cityName, boolean city, String abbreviation, String name, List<Point> points, Country controller, Country owner) {
        this.core = core;
        this.tileConnections = tileConnections;
        this.claims = claims;
        this.abbreviation = abbreviation;
        this.name = name;
        this.controller = controller;
        this.owner = owner;
        this.points = points;
        this.cityName = cityName;
        this.city = city;
        this.victoryPoints = victoryPoints;
    }

    public Country getOwner() {
        return owner;
    }

    public void setOwner(Country owner) {
        this.owner = owner;
    }

    public void setOwnerForAll(Set<AsynchronousSocketChannel> channels, Country owner) {
        TileChangedEvent event = new TileChangedEvent(this.owner, owner, this, TileChangedEvent.Type.OWNER);

        core.getListenerRegistry().call(event);

        if (event.isCancelled()) return;

        core.getTool().broadcast(channels, new ReplyPacket(Type.TILE_CHANGE, this.constructOwnerChange(owner)));

        this.owner = owner;
    }

    public void setController(Country controller) {
        this.controller = controller;
    }

    public Country getController() {
        return controller == null ? owner : controller;
    }

    public void setControllerForAll(Set<AsynchronousSocketChannel> channels, Country controller) {
        TileChangedEvent event = new TileChangedEvent(this.controller, controller, this, TileChangedEvent.Type.CONTROLLER);

        core.getListenerRegistry().call(event);

        if (event.isCancelled()) return;

        core.getTool().broadcast(channels, new ReplyPacket(Type.TILE_CHANGE, this.constructControllerChange(controller)));

        this.controller = controller;
    }

    //später mils etc auch?

    public String constructControllerChange(Country controller) {
        return "controller=" + this.getAbbreviation() + ";" + controller.getAbbreviation();
    }

    public static TileChangedEvent.Type getChangeType(String s) {
        if (s.startsWith("controller=")) {
            return TileChangedEvent.Type.CONTROLLER;
        } else if (s.startsWith("owner=")) {
            return TileChangedEvent.Type.OWNER;
        }

        return null;
    }

    public static Pair<Tile, Country> deconstructChange(String s, CountryResolver cR, TileResolver sR) {
        String[] parts = s.split("=", 2);
        String[] values = parts[1].split(";");

        Tile tile = sR.byAbbreviation(values[0]);
        Country country = cR.byAbbreviation(values[1]);

        return Pair.of(tile, country);
    }

    public String constructOwnerChange(Country owner) {
        return "owner=" + this.getAbbreviation() + ";" + owner.getAbbreviation();
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public List<Point> getPoints() {
        return points;
    }

    public String getName() {
        return name;
    }

    //public static @NotNull Tile fromString(SharedCore core, @NotNull CountryResolver resolver, @NotNull String string) {
    //    String[] parts = string.split(";");

    //    String abbreviation = parts[0];
    //    String name = parts[1];

    //    String[] pointParts = parts[2].split("\\|");
    //    List<Point> points = new ArrayList<>();

    //    for (String p : pointParts) {
    //        String[] coords = p.split(",");
    //        int x = Integer.parseInt(coords[0]);
    //        int y = Integer.parseInt(coords[1]);
    //        points.add(new Point(x, y));
    //    }

    //    Country controller = resolver.byAbbreviation(parts[3]);

    //    return new Tile(core, cityName, hasCity, abbreviation, name, points, controller);
    //}

    public boolean hasCity() {
        return city;
    }

    public int getVictoryPoints() {
        return victoryPoints;
    }

    public String getCityName() {
        return cityName;
    }

    public Set<TileConnection> getTileConnections() {
        return tileConnections;
    }

    public static @NotNull Tile by(SharedCore core, Set<TileConnection> tileConnections, Set<Country> claims, @NotNull CountryResolver resolver, @NotNull String abbreviation, int victoryPoints, String name, String cityName, boolean hasCity, List<Point> points, String controllerAbbreviation, String ownerAbbreviation) {

        Country controller = "null".equals(controllerAbbreviation) ? null : resolver.byAbbreviation(controllerAbbreviation);
        Country owner = "null".equals(ownerAbbreviation) ? null : resolver.byAbbreviation(ownerAbbreviation);

        return new Tile(core, tileConnections, claims, victoryPoints, cityName, hasCity, abbreviation, name, points, controller,owner);
    }

    public String toDataString() {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < points.size(); i++) {
            Point p = points.get(i);
            sb.append(p.x).append(",").append(p.y);

            if (i < points.size() - 1) sb.append("|");
        }

        List<Country> claims = new ArrayList<>(this.claims);
        StringBuilder claimsSb = new StringBuilder();

        for (int i = 0; i < claims.size(); i++) {
            claimsSb.append(claims.get(i).getAbbreviation());

            if (i < claims.size() - 1) claimsSb.append(",");
        }


        String connections = TileConnection.serialize(tileConnections);

        return getAbbreviation() + ";" + getName() + ";" + hasCity() + ";" + getCityName() + ";" + sb + ";" + (getController() != null ? getController().getAbbreviation() : "null") + ";" + (getOwner() != null ? getOwner().getAbbreviation() : "null") + ";" + victoryPoints + ";" + claimsSb + ";" + connections;
    }

    public void addClaim(Country c) {
        claims.add(c);
    }

    public void removeClaim(Country c) {
        claims.remove(c);
    }

    public Set<Country> getClaims() {
        return claims;
    }

    @Override
    public String toString() {
        return "Tile{" +
                "name='" + name + '\'' +
                ", abbreviation='" + abbreviation + '\'' +
                ", controller=" + (controller != null ? controller.getAbbreviation() : "null") +
                ", owner=" + (owner != null ? owner.getAbbreviation() : "null") +
                ", points=" + points +
                ", hasCity=" + city +
                ", citName=" + cityName +
                ", victoryPoints=" + victoryPoints +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Tile other)) return false;

        return Objects.equals(abbreviation, other.abbreviation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(abbreviation);
    }

    public Set<UUID> getConnectionIds() {
        return tileConnections.stream().map(TileConnection::getUuid).collect(Collectors.toSet());
    }

    public TileConnection getConnection(UUID uuid) {
        return tileConnections.stream().filter(c -> c.getUuid().equals(uuid)).findFirst().orElse(null);
    }

    public static class TileConnection {
        private boolean broken;
        private ConnectionType type;

        private final UUID uuid;

        private String origin;
        private String connectedTo;

        public TileConnection(String origin, String connectedTo, UUID uuid, ConnectionType type, boolean broken) {
            this.origin = origin;
            this.connectedTo = connectedTo;
            this.uuid = uuid;
            this.type = type;
            this.broken = broken;
        }

        public UUID getUuid() {
            return uuid;
        }

        public Tile getOrigin(TileResolver r) {
            return r.byAbbreviation(origin);
        }

        public Tile getConnectedTo(TileResolver r) {
            return r.byAbbreviation(connectedTo);
        }

        public void setBroken(SendTool t, ChannelResolver r, boolean broken) {
            this.broken = broken;

            t.broadcast(r.channels(), new ReplyPacket(Type.TILE_CONNECTION_CHANGE,serializeBrokenChange()));
        }

        public String getConnectedTo() {
            return connectedTo;
        }

        public String getOrigin() {
            return origin;
        }

        public void setBrokenSimple(boolean broken) {
            this.broken = broken;
        }

        public boolean isBroken() {
            return broken;
        }

        public void setTypeSimple(ConnectionType type) {
            this.type = type;
        }

        public void setType(SendTool t, ChannelResolver r, ConnectionType type) {
            this.type = type;

            t.broadcast(r.channels(), new ReplyPacket(Type.TILE_CONNECTION_CHANGE, serializeTypeChange()));
        }

        public ConnectionType getType() {
            return type;
        }

        public String serializeTypeChange() {
            return "type=" + type + ";" + uuid.toString();
        }

        public String serializeBrokenChange() {
            return "broken=" + broken + ";" + uuid.toString();
        }

        public static Pair<ConnectionType, UUID> deserializeType(String s) {
            if(!s.startsWith("broken=")) return Pair.of(null,null);

            String[] strings = s.split("=");

            String[] strings1 = strings[1].split(";");

            ConnectionType b = ConnectionType.valueOf(strings[0]);
            UUID uuid = UUID.fromString(strings1[1]);

            return Pair.of(b,uuid);
        }

        public static Pair<Boolean, UUID> deserializeBroken(String s) {
            if(!s.startsWith("broken=")) return Pair.of(null,null);

            String[] strings = s.split("=");

            String[] strings1 = strings[1].split(";");

            boolean b = Boolean.parseBoolean(strings[0]);
            UUID uuid = UUID.fromString(strings1[1]);

            return Pair.of(b,uuid);
        }

        public String toDataString() {
            return origin + "," + connectedTo + "," + uuid + "," + type.name() + "," + broken;
        }

        public static TileConnection fromDataString(String data) {
            String[] parts = data.split(",", -1);

            if (parts.length != 5) throw new IllegalArgumentException("Invalid TileConnection data: " + data);

            String origin = parts[0];
            String connectedTo = parts[1];
            UUID uuid = UUID.fromString(parts[2]);
            ConnectionType type = ConnectionType.valueOf(parts[3]);
            boolean broken = Boolean.parseBoolean(parts[4]);

            return new TileConnection(origin, connectedTo, uuid, type, broken);
        }

        public static String serialize(Set<TileConnection> connections) {
            return connections.stream().map(TileConnection::toDataString).collect(Collectors.joining("|"));
        }

        public static Set<TileConnection> deserialize(String data) {
            Set<TileConnection> connections = new HashSet<>();

            if (data == null || data.isEmpty()) return connections;

            for (String connectionData : data.split("\\|", -1)) if (!connectionData.isEmpty()) connections.add(TileConnection.fromDataString(connectionData));

            return connections;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof TileConnection other)) return false;

            return Objects.equals(uuid, other.getUuid());
        }

        @Override
        public int hashCode() {
            return Objects.hash(uuid);
        }

        public enum ConnectionType {
            TUNNEL(true),
            BRIDGE(true);

            private final boolean canBeBlown;

            ConnectionType(boolean canBeBlown) {
                this.canBeBlown = canBeBlown;
            }

            public boolean canBeBlown() {
                return canBeBlown;
            }
        }
    }
}