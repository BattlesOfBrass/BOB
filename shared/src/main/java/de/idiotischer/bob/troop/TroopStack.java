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
    private int baseHp = 200;
    private int baseAttack = 25;
    private int baseDefense = 10;
    private int baseOrg = 100;
    private int hp = baseHp;
    private int attack = baseAttack;
    private int defense = baseDefense;
    private int org = baseOrg;
    private boolean alive = true;

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
                ";template=" + template +
                ";baseHp=" + baseHp +
                ";baseAttack=" + baseAttack +
                ";baseDefense=" + baseDefense +
                ";baseOrg=" + baseOrg +
                ";hp=" + hp +
                ";attack=" + attack +
                ";defense=" + defense +
                ";org=" + org +
                ";alive=" + alive;
    }

    public static Pair<UUID, TroopStack> deserialize(
            String data,
            CountryResolver countryResolver,
            TileResolver tileResolver
    ) {
        Map<String, String> values = new HashMap<>();

        for (String part : data.split(";")) {
            String[] keyValue = part.split("=", 2);
            if (keyValue.length == 2) {
                values.put(keyValue[0], keyValue[1]);
            }
        }

        UUID uuid = UUID.fromString(values.get("uuid"));
        String name = values.get("name");
        int count = Integer.parseInt(values.get("count"));

        Country owner = countryResolver.byAbbreviation(values.get("owner"));
        Country controller = countryResolver.byAbbreviation(values.get("controller"));
        Tile tile = tileResolver.byAbbreviation(values.get("tile"));

        String template = values.get("template");

        TroopStack stack = new TroopStack(
                name,
                tile,
                owner,
                controller,
                count,
                template
        );

        stack.baseHp = Integer.parseInt(values.getOrDefault("baseHp", "200"));
        stack.baseAttack = Integer.parseInt(values.getOrDefault("baseAttack", "25"));
        stack.baseDefense = Integer.parseInt(values.getOrDefault("baseDefense", "10"));
        stack.baseOrg = Integer.parseInt(values.getOrDefault("baseOrg", "100"));

        stack.hp = Integer.parseInt(values.getOrDefault("hp", String.valueOf(stack.baseHp)));
        stack.attack = Integer.parseInt(values.getOrDefault("attack", String.valueOf(stack.baseAttack)));
        stack.defense = Integer.parseInt(values.getOrDefault("defense", String.valueOf(stack.baseDefense)));
        stack.org = Integer.parseInt(values.getOrDefault("org", String.valueOf(stack.baseOrg)));

        stack.alive = Boolean.parseBoolean(values.getOrDefault("alive", "true"));

        return Pair.of(uuid, stack);
    }

    public int getHp() {
        return hp * troops.size();
    }

    public void setHp(int hp) {
        this.hp = hp;
    }

    public int getAttack() {
        return attack * troops.size();
    }

    public void setAttack(int attack) {
        this.attack = attack;
    }

    public int getDefense() {
        return defense * troops.size();
    }

    public void setDefense(int defense) {
        this.defense = defense;
    }

    public int getOrg() {
        return org * troops.size();
    }

    public void setOrg(int org) {
        this.org = Math.max(0, Math.min(baseOrg, org));
    }

    public boolean isBroken() {
        return org <= 0;
    }

    public void resetAttributes() {
        this.hp = this.baseHp;
        this.org = this.baseOrg;
        this.attack = this.baseAttack;
        this.defense = this.baseDefense;
    }

    public boolean isAlive() {
        return alive;
    }

    public void setAlive(boolean b) {
        this.alive = b;
    }
}
