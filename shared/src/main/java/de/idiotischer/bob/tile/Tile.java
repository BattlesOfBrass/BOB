package de.idiotischer.bob.tile;

import de.idiotischer.bob.SharedCore;
import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.country.CountryResolver;
import de.idiotischer.bob.networking.packet.impl.pp.ReplyPacket;
import de.idiotischer.bob.networking.packet.impl.pp.RequestPacket;
import de.idiotischer.bob.networking.packet.impl.pp.Type;
import de.idiotischer.bob.tile.event.TileChangedEvent;
import it.unimi.dsi.fastutil.Pair;
import org.jetbrains.annotations.NotNull;


//TODO: Point[] speichern können falls ein tile so weirde formen haben bei denen der nicht ganz zusammenhängt
import java.awt.*;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.List;
import java.util.Set;

public class Tile {

    private final String name;
    private final String abbreviation;
    private final SharedCore core;
    private final int victoryPoints;
    private Country controller;
    private Country owner;
    private final List<Point> points;
    private final String cityName;
    private final boolean city;

    public Tile(SharedCore core, int victoryPoints, String cityName, boolean city, String abbreviation, String name, List<Point> points, Country controller, Country owner) {
        this.core = core;
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
        System.out.println("set tile owner for client: " + abbreviation + " " + owner.getAbbreviation());
    }

    public void setOwnerForAll(Set<AsynchronousSocketChannel> channels, Country owner) {
        TileChangedEvent event = new TileChangedEvent(this.owner, owner, this, TileChangedEvent.Type.OWNER);

        core.getListenerRegistry().call(event);

        if (event.isCancelled()) return;

        core.getTool().broadcast(channels, new ReplyPacket(Type.TILE_CHANGE, this.constructOwnerChange(controller)));

        this.owner = owner;
        System.out.println("set tile owner for: " + abbreviation + " " + owner.getAbbreviation());
    }

    public void setController(Country controller) {
        this.controller = controller;
        System.out.println("set tile controller for client: " + abbreviation + " " + owner.getAbbreviation());
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
        System.out.println("set tile controller for: " + abbreviation + " " + owner.getAbbreviation());
    }

    public void setControllerClient(AsynchronousSocketChannel channel, Country controller) {
        TileChangedEvent event = new TileChangedEvent(this.controller, controller, this, TileChangedEvent.Type.CONTROLLER);

        core.getListenerRegistry().call(event);

        if (event.isCancelled()) return;

        core.getTool().send(channel, new RequestPacket(Type.TILE_CHANGE, constructControllerChange(controller)));
    }

    //TODO: these method names are dumb xD
    public void setControllerFinish(Country controller, boolean debug) {
        this.controller = controller;
        if(debug) System.out.println("got controller sync on: " + abbreviation);
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

    public static @NotNull Tile by(SharedCore core, @NotNull CountryResolver resolver, @NotNull String abbreviation, int victoryPoints, String name, String cityName, boolean hasCity, List<Point> points, String controllerAbbreviation, String ownerAbbreviation) {

        Country controller = "null".equals(controllerAbbreviation) ? null : resolver.byAbbreviation(controllerAbbreviation);
        Country owner = "null".equals(ownerAbbreviation) ? null : resolver.byAbbreviation(ownerAbbreviation);

        return new Tile(core, victoryPoints, cityName, hasCity, abbreviation, name, points, controller,owner);
    }

    public String toDataString() {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < points.size(); i++) {
            Point p = points.get(i);
            sb.append(p.x).append(",").append(p.y);

            if (i < points.size() - 1) {
                sb.append("|");
            }
        }

        return getAbbreviation() + ";" +
                getName() + ";" +
                hasCity() + ";" +
                getCityName() + ";" +
                sb + ";" +
                (getController() != null ? getController().getAbbreviation() : "null") + ";" +
                (getOwner() != null ? getOwner().getAbbreviation() : "null") + ";" +
                victoryPoints;
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

        return abbreviation.equals(other.abbreviation);
    }

    @Override
    public int hashCode() {
        return abbreviation.hashCode();
    }
}