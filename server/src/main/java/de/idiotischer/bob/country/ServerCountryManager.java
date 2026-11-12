package de.idiotischer.bob.country;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import de.idiotischer.bob.Server;
import de.idiotischer.bob.SharedCore;
import de.idiotischer.bob.ideology.Ideology;
import de.idiotischer.bob.modifiers.Modifier;
import de.idiotischer.bob.networking.packet.impl.CountrySyncPacket;
import de.idiotischer.bob.tile.Tile;

import java.awt.*;
import java.nio.file.Files;
import java.util.*;
import java.util.List;
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
                Country.Conscription conscription = Country.Conscription.LIMITED_CONSCRIPTION;

                Ideology rulingIdeology = null;
                Set<Ideology> availableIdeologies = new HashSet<>();

                Country overlord = null;

                Set<Modifier> mods = new HashSet<>();

                if(countryElement.has("modifiers") && !countryElement.get("modifiers").isJsonNull() && countryElement.get("modifiers").isJsonArray()) {
                    JsonArray array = countryElement.getAsJsonArray("modifiers");

                    array.forEach(el -> {
                        mods.add(Server.getInstance().getModifierManager().by(el.getAsString()));
                    });
                }

                if(countryElement.has("majorAtStart") && !countryElement.get("majorAtStart").isJsonNull())
                    majorAtStart = countryElement.get("majorAtStart").getAsBoolean();

                if(countryElement.has("ideology") && !countryElement.get("ideology").isJsonNull())
                    rulingIdeology = Server.getInstance().getIdeologyManager().byAbbreviation(countryElement.get("ideology").getAsString().toUpperCase());

                if(countryElement.has("overlord") && !countryElement.get("overlord").isJsonNull())
                    overlord = Server.getInstance().getCountryManager().byAbbreviation(countryElement.get("overlord").getAsString().toUpperCase());

                if(countryElement.has("availableIdeologies") && !countryElement.get("availableIdeologies").isJsonNull())
                    availableIdeologies = countryElement.get("availableIdeologies").getAsJsonArray().asList().stream().map(s -> Server.getInstance().getIdeologyManager().byAbbreviation(s.getAsString().toUpperCase())).collect(Collectors.toSet());

                if(countryElement.has("selectScreen") && !countryElement.get("selectScreen").isJsonNull())
                    selectScreen = countryElement.get("selectScreen").getAsBoolean();

                long manpower = 0;
                if(countryElement.has("manpower") && !countryElement.get("manpower").isJsonNull())
                    manpower = countryElement.get("manpower").getAsLong();

                if(countryElement.has("conscription") && !countryElement.get("conscription").isJsonNull()) {
                    try {
                        conscription = Country.Conscription.valueOf(countryElement.get("conscription").getAsString().toUpperCase());
                    } catch (EnumConstantNotPresentException e) {
                        conscription = Country.Conscription.LIMITED_CONSCRIPTION;
                    }
                }

                String[] colorStrings = countryElement.get("color").getAsString().split("[;,]");

                Color color = new Color(Integer.parseInt(colorStrings[0]), Integer.parseInt(colorStrings[1]), Integer.parseInt(colorStrings[2]));

                Country country = new Country(countryAbbreviation.toUpperCase(), name, color, majorAtStart, selectScreen,overlord == null ? "" : overlord.getAbbreviation(), mods);

                country.setManpower(manpower);
                country.setConscription(conscription);
                country.setStartIdeologySimple(rulingIdeology);
                country.setRulingIdeologySimple(rulingIdeology);
                availableIdeologies.forEach(country::addAvailableIdeologiesSimple);

                registerCountry(country);

                if(Server.getInstance().isDebug()) System.out.println("registered country: " + countryAbbreviation + " with name: " + name + " and color: " + color);
            });

        } catch (Exception e) {
            e.printStackTrace();
        }

        //TODO: make this send sync packets without crashing the client (liek tilemanager)
        //Server.getInstance().getSendTool().broadcast(Server.getInstance().getServerSocket().getClients(), CountriesSyncPacket.fromCountries(countrySet));
    }

    public Set<Country> getPuppets() {
        return this.getCountries().stream().filter(c -> c.getPuppetState() != PuppetState.NONE).collect(Collectors.toSet());
    }

    public double getTotalPopulation(Country country) {
        var controlled = new HashSet<>(Server.getInstance().getTileManager().getTilesControlledDirectly(country));
        var owned = new HashSet<>(Server.getInstance().getTileManager().getTilesOwnedDirectly(country));

        owned.removeAll(controlled);
        controlled.removeAll(owned);

        double controlledSumPenalty = controlled.stream().mapToDouble(Tile::getPopulation).sum();
        double ownedSum = owned.stream().mapToDouble(Tile::getPopulation).sum();

        return ownedSum + controlledSumPenalty;
    }


    public double getPopulation(Country country) {
        var controlled = new HashSet<>(Server.getInstance().getTileManager().getTilesControlledDirectly(country));
        var owned = new HashSet<>(Server.getInstance().getTileManager().getTilesOwnedDirectly(country));

        owned.removeAll(controlled);
        controlled.removeAll(owned);
        //var mods = country.getModifiers().stream().filter(mod -> mod.getType().equals(Modifier.Types.CONSCRIPTION_FOR_OCCUPATION.getMapping())).collect(Collectors.toSet());

        double controlledSumPenalty = controlled.stream().mapToDouble(Tile::getPopulation).sum() * 0.75; //retain about 75%, i dont really know but we could scale it with resistance later or with modifiers
        double ownedSum = owned.stream().mapToDouble(Tile::getPopulation).sum();

        return ownedSum + controlledSumPenalty;
    }

    public double removePopulationCheck(double pop, Country country) {
        Set<Tile> controlled = new HashSet<>(Server.getInstance().getTileManager().getTilesControlledDirectly(country));
        Set<Tile> owned = new HashSet<>(Server.getInstance().getTileManager().getTilesOwnedDirectly(country));

        owned.removeIf(t -> !controlled.contains(t));

        Set<Tile> tiles = new HashSet<>(owned);
        tiles.addAll(controlled);

        if (tiles.isEmpty() || pop <= 0) return 0;

        double totalPopulation = getTotalPopulation(country);

        double actualRemoval = Math.min(pop, totalPopulation);

        double perTile = actualRemoval / tiles.size();
        double remainder = actualRemoval % tiles.size();

        for (Tile tile : tiles) {
            double amount = perTile;

            if (remainder > 0) {
                amount++;
                remainder--;
            }

            tile.setPopulationServer(Server.getInstance().getSendTool(), Server.getInstance().getServerSocket(), tile.getPopulation() - amount);
        }

        return actualRemoval;
    }

    public long removeManpowerCheck(double manpower, Country country) {
        if (manpower <= 0) return 0;

        long actualRemoval = (long) Math.min(manpower, country.getManpower());

        country.setManpowerServer(Server.getInstance().getSendTool(), Server.getInstance().getServerSocket(), country.getManpower() - actualRemoval);

        return actualRemoval;
    }

    public void removePopulation(double pop, Country country) {
        Set<Tile> controlled = new HashSet<>(Server.getInstance().getTileManager().getTilesControlledDirectly(country));
        Set<Tile> owned = new HashSet<>(Server.getInstance().getTileManager().getTilesOwnedDirectly(country));

        owned.removeIf(t -> !controlled.contains(t));

        Set<Tile> tiles = new HashSet<>();
        tiles.addAll(owned);
        tiles.addAll(controlled);

        if (tiles.isEmpty() || pop <= 0) return;

        double perTile = pop / tiles.size();
        double remainder = pop % tiles.size();

        for (var tile : tiles) {
            double amount = perTile;

            if (remainder > 0) {
                amount++;
                remainder--;
            }

            tile.setPopulationServer(Server.getInstance().getSendTool(), Server.getInstance().getServerSocket(), tile.getPopulation() - amount);
        }
    }
    public void addPopPercent(float perc, Country country) {
        Set<Tile> controlled = new HashSet<>(Server.getInstance().getTileManager().getTilesControlledDirectly(country));
        Set<Tile> owned = new HashSet<>(Server.getInstance().getTileManager().getTilesOwnedDirectly(country));

        owned.removeIf(t -> !controlled.contains(t));

        Set<Tile> tiles = new HashSet<>();
        tiles.addAll(owned);
        tiles.addAll(controlled);

        if(tiles.isEmpty() || perc <= 0) return;

        double pop = getTotalPopulation(country) * perc;

        double perTile = pop / tiles.size();
        double remainder = pop % tiles.size();

        for (var tile : tiles) {
            double amount = perTile;

            if (remainder > 0) {
                amount++;
                remainder--;
            }

            tile.setPopulationServer(Server.getInstance().getSendTool(), Server.getInstance().getServerSocket(), tile.getPopulation() + amount);
        }
    }

    public void addPop(double pop, Country country) {
        Set<Tile> controlled = new HashSet<>(Server.getInstance().getTileManager().getTilesControlledDirectly(country));
        Set<Tile> owned = new HashSet<>(Server.getInstance().getTileManager().getTilesOwnedDirectly(country));

        owned.removeIf(t -> !controlled.contains(t));

        Set<Tile> tiles = new HashSet<>();
        tiles.addAll(owned);
        tiles.addAll(controlled);

        if(tiles.isEmpty() || pop <= 0) return ;

        double perTile = pop / tiles.size();
        double remainder = pop % tiles.size();

        for (var tile : tiles) {
            double amount = perTile;

            if (remainder > 0) {
                amount++;
                remainder--;
            }

            tile.setPopulationServer(Server.getInstance().getSendTool(), Server.getInstance().getServerSocket(), tile.getPopulation() + amount);
        }
    }

    public Country registerCountry(Country country) {
        countrySet.remove(country);

        countrySet.add(country);

        return country;
    }

    public Country getCountry(String abbreviation) {
        return countrySet.stream().filter(c -> c.getAbbreviation().equals(abbreviation)).findFirst().orElse(null);
    }

    public Set<Country> getCountrySet() {
        return countrySet;
    }

    public List<Tile> getOwned(Country country) {
        if(Server.getInstance().getTileManager() == null) return List.of();
        return Server.getInstance().getTileManager().getTileSet().stream().filter(s -> s.getOwner().getAbbreviation().equals(country.getAbbreviation())).toList();
    }
    public List<Tile> getOwned(String country) {
        if(Server.getInstance().getTileManager() == null) return List.of();
        return Server.getInstance().getTileManager().getTileSet().stream().filter(s -> s.getOwner().getAbbreviation().equals(country)).toList();
    }

    public List<Tile> getControlled(Country country) {
        if(Server.getInstance().getTileManager() == null) return List.of();
        return Server.getInstance().getTileManager().getTileSet().stream().filter(s -> s.getController().getAbbreviation().equals(country.getAbbreviation())).toList();
    }

    public List<Tile> getControlled(String country) {
        if(Server.getInstance().getTileManager() == null) return List.of();
        return Server.getInstance().getTileManager().getTileSet().stream().filter(s -> s.getController().getAbbreviation().equals(country)).toList();
    }

    public List<Country> getCountries() {
        return countrySet.stream().sorted(Comparator.comparing(Country::getAbbreviation)).toList();
    }

    public int getTotalVPs(Country country) {
        //getOwned(country).;
        return getOwned(country).stream().mapToInt(Tile::getVictoryPoints).sum();
    }

    //public List<Country> getMajors() {
    //    return getCountrySet().stream().filter(Country::isMajor).toList();
    //}

    //public List<Country> getOnSelectScreen() {
    //    return getCountrySet().stream().filter(Country::isSelectScreen).toList();
    //}

    public void splitCountry(Country country) {
        String newAbbr;
        int turn = 0;

        do {
            newAbbr = country.getAbbreviation() + "-split-" + turn; //TODO: if civ war Return to the original country tag after civil war is over
            turn++;
        } while(abbreviationExists(newAbbr));

        Country newC = country.uniqueCopy(newAbbr);

        countrySet.add(newC);
        CountrySyncPacket pack = new CountrySyncPacket(country);
        Server.getInstance().getSendTool().broadcast(Server.getInstance().getServerSocket().channels(), pack);
    }

    public List<Country> getMajors() {
        return getCountries().stream().filter(Country::isMajor).sorted(Comparator.comparing(Country::getAbbreviation)).toList();
    }

    public boolean isAllied(Country a, Country b) {
        return Server.getInstance().getFactionManager().inFactionWith(a,b);
    }

    public boolean abbreviationExists(String abbr) {
        return countrySet.stream().anyMatch(c -> c.getAbbreviation().equals(abbr));
    }

    @Override
    public boolean anyAlliedWith(List<Country> testers, Country country) {
        return testers.stream().anyMatch(c -> isAllied(c, country));
    }

    public List<Country> getOnSelectScreen() {
        return getCountries().stream().filter(Country::isSelectScreen).sorted(Comparator.comparing(Country::getAbbreviation)).toList();
    }

    public List<Country> getMinors() {
        return getCountries().stream().filter(c -> !c.isMajor()).sorted(Comparator.comparing(Country::getAbbreviation)).toList();
    }

    public Country getRandom() {
        Country[] country = countrySet.toArray(new Country[0]);

        int n = ThreadLocalRandom.current().nextInt(countrySet.size());

        return country[n];
    }

    public void defaultManpowerNow() {
        for(Country country : getCountries()) {
            List<Tile> tiles = getAllTiles(country);

            long sum = (long) (tiles.stream().mapToDouble(Tile::getPopulation).sum() * country.getRecruitablePop());

            country.setManpowerServer(Server.getInstance().getSendTool(), Server.getInstance().getServerSocket(), sum);
        }
    }
}
