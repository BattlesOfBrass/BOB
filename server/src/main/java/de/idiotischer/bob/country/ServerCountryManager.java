package de.idiotischer.bob.country;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import de.idiotischer.bob.Server;
import de.idiotischer.bob.SharedCore;
import de.idiotischer.bob.networking.packet.impl.CountriesSyncPacket;
import de.idiotischer.bob.networking.packet.impl.StatesSyncPacket;
import de.idiotischer.bob.state.State;

import java.awt.*;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class ServerCountryManager implements CountryResolver {
    @Override
    public Country byAbbreviation(String abbreviation) {
        return getCountry(abbreviation);
    }

    private final Set<Country> countrySet = new HashSet<>();

    public ServerCountryManager() {
    }

    public void reload() {
        countrySet.clear();

        try (JsonReader reader = new JsonReader(Files.newBufferedReader(Server.getInstance().getScenarioSceneLoader().getCurrentScenario().getCountryConfig()))) {
            JsonElement root = SharedCore.GSON.fromJson(reader, JsonElement.class);

            root.getAsJsonObject().entrySet().forEach(entry -> {
                String countryAbbreviation = entry.getKey();

                JsonObject countryElement = entry.getValue().getAsJsonObject();

                String name = countryElement.get("name").getAsString();

                boolean majorAtStart = false;
                boolean selectScreen = false;

                if(countryElement.has("majorAtStart") && !countryElement.get("majorAtStart").isJsonNull()) {
                    majorAtStart = countryElement.get("majorAtStart").getAsBoolean();
                }

                if(countryElement.has("selectScreen") && !countryElement.get("selectScreen").isJsonNull()) {
                    selectScreen = countryElement.get("selectScreen").getAsBoolean();
                }

                String[] colorStrings = countryElement.get("color").getAsString().split("[;,]");

                Color color = new Color(Integer.parseInt(colorStrings[0]), Integer.parseInt(colorStrings[1]), Integer.parseInt(colorStrings[2]));

                Country country = new Country(countryAbbreviation.toUpperCase(), name, color, majorAtStart, selectScreen);

                registerCountry(country);

                if(Server.getInstance().isDebug()) System.out.println("registered country: " + countryAbbreviation + " with name: " + name + " and color: " + color);
            });

        } catch (Exception e) {
            e.printStackTrace();
        }

        //TODO: make this send sync packets without crashing the client (liek statemanager)
        //Server.getInstance().getSendTool().broadcast(Server.getInstance().getServerSocket().getClients(), CountriesSyncPacket.fromCountries(countrySet));
    }

    public Set<Country> getPuppets() {
        return this.getCountries().stream().filter(c -> c.getPuppetState() != PuppetState.NONE).collect(Collectors.toSet());
    }

    public Country fromNameExact(String name) {
        return this.getCountries().stream().filter(country -> country.countryName().equals(name)).findFirst().orElse(null);
    }

    public Country fromAbbreviation(String abbreviation) {
        return this.getCountries().stream().filter(country -> country.getAbbreviation().equals(abbreviation.toUpperCase())).findFirst().orElse(null);
    }

    @Deprecated(forRemoval = true)
    public Country fromColor(Color color) {
        return this.getCountries().stream().filter(country -> country.countryColor().equals(color)).findFirst().orElse(null);
    }

    @Deprecated(forRemoval = true)
    public Country fromPixel(int x, int y) {
        Color color = null;//color getten
        return this.getCountries().stream().filter(country -> country.countryColor().equals(color)).findFirst().orElse(null);
    }

    public Country registerCountry(Country country) {
        countrySet.remove(country);

        countrySet.add(country);

        return country;
    }

    @Deprecated(forRemoval = true)
    public Country colorToCountry(int red, int green, int blue) {
        return fromColor(new Color(red, green, blue));
    }

    @Deprecated(forRemoval = true)
    public Country colorToCountry(int rgb) {
        return fromColor(new Color(rgb));
    }

    public Country getCountry(String abbreviation) {
        return countrySet.stream().filter(c -> c.getAbbreviation().equals(abbreviation)).findFirst().orElse(null);
    }

    public List<State> getControlled(Country country) {
        if(Server.getInstance().getStateManager() == null) return List.of();
        return Server.getInstance().getStateManager().getStateSet().stream().filter(s -> s.getController() == country).toList();
    }

    public List<Country> getCountries() {
        return countrySet
                .stream()
                .sorted(Comparator.comparing(Country::getAbbreviation))
                .toList();
    }

    //public List<Country> getMajors() {
    //    return getCountrySet().stream().filter(Country::isMajor).toList();
    //}

    //public List<Country> getOnSelectScreen() {
    //    return getCountrySet().stream().filter(Country::isSelectScreen).toList();
    //}

    public List<Country> getMajors() {
        return getCountries().stream()
                .filter(Country::isMajor)
                .sorted(Comparator.comparing(Country::getAbbreviation))
                .toList();
    }

    public List<Country> getOnSelectScreen() {
        return getCountries().stream()
                .filter(Country::isSelectScreen)
                .sorted(Comparator.comparing(Country::getAbbreviation))
                .toList();
    }

    public List<Country> getMinors() {
        return getCountries().stream()
                .filter(c -> !c.isMajor())
                .sorted(Comparator.comparing(Country::getAbbreviation))
                .toList();
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
