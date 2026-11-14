package de.idiotischer.bob.player.tasks;

import de.idiotischer.bob.country.Country;

public interface Task {

    boolean canExecute(String country);

    double getPriority(String country);

    default void onStart(String country) {}

    void tick(String country);

    boolean finished(String country);

    default void onStop(String country) {}
}