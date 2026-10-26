package de.idiotischer.bob.faction;

import de.idiotischer.bob.BOB;
import de.idiotischer.bob.Server;
import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.modifiers.Modifier;
import de.idiotischer.bob.networking.packet.impl.pp.RequestPacket;
import de.idiotischer.bob.networking.packet.impl.pp.Type;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class FactionManager implements FactionResolver {

    private final Set<Faction.FactionRule> factionRules = new HashSet<>();

    private final Set<Faction> factions = new HashSet<>();

    private CompletableFuture<Void> awaitingFuture = new CompletableFuture<>();
    private CompletableFuture<Void> awaitingFutureRules = new CompletableFuture<>();

    public CompletableFuture<Void> reloadFactions() {
        awaitingFuture = new CompletableFuture<>();

        factions.clear();

        BOB.getInstance().getSendTool().send(BOB.getInstance().getClient().getChannel(), new RequestPacket(Type.FACTIONS_SYNC, ""));

        return awaitingFuture;
    }

    public CompletableFuture<Void> reloadRules() {
        awaitingFutureRules = new CompletableFuture<>();

        factionRules.clear();

        BOB.getInstance().getSendTool().send(BOB.getInstance().getClient().getChannel(), new RequestPacket(Type.FACTION_RULES_SYNC, ""));

        return awaitingFutureRules;
    }

    @Override
    public Faction.FactionRule ruleBy(String abbreviation) {
        return factionRules.stream().filter(rule -> rule.id().equals(abbreviation)).findFirst().orElse(null);
    }

    @Override
    public Faction factionBy(String abbreviation) {
        return factions.stream().filter(faction -> faction.getAbbreviation().equals(abbreviation)).findFirst().orElse(null);
    }

    public void rmFaction(Faction faction) {
        factions.remove(faction);
    }

    public void addFaction(Faction faction) {
        factions.add(faction);
    }

    public void finishRulesReload() {
        this.reloadFactions();

        if(awaitingFutureRules == null || awaitingFutureRules.isDone()) return;
        awaitingFutureRules.complete(null);
    }

    public void finishFactionsReload() {
        BOB.getInstance().getTileManager().reload();

        if(awaitingFuture == null || awaitingFuture.isDone()) return;
        awaitingFuture.complete(null);
    }


    public void rmFactionRule(Faction.FactionRule faction) {
        factionRules.remove(faction);
    }

    public void addFactionRule(Faction.FactionRule faction) {
        factionRules.add(faction);
    }

    public Set<Country> getFactionMembers(Faction faction) {
        return faction.getMembers().stream().map(m -> Server.getInstance().getCountryManager().byAbbreviation(m)).collect(Collectors.toSet());
    }

    public Set<Faction> getFactionsFor(Country c) {
        return factions.stream().filter(fac -> fac.getMembers().contains(c.getAbbreviation())).collect(Collectors.toSet());
    }
}
