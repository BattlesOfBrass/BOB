package de.idiotischer.bob.troop;

import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.country.CountryResolver;
import de.idiotischer.bob.tile.Tile;
import de.idiotischer.bob.tile.TileResolver;
import de.idiotischer.bob.util.UUIDUtil;
import it.unimi.dsi.fastutil.Pair;

import java.util.*;

public class TroopStack extends Troop {

    private final List<Troop> troops;
    private Tile tile;

    public TroopStack(String name, Tile tile, Country owner,Country controller, int count, String template) {
        super(name,tile, owner, controller, template);

        this.tile = tile;
        this.troops = new ArrayList<>();

        for(int i = 0; i < count; i++) {
            Troop troop = new Troop(name,tile, owner, controller,template);

            troops.add(troop);
        }
    }

    @Deprecated
    public TroopStack(Tile tile, Country controller, List<Troop> troops) {
        super("",tile, controller,controller, "");

        this.tile = tile;
        this.troops = troops;

        troops.forEach(troop -> troop.setTile(tile));
    }

    public Tile getTile() {
        return tile;
    }

    public void setTile(Tile tile) {
        this.tile = tile;

        this.troops.forEach(troop -> troop.setTile(tile));
    }

    public List<Troop> getTroops() {
        return troops;
    }

    public String serialize(UUID troopId) {
        return "uuid=" + troopId.toString() +
                ";name=" + getName() +
                ";count=" + troops.size() +
                ";owner=" + getOwner().getAbbreviation() +
                ";controller=" + getController().getAbbreviation() +
                ";tile=" + tile.getAbbreviation() +
                ";template=" + template;
    }

    public static Pair<UUID,TroopStack> deserialize(String data, CountryResolver countryResolver, TileResolver tileResolver) {
        Map<String, String> values = new HashMap<>();

        for (String part : data.split(";")) {
            String[] keyValue = part.split("=", 2);
            if (keyValue.length == 2) {
                values.put(keyValue[0], keyValue[1]);
            }
        }

        UUID uuid = UUID.fromString(values.get("uuid"));
        String name = values.get("name");
        String count = values.get("count");

        Country owner = countryResolver.byAbbreviation(values.get("owner"));
        Country controller = countryResolver.byAbbreviation(values.get("controller"));
        Tile tile = tileResolver.byAbbreviation(values.get("tile"));

        String template = values.get("template");

        return Pair.of(uuid,new TroopStack(
                name,
                tile,
                owner,
                controller,
                Integer.parseInt(count),
                template
        ));
    }
}
