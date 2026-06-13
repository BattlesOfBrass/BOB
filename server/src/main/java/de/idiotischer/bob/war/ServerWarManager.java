package de.idiotischer.bob.war;

import de.idiotischer.bob.country.Country;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

//will probably remain only server side but i'll call it this way anyways
public class ServerWarManager {
    private final ExecutorService warExecutorService = Executors.newSingleThreadExecutor();

    public void reload() {}

    public boolean fightsTogetherWith(Country one, Country two) {
        return true;
    }

    public boolean isAtWar(Country aggressor, Country defender) {
        return true;
    }
}
