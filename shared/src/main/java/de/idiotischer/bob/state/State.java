package de.idiotischer.bob.state;

import de.idiotischer.bob.SharedCore;
import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.country.CountryResolver;
import de.idiotischer.bob.networking.packet.impl.pp.ReplyPacket;
import de.idiotischer.bob.networking.packet.impl.pp.RequestPacket;
import de.idiotischer.bob.networking.packet.impl.pp.Type;
import de.idiotischer.bob.state.event.StateChangedEvent;
import it.unimi.dsi.fastutil.Pair;
import org.jetbrains.annotations.NotNull;


//TODO: Point[] speichern können falls ein state so weirde formen haben bei denen der nicht ganz zusammenhängt
import java.awt.*;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class State {

    private final String name;
    private final String abbreviation;
    private final SharedCore core;
    private Country controller;
    private final List<Point> points;

    public State(SharedCore core, String abbreviation, String name, List<Point> points, Country controller) {
        this.core = core;
        this.abbreviation = abbreviation;
        this.name = name;
        this.controller = controller;
        this.points = points;
    }

    public Country getController() {
        return controller;
    }

    public void setControllerForAll(Set<AsynchronousSocketChannel> channels, Country controller) {
        StateChangedEvent event = new StateChangedEvent(this.controller, controller, this);

        core.getListenerRegistry().call(event);

        if (event.isCancelled()) {
            return;
        }

        core.getTool().broadcast(channels, new ReplyPacket(Type.STATE_CHANGE, this.constructChange(controller)));

        this.controller = controller;
    }

    public void setControllerClient(AsynchronousSocketChannel channel, Country controller) {
        StateChangedEvent event = new StateChangedEvent(this.controller, controller, this);

        core.getListenerRegistry().call(event);

        if (event.isCancelled()) {
            return;
        }

        core.getTool().send(channel, new RequestPacket(Type.STATE_CHANGE, constructChange(controller)));
        //wenn das packet nachher ankommt dann das img recoloren (das fällt mit delay auch nd mehr auf wenn dann eben nicht mehr auf den click direkt der state recolored werden soll

        //this.controller = controller; das dann im packet
    }

    //TODO: these method names are dumb xD
    public void setController(Country controller) {
        this.controller = controller;
    }

    //später mils etc auch?
    public String constructChange(Country controller) {
        return this.getAbbreviation() + ";" + controller.getAbbreviation();
    }

    public static Pair<State,Country> deconstructChange(String s, CountryResolver cR, StateResolver sR) {
        String[] parts = s.split(";");

        State state = sR.byAbbreviation(parts[0]);

        Country country = cR.byAbbreviation(parts[1]);

        return Pair.of(state, country);
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

    public static @NotNull State fromString(SharedCore core, @NotNull CountryResolver resolver, @NotNull String string) {
        String[] parts = string.split(";");

        String abbreviation = parts[0];
        String name = parts[1];

        String[] pointParts = parts[2].split("\\|");
        List<Point> points = new ArrayList<>();

        for (String p : pointParts) {
            String[] coords = p.split(",");
            int x = Integer.parseInt(coords[0]);
            int y = Integer.parseInt(coords[1]);
            points.add(new Point(x, y));
        }

        Country controller = resolver.byAbbreviation(parts[3]);

        return new State(core, abbreviation, name, points, controller);
    }

    public static @NotNull State by(SharedCore core,
                                    @NotNull CountryResolver resolver,
                                    @NotNull String abbreviation,
                                    String name,
                                    List<Point> points,
                                    String controllerAbbreviation) {

        Country controller = resolver.byAbbreviation(controllerAbbreviation);
        return new State(core, abbreviation, name, points, controller);
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
                sb + ";" +
                (getController() != null ? getController().getAbbreviation() : "null");
    }

    @Override
    public String toString() {
        return "State{" +
                "name='" + name + '\'' +
                ", abbreviation='" + abbreviation + '\'' +
                ", controller=" + (controller != null ? controller.getAbbreviation() : "null") +
                ", points=" + points +
                '}';
    }
}