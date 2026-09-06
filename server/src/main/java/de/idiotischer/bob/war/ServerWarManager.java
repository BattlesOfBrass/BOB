package de.idiotischer.bob.war;

import de.idiotischer.bob.Server;
import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.networking.packet.impl.pp.ReplyPacket;
import de.idiotischer.bob.networking.packet.impl.pp.Type;
import de.idiotischer.bob.tile.Tile;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class ServerWarManager {

    private final ExecutorService warExecutorService = Executors.newSingleThreadExecutor();

    private final Map<String, Set<WarStatus>> activeWars = new ConcurrentHashMap<>();

    public void reload() {
        activeWars.clear();
    }

    public Set<WarStatus> getWars(Country c) {
        return activeWars.getOrDefault(c.getAbbreviation(), Collections.emptySet());
    }

    public boolean isAtWar(Country a, Country b) {
        if(Objects.equals(a.getAbbreviation(), b.getAbbreviation())) return false;
        return !Collections.disjoint(getWars(a), getWars(b));
    }

    public boolean fightsTogetherWith(Country one, Country two) {
        return getWars(one).stream().anyMatch(w ->
                w.getAttackers().stream().anyMatch(ally -> ally.getAbbreviation().equals(two.getAbbreviation()))
        );
    }

    private Set<WarStatus> getOrCreateWars(Country c) {
        return activeWars.computeIfAbsent(
                c.getAbbreviation(),
                k -> ConcurrentHashMap.newKeySet()
        );
    }

    public boolean isEnemy(Country a, Country b) {
        return getWars(a).stream().anyMatch(w ->
                w.getDefenders().stream().anyMatch(ally -> ally.getAbbreviation().equals(b.getAbbreviation()))
        );
    }

    /*public boolean isEnemy(Country a, Country b) {
    if (a == null || b == null) return false;

    String abA = a.getAbbreviation();
    String abB = b.getAbbreviation();

    if (abA.equals(abB)) return false;

    return getWars(a).stream().anyMatch(war ->
            (war.getAttackers().stream()
                    .map(Country::getAbbreviation)
                    .anyMatch(abA::equals)
             &&
             war.getDefenders().stream()
                    .map(Country::getAbbreviation)
                    .anyMatch(abB::equals))
            ||
            (war.getDefenders().stream()
                    .map(Country::getAbbreviation)
                    .anyMatch(abA::equals)
             &&
             war.getAttackers().stream()
                    .map(Country::getAbbreviation)
                    .anyMatch(abB::equals))
    );
    }*/

    public boolean declareWar(boolean callAllies, Tile declaredTile, Country controller, Country aggressor) {

        if (isAtWar(controller, aggressor)) {
            return false;
        }

        warExecutorService.submit(() -> {

            String name = controller.countryName() + "–" + aggressor.countryName() + " War";
            String abbr = controller.getAbbreviation() + "-" + aggressor.getAbbreviation();

            Set<Country> defenders = new HashSet<>();
            defenders.add(controller);

            Set<Country> attackers = new HashSet<>();
            attackers.add(aggressor);

            if(callAllies) {
                //TODO add allies
            }

            Map<Country, Integer> baseVP = new HashMap<>();
            baseVP.put(controller, Server.getInstance().getCountryManager().getTotalVPs(controller));
            baseVP.put(aggressor, Server.getInstance().getCountryManager().getTotalVPs(aggressor));

            WarStatus status = new WarStatus(baseVP, new HashMap<>(baseVP), name, abbr, attackers, defenders);

            getOrCreateWars(controller).add(status);
            getOrCreateWars(aggressor).add(status);

            Server.getInstance().getSendTool().broadcast(Server.getInstance().getServerSocket().getClients(), new ReplyPacket(Type.START_WAR, status.toDataString()));
        });

        return true;
    }

    public void checkWarOver(Country aggressor, Country defender, Tile tile) {
        Set<WarStatus> wars = getWars(aggressor);

        for (WarStatus war : new HashSet<>(wars)) {

            boolean aggressorIsAttacker = war.getAttackers().stream().map(Country::getAbbreviation).collect(Collectors.toSet()).contains(aggressor.getAbbreviation());
            boolean defenderIsDefender = war.getDefenders().stream().map(Country::getAbbreviation).collect(Collectors.toSet()).contains(defender.getAbbreviation());

            if (!aggressorIsAttacker || !defenderIsDefender) {
                continue;
            }

            Map<Country, Integer> vpMap = war.getCurrentVP();

            Country owner = tile.getOwner();

            vpMap.computeIfPresent(defender, (c, vp) -> vp - tile.getVictoryPoints());

            if (owner.getAbbreviation().equals(aggressor.getAbbreviation())) {
                vpMap.computeIfPresent(aggressor, (c, vp) -> vp + tile.getVictoryPoints());
            }

            int base = war.getBaseVP().get(defender);
            int current = vpMap.getOrDefault(defender, 0);

            if (base > 0 && current <= base * 0.35) {
                defender.setCapitulated(true, (v) -> {
                    Server.getInstance().getTroopManager().removeTroops(defender);
                    //TODO: only replace tiles without enemy troops
                    Server.getInstance().getCountryManager().getOwned(defender).forEach(c -> c.setControllerForAll(Server.getInstance().getServerSocket().getClients(), defender));
                    Server.getInstance().getSendTool().broadcast(Server.getInstance().getServerSocket().getClients(), new ReplyPacket(Type.CAPITULATE_COUNTRY, aggressor.getAbbreviation() + ";" + defender.getAbbreviation()));
                });
            }

            boolean defendersDead = war.getDefenders().stream().allMatch(Country::isCapitulated);
            boolean attackersDead = war.getAttackers().stream().allMatch(Country::isCapitulated);

            if (defendersDead || attackersDead || war.getDefenders().isEmpty() || war.getAttackers().isEmpty()) {
                boolean hasAttackersWon = !attackersDead;
                endWar(war, hasAttackersWon);
            }
        }
    }

    private void endWar(WarStatus status, boolean hasAttackingWon) {
        status.end();
        status.setAttackingWon(hasAttackingWon);
        Server.getInstance().getCombatManager().removeByWar(status);

        for (Country c : status.getAttackers()) {
            Set<WarStatus> wars = activeWars.get(c.getAbbreviation());
            if (wars != null) {
                wars.remove(status);
            }
        }

        for (Country c : status.getDefenders()) {
            Set<WarStatus> wars = activeWars.get(c.getAbbreviation());
            if (wars != null) {
                wars.remove(status);
            }
        }

        Server.getInstance().getSendTool().broadcast(Server.getInstance().getServerSocket().getClients(), new ReplyPacket(Type.END_WAR, status.toDataString()));
        Server.getInstance().getConferenceManager().start(status);
    }

    public String serializeWars() {
        StringBuilder sb = new StringBuilder();

        for (Map.Entry<String, Set<WarStatus>> entry : activeWars.entrySet()) {

            String country = entry.getKey();

            for (WarStatus war : entry.getValue()) {
                sb.append(country).append("#").append(war.toDataString()).append("\n");
            }
        }

        return sb.toString();
    }
}