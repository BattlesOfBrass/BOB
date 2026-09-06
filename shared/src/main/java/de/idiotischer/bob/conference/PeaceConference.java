package de.idiotischer.bob.conference;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.idiotischer.bob.SharedCore;
import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.country.CountryResolver;
import de.idiotischer.bob.networking.ChannelResolver;
import de.idiotischer.bob.networking.packet.impl.pp.ReplyPacket;
import de.idiotischer.bob.networking.packet.impl.pp.Type;
import de.idiotischer.bob.tile.Tile;
import de.idiotischer.bob.tile.TileResolver;
import de.idiotischer.bob.war.WarStatus;
import it.unimi.dsi.fastutil.Pair;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class PeaceConference {

    private final CountryResolver resolver;
    private final ChannelResolver r;
    private final Map<Country, Integer> baseVP;
    private final Map<Country, Integer> currentVP;

    private final List<Country> defeated;
    private final List<Country> winners;

    private final Map<Tile, List<TakeTileStatus>> tileClaims = new HashMap<>();
    private final Map<Tile, Country> finalizedAnnexations = new HashMap<>();
    private final SharedCore core;
    private int turnCount;

    private List<Country> ended = new ArrayList<>();
    private List<Country> finished = new ArrayList<>();

    private final UUID uuid;

    private Runnable endHook;

    public PeaceConference(SharedCore core, UUID uuid, CountryResolver resolver, Map<Country, Integer> baseVP, Map<Country, Integer> currentVP, Set<Country> defeated, Set<Country> winners) {
        this(core, uuid,null, resolver, baseVP, currentVP, defeated, winners);
    }

    public PeaceConference(SharedCore core, UUID uuid, ChannelResolver r, CountryResolver resolver, Map<Country, Integer> baseVP, Map<Country, Integer> currentVP, Set<Country> defeated, Set<Country> winners) {
        this.r = r;
        this.resolver = resolver;
        this.baseVP = baseVP;
        this.currentVP = currentVP;
        this.defeated = new ArrayList<>(defeated);
        this.winners = new ArrayList<>(winners);
        this.uuid = uuid;
        this.core = core;
    }

    public void startConference() {
        ended.clear();
        finished.clear();
        turnCount = 0;
        tileClaims.clear();
        finalizedAnnexations.clear();

        sortVP();

        core.getTool().broadcast(r.channels(), new ReplyPacket(Type.CONFERENCE_STARTED, this.serialize()));
    }


    public boolean nextTurn() {
        Set<String> finishedAbbreviations = finished.stream().map(Country::getAbbreviation).collect(Collectors.toSet());
        boolean allFinished = winners.stream().map(Country::getAbbreviation).allMatch(finishedAbbreviations::contains);

        if (!allFinished) return false;

        if (winners.isEmpty()) {
            endConference();
            return true;
        }

        turnCount++;
        finished.clear();
        sortVP();

        core.getTool().broadcast(r.channels(), new ReplyPacket(Type.ROUND_ENDED, this.serializeUpdate()));

        return true;
    }

    public boolean proposeTileTake(Tile tile, Country country, TakeTileType type) {
        //System.out.println("proposed tile annex");
        if (finalizedAnnexations.containsKey(tile)) return false;
        //System.out.println("proposed tile got through check 1");

        List<TakeTileStatus> claims = tileClaims.getOrDefault(tile, Collections.emptyList());

        if (!claims.isEmpty() && claims.getLast().country().equals(country)) return false;
        //System.out.println("proposed tile got through check2");

        int cost = calculateTileCost(tile);
        int available = currentVP.getOrDefault(country, 0);

        //if (available < cost) return false; //TODO: it doesnt get through this check, why is the vp not working?
        //System.out.println("proposed tile got through check 3");

        currentVP.put(country, available - cost);
        dispute(tile, new TakeTileStatus(country, cost, type));

        return true;
    }

    public boolean addFinished(Country c) {
        if(hasDispute(c)) return false;

        finished.add(c);

        if(new HashSet<>(finished.stream().map(Country::getPlayer).toList()).containsAll(winners.stream().map(Country::getPlayer).toList())) nextTurn();
        else core.getTool().broadcast(r.channels(), new ReplyPacket(Type.ROUND_UPDATE, this.serializeUpdate()));

        return true;
    }

    public boolean addEnded(Country c) {
        if(hasDispute(c)) return false;

        ended.add(c);

        if(new HashSet<>(ended.stream().map(Country::getPlayer).toList()).containsAll(winners.stream().map(Country::getPlayer).toList())) endConference();
        else core.getTool().broadcast(r.channels(), new ReplyPacket(Type.ROUND_UPDATE, this.serializeUpdate()));

        return true;
    }

    public boolean hasDispute(Country country) {
        return tileClaims.values().stream().anyMatch(claims -> claims.size() > 1 && claims.stream().anyMatch(claim -> claim.country().equals(country)));
    }

    public void dispute(Tile tile, TakeTileStatus status) {
        tileClaims.computeIfAbsent(tile, k -> new ArrayList<>()).add(status);
    }

    public int calculateTileCost(Tile tile) {
        int baseCost = Math.max(1, tile.getVictoryPoints());
        List<TakeTileStatus> claims = tileClaims.getOrDefault(tile, Collections.emptyList());

        if (claims.isEmpty()) return baseCost;

        int contestCount = claims.size();
        return baseCost + (contestCount * 2);
    }

    public boolean isDisputed(Tile tile) {
        List<TakeTileStatus> claims = tileClaims.getOrDefault(tile, Collections.emptyList());
        return claims.size() > 1;
    }

    public Map<Pair<Country, Country>, List<Tile>> getDisputed() {
        Map<Pair<Country, Country>, List<Tile>> disputedMap = new HashMap<>();

        for (Map.Entry<Tile, List<TakeTileStatus>> entry : tileClaims.entrySet()) {
            Tile tile = entry.getKey();
            List<TakeTileStatus> claims = entry.getValue();

            if (claims.size() < 2) continue;

            List<Country> claimants = claims.stream().map(TakeTileStatus::country).distinct().toList();

            if (claimants.size() < 2) continue;

            for (int i = 0; i < claimants.size(); i++) {
                for (int j = i + 1; j < claimants.size(); j++) {
                    Country first = claimants.get(i);
                    Country second = claimants.get(j);

                    Pair<Country, Country> pair = Pair.of(first, second);

                    disputedMap.computeIfAbsent(pair, k -> new ArrayList<>()).add(tile);
                }
            }
        }

        return disputedMap;
    }


    public void endConference() {
        //there is also no disputed logic here so i gotta add that
        if (r.channels() != null) {
            for (Map.Entry<Tile, List<TakeTileStatus>> entry : tileClaims.entrySet()) {
                Tile tile = entry.getKey();
                List<TakeTileStatus> claims = entry.getValue();

                if (claims.size() != 1) continue;

                TakeTileStatus status = claims.getFirst();

                if (status.type() == TakeTileType.TAKE) {
                    tile.setOwnerForAll(r.channels(), status.country());
                    tile.setControllerForAll(r.channels(), status.country());

                    finalizedAnnexations.put(tile, status.country());
                }
            }
        }

        core.getTool().broadcast(r.channels(), new ReplyPacket(Type.END_CONFERENCE, uuid.toString()));

        reinstate();

        if (endHook != null) endHook.run();
    }

    public boolean isTaken(Tile tile) {
        return finalizedAnnexations.containsKey(tile);
    }


    public List<Tile> getTilesToReinstate() {
        return defeated.stream().flatMap(c -> {
            List<Tile> tiles = new ArrayList<>(resolver.getOwned(c));
            tiles.removeIf(t -> !/*ig the ! is right, gotta check though*/resolver.anyAlliedWith(winners, t.getOwner()) || isTaken(t));
            return tiles.stream();
        }).toList();
    }


    public List<Tile> getTakableTiles() {
        return defeated.stream().flatMap(c -> {
            List<Tile> tiles = new ArrayList<>(resolver.getOwned(c));
            tiles.removeIf(t -> resolver.anyAlliedWith(winners, t.getOwner()));
            return tiles.stream();
        }).toList();
    }

    public void reinstate() {
        if (r.channels() == null) return;

        getTilesToReinstate().forEach(t -> t.setControllerForAll(r.channels(), t.getOwner()));
    }

    public List<Tile> getTakableTiles(CountryResolver resolver) {
        return defeated.stream().flatMap(c -> resolver.getOwned(c).stream()).filter(tile -> !finalizedAnnexations.containsKey(tile)).toList();
    }

    private void sortVP() {
        winners.sort((c1, c2) -> Integer.compare(currentVP.getOrDefault(c2, 0), currentVP.getOrDefault(c1, 0)));
    }

    public static PeaceConference fromWar(SharedCore core, UUID uuid, ChannelResolver cr, CountryResolver resolver, @NonNull WarStatus status) {
        if (!status.isEnded()) return null;
        return new PeaceConference(core,uuid, cr, resolver, status.getBaseVP(), status.getCurrentVP(), status.hasAttackingWon() ? status.getDefenders() : status.getAttackers(), status.hasAttackingWon() ? status.getAttackers() : status.getDefenders());
    }

    @ApiStatus.Obsolete
    public static PeaceConference fromWar(SharedCore core, UUID uuid, CountryResolver resolver, @NonNull WarStatus status) {
        if (!status.isEnded()) return null;
        return new PeaceConference(core,uuid, resolver, status.getBaseVP(), status.getCurrentVP(), status.hasAttackingWon() ? status.getDefenders() : status.getAttackers(), status.hasAttackingWon() ? status.getAttackers() : status.getDefenders());
    }

    public List<Country> getWinners() {
        return winners;
    }

    public List<Country> getDefeated() {
        return defeated;
    }

    public UUID getUUID() {
        return uuid;
    }

    public record TakeTileStatus(Country country, int vp, TakeTileType type) {}

    private String serialize() {
        Map<String, Object> data = new LinkedHashMap<>();

        data.put("uuid", uuid.toString());
        data.put("turn", turnCount);

        data.put("winners", winners.stream().map(Country::getAbbreviation).toList());
        data.put("defeated", defeated.stream().map(Country::getAbbreviation).toList());
        data.put("ended", ended.stream().map(Country::getAbbreviation).toList());
        data.put("finished", finished.stream().map(Country::getAbbreviation).toList());

        Map<String, Integer> baseVPData = baseVP.entrySet().stream().collect(Collectors.toMap(e -> e.getKey().getAbbreviation(), Map.Entry::getValue, (a, b) -> b, LinkedHashMap::new));
        Map<String, Integer> currentVPData = currentVP.entrySet().stream().collect(Collectors.toMap(e -> e.getKey().getAbbreviation(), Map.Entry::getValue, (a, b) -> b, LinkedHashMap::new));

        data.put("baseVP", baseVPData);
        data.put("currentVP", currentVPData);

        Map<String, List<Map<String, Object>>> claims = new LinkedHashMap<>();

        for (Map.Entry<Tile, List<TakeTileStatus>> entry : tileClaims.entrySet()) {
            Tile tile = entry.getKey();

            List<Map<String, Object>> tileClaimsData = entry.getValue().stream().map(claim -> {Map<String, Object> claimData = new LinkedHashMap<>();claimData.put("country", claim.country().getAbbreviation());claimData.put("vp", claim.vp());claimData.put("type", claim.type().name());return claimData;}).toList();

            claims.put(String.valueOf(tile.getAbbreviation()), tileClaimsData);
        }

        data.put("tileClaims", claims);

        Map<String, String> annexations = new LinkedHashMap<>();

        finalizedAnnexations.forEach((tile, country) -> annexations.put(String.valueOf(tile.getAbbreviation()), country.getAbbreviation()));

        data.put("finalizedAnnexations", annexations);

        return SharedCore.GSON.toJson(data);
    }

    public static PeaceConference deserialize(SharedCore core, CountryResolver resolver, TileResolver resolver2, String json) {
        JsonObject data = SharedCore.GSON.fromJson(json, JsonObject.class);

        UUID uuid = UUID.fromString(data.get("uuid").getAsString());
        int turn = data.get("turn").getAsInt();

        Set<Country> winners = new LinkedHashSet<>();
        for (JsonElement element : data.getAsJsonArray("winners")) {
            Country country = resolver.byAbbreviation(element.getAsString());
            if (country != null) winners.add(country);
        }

        Set<Country> defeated = new LinkedHashSet<>();
        for (JsonElement element : data.getAsJsonArray("defeated")) {
            Country country = resolver.byAbbreviation(element.getAsString());
            if (country != null) defeated.add(country);
        }

        Map<Country, Integer> baseVP = new LinkedHashMap<>();
        JsonObject baseVPData = data.getAsJsonObject("baseVP");

        for (Map.Entry<String, JsonElement> entry : baseVPData.entrySet()) {
            Country country = resolver.byAbbreviation(entry.getKey());

            if (country != null) baseVP.put(country, entry.getValue().getAsInt());
        }

        Map<Country, Integer> currentVP = new LinkedHashMap<>();
        JsonObject currentVPData = data.getAsJsonObject("currentVP");

        for (Map.Entry<String, JsonElement> entry : currentVPData.entrySet()) {
            Country country = resolver.byAbbreviation(entry.getKey());

            if (country != null) currentVP.put(country, entry.getValue().getAsInt());
        }

        PeaceConference conference = new PeaceConference(core, uuid, null, resolver, baseVP, currentVP, defeated, winners);

        conference.turnCount = turn;

        if (data.has("ended")) {
            for (JsonElement element : data.getAsJsonArray("ended")) {
                Country country = resolver.byAbbreviation(element.getAsString());

                if (country != null) conference.ended.add(country);
            }
        }


        if (data.has("finished")) {
            for (JsonElement element : data.getAsJsonArray("finished")) {
                Country country = resolver.byAbbreviation(element.getAsString());

                if (country != null) conference.finished.add(country);
            }
        }

        if (data.has("tileClaims")) {
            JsonObject claimsData = data.getAsJsonObject("tileClaims");

            for (Map.Entry<String, JsonElement> entry : claimsData.entrySet()) {
                Tile tile = resolver2.byAbbreviation(entry.getKey());

                if (tile == null) continue;

                List<TakeTileStatus> claims = new ArrayList<>();

                for (JsonElement claimElement : entry.getValue().getAsJsonArray()) {
                    JsonObject claim = claimElement.getAsJsonObject();

                    Country country = resolver.byAbbreviation(claim.get("country").getAsString());

                    if (country == null) continue;

                    int vp = claim.get("vp").getAsInt();

                    TakeTileType type = TakeTileType.valueOf(claim.get("type").getAsString());

                    claims.add(new TakeTileStatus(country, vp, type));
                }

                conference.tileClaims.put(tile, claims);
            }
        }

        if (data.has("finalizedAnnexations")) {
            JsonObject annexations = data.getAsJsonObject("finalizedAnnexations");

            for (Map.Entry<String, JsonElement> entry : annexations.entrySet()) {
                Tile tile = resolver2.byAbbreviation(entry.getKey());
                Country country = resolver.byAbbreviation(entry.getValue().getAsString());

                if (tile != null && country != null) conference.finalizedAnnexations.put(tile, country);
            }
        }

        return conference;
    }

    public String serializeUpdate() {
        Map<String, Object> data = new LinkedHashMap<>();

        data.put("uuid", uuid.toString()); //we only write this since i couldnt identify the uuid in the packet any other way

        data.put("turn", turnCount);

        data.put("ended", ended.stream().map(Country::getAbbreviation).toList());

        data.put("finished", finished.stream().map(Country::getAbbreviation).toList());

        Map<String, Integer> currentVPData = currentVP.entrySet().stream().collect(Collectors.toMap(e -> e.getKey().getAbbreviation(), Map.Entry::getValue, (a, b) -> b, LinkedHashMap::new));

        data.put("currentVP", currentVPData);

        Map<String, List<Map<String, Object>>> claims = new LinkedHashMap<>();

        for (Map.Entry<Tile, List<TakeTileStatus>> entry : tileClaims.entrySet()) {
            Tile tile = entry.getKey();

            List<Map<String, Object>> tileClaimsData = entry.getValue().stream().map(claim -> {
                Map<String, Object> claimData = new LinkedHashMap<>();

                claimData.put("country", claim.country().getAbbreviation());
                claimData.put("vp", claim.vp());
                claimData.put("type", claim.type().name());

                return claimData;
            }).toList();

            claims.put(String.valueOf(tile.getAbbreviation()), tileClaimsData);
        }

        data.put("tileClaims", claims);

        Map<String, String> annexations = new LinkedHashMap<>();

        finalizedAnnexations.forEach((tile, country) -> annexations.put(String.valueOf(tile.getAbbreviation()), country.getAbbreviation()));

        data.put("finalizedAnnexations", annexations);

        return SharedCore.GSON.toJson(data);
    }

    public static UUID getUUID(String json) {
        JsonObject data = SharedCore.GSON.fromJson(json, JsonObject.class);

        if (!data.has("uuid")) {
            return null;
        }

        return UUID.fromString(data.get("uuid").getAsString());
    }

    public void deserializeUpdate(TileResolver tileResolver, String json) {
        JsonObject data = SharedCore.GSON.fromJson(json, JsonObject.class);

        //if (data.has("uuid")) {
        //    uuid = UUID.fromString(data.get("uuid").getAsString());
        //}

        if (data.has("turn")) turnCount = data.get("turn").getAsInt();

        if (data.has("ended")) {
            ended.clear();

            for (JsonElement element : data.getAsJsonArray("ended")) {
                Country country = resolver.byAbbreviation(element.getAsString());

                if (country != null) ended.add(country);
            }
        }

        if (data.has("finished")) {
            finished.clear();

            for (JsonElement element : data.getAsJsonArray("finished")) {
                Country country = resolver.byAbbreviation(element.getAsString());

                if (country != null) finished.add(country);
            }
        }

        if (data.has("currentVP")) {
            currentVP.clear();

            JsonObject currentVPData = data.getAsJsonObject("currentVP");

            for (Map.Entry<String, JsonElement> entry : currentVPData.entrySet()) {
                Country country = resolver.byAbbreviation(entry.getKey());

                if (country != null) currentVP.put(country, entry.getValue().getAsInt());
            }
        }

        if (data.has("tileClaims")) {
            tileClaims.clear();

            JsonObject claimsData = data.getAsJsonObject("tileClaims");

            for (Map.Entry<String, JsonElement> entry : claimsData.entrySet()) {
                Tile tile = tileResolver.byAbbreviation(entry.getKey());

                if (tile == null) continue;

                List<TakeTileStatus> claims = new ArrayList<>();

                for (JsonElement claimElement : entry.getValue().getAsJsonArray()) {
                    JsonObject claim = claimElement.getAsJsonObject();

                    Country country = resolver.byAbbreviation(claim.get("country").getAsString());

                    if (country == null) continue;

                    int vp = claim.get("vp").getAsInt();

                    TakeTileType type = TakeTileType.valueOf(claim.get("type").getAsString());

                    claims.add(new TakeTileStatus(country, vp, type));
                }

                tileClaims.put(tile, claims);
            }
        }

        if (data.has("finalizedAnnexations")) {
            finalizedAnnexations.clear();

            JsonObject annexations = data.getAsJsonObject("finalizedAnnexations");

            for (Map.Entry<String, JsonElement> entry : annexations.entrySet()) {
                Tile tile = tileResolver.byAbbreviation(entry.getKey());
                Country country = resolver.byAbbreviation(entry.getValue().getAsString());

                if (tile != null && country != null) finalizedAnnexations.put(tile, country);
            }
        }
    }

    public void setEndHook(Runnable endHook) {
        this.endHook = endHook;
    }

    public record Demands(Country country, TakeTileType type, Set<Tile> tiles, UUID peaceId) {}

    //TODO: add methods to update parts for the client like for example the guy that has the current turn etc and add serializers
}