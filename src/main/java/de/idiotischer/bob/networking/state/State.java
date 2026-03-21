package de.idiotischer.bob.networking.state;

import de.idiotischer.bob.networking.country.Country;


//TODO: Point[] speichern können falls ein state so weirde formen haben bei denen der nicht ganz zusammenhängt
public class State {

    private final String name;
    private final String abbreviation;
    private Country controller;
    private final int x;
    private final int y;

    public State(String abbreviation, String name, int x, int y, Country controller) {
        this.abbreviation = abbreviation;
        this.name = name;
        this.controller = controller;
        this.x = x;
        this.y = y;
    }

    public Country getController() {
        return controller;
    }

    public void setController(Country controller) {
        this.controller = controller;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "State{" +
                "name='" + name + '\'' +
                ", abbreviation='" + abbreviation + '\'' +
                ", controller=" + controller +
                ", x=" + x +
                ", y=" + y +
                '}';
    }
}
