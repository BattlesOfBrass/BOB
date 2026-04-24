package de.idiotischer.bob.country;

import de.idiotischer.bob.BOB;
import de.idiotischer.bob.networking.packet.impl.pp.RequestPacket;
import de.idiotischer.bob.networking.packet.impl.pp.Type;
import de.idiotischer.bob.state.State;

import java.awt.*;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class CountryManager implements CountryResolver{

    private final Set<Country> countrySet = new HashSet<>();
    private CompletableFuture<Void> awaitingFuture = new CompletableFuture<>();

    public CountryManager() {
        //reload();
    }

    public CompletableFuture<Void> reload() {
        awaitingFuture = new CompletableFuture<>();

        countrySet.clear();

        BOB.getInstance().getSendTool().send(BOB.getInstance().getClient().getChannel(), new RequestPacket(Type.COUNTRIES_SYNC, ""));

        return awaitingFuture;
    }

    public void finishReload() {
        BOB.getInstance().getStateManager().reload();

        if(awaitingFuture == null || awaitingFuture.isDone()) return;
        awaitingFuture.complete(null);
    }

    public Country registerCountry(Country country) {
        countrySet.remove(country);

        countrySet.add(country);

        return country;
    }

    public boolean has(String countryAbbreviation) {
        return getCountries().stream().map(Country::getAbbreviation).collect(Collectors.toSet()).contains(countryAbbreviation);
    }

    public boolean has(Country country) {
        return has(country.getAbbreviation());
    }

    public static Country fromNameExact(String name) {
        return BOB.getInstance().getCountryManager().getCountries().stream().filter(country -> country.countryName().equals(name)).findFirst().orElse(null);
    }

    public static Country fromAbbreviation(String abbreviation) {
        return BOB.getInstance().getCountryManager().getCountries().stream().filter(country -> country.getAbbreviation().equals(abbreviation.toUpperCase())).findFirst().orElse(null);
    }

    @Deprecated(forRemoval = true)
    public static Country fromColor(Color color) {
        return BOB.getInstance().getCountryManager().getCountries().stream().filter(country -> country.countryColor().equals(color)).findFirst().orElse(null);
    }

    @Deprecated(forRemoval = true)
    public static Country fromPixel(int x, int y) {
        Color color = null;//color getten
        return BOB.getInstance().getCountryManager().getCountries().stream().filter(country -> country.countryColor().equals(color)).findFirst().orElse(null);
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
        if(BOB.getInstance().getStateManager() == null) return List.of();
        return BOB.getInstance().getStateManager().getStateSet().stream().filter(s -> s.getController() == country).toList();
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

    @Override
    public Country byAbbreviation(String abbreviation) {
        return getCountry(abbreviation);
    }
}