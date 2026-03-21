package de.idiotischer.bob.networking.country;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import de.idiotischer.bob.networking.BOB;
import de.idiotischer.bob.networking.state.State;

import java.awt.*;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class Countries {

    private final Set<Country> countrySet = new HashSet<>();

    public Countries() {
        reload();
    }

    private void reload() {
        countrySet.clear();

        Path path = BOB.getInstance().getScenarioSceneLoader().getScenariopath();

        try (JsonReader reader = new JsonReader(new FileReader(path.resolve("countries.json").toFile()))) {
            JsonElement root = BOB.getInstance().getScenarioSceneLoader().getGson().fromJson(reader, JsonElement.class);

            root.getAsJsonObject().entrySet().forEach(entry -> {
                String countryAbbreviation = entry.getKey();

                JsonObject countryElement = entry.getValue().getAsJsonObject();

                String name = countryElement.get("name").getAsString();

                String[] colorStrings = countryElement.get("color").getAsString().split(";");

                Color color = new Color(Integer.parseInt(colorStrings[0]), Integer.parseInt(colorStrings[1]), Integer.parseInt(colorStrings[2]));

                Country country = new Country(countryAbbreviation.toUpperCase(), name, color);

                registerCountry(country);

                IO.println("registered country: " + countryAbbreviation + " with name: " + name + " and color: " + color);
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Country registerCountry(Country country) {
        countrySet.remove(country);

        countrySet.add(country);

        return country;
    }

    public Country colorToCountry(int red, int green, int blue) {
        return Country.fromColor(new Color(red, green, blue));
    }

    public List<State> getControlled(Country country) {
        if(BOB.getInstance().getStateManager() == null) return List.of();
        return BOB.getInstance().getStateManager().getStateSet().stream().filter(s -> s.getController() == country).toList();
    }

    public Set<Country> getCountrySet() {
        return countrySet;
    }

    public void splitCountry(Country country) {
        //halt um ddr, brd zu machen
    }

    public Country getRandom() {
        Country[] country = countrySet.toArray(new Country[0]);

        int n = ThreadLocalRandom.current().nextInt(countrySet.size());

        return country[n];
    }
}
