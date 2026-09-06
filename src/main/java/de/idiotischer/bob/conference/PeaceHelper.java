package de.idiotischer.bob.conference;

import de.idiotischer.bob.BOB;
import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.networking.packet.impl.pp.RequestPacket;
import de.idiotischer.bob.networking.packet.impl.pp.Type;
import de.idiotischer.bob.render.menu.impl.select.PeaceMenuOverlay;
import de.idiotischer.bob.tile.Tile;
import de.idiotischer.bob.tile.TileResolver;
import it.unimi.dsi.fastutil.Pair;

import java.util.*;
import java.util.stream.Collectors;

public class PeaceHelper {

    private final Set<PeaceConference> peaces = new HashSet<>();

    //later make it work likle hoi4 so you can select stfuf from multiple types without going to the next round
    public void sendDemands(UUID conf, TakeTileType type, Set<Tile> tiles, Country country) {
        String s = country.getAbbreviation() + ";" + conf.toString() + ";" + type.name().toUpperCase() + ";" + tiles.stream().map(Tile::getAbbreviation).collect(Collectors.joining(";"));

        BOB.getInstance().getSendTool().send(BOB.getInstance().getClient().getChannel(), new RequestPacket(Type.SEND_DEMANDS, s));
    }

    public void quitConference(UUID conf) {
        BOB.getInstance().getSendTool().send(BOB.getInstance().getClient().getChannel(), new RequestPacket(Type.END_CONFERENCE, conf.toString()));
    }

    public void addPeaces(PeaceConference conference) {
        peaces.add(conference);

        if(peaces.stream().noneMatch(peace -> peace.getWinners().stream().map(Country::getAbbreviation).toList().contains(BOB.getInstance().getPlayer().country().getAbbreviation()))) {
            //TODO: open the waiting popup hihihihiihihihi
            return;
        }

        BOB.getInstance().getMainRenderer().getGamePanel().setPaused(true);
        BOB.getInstance().getMainRenderer().getGamePanel().showPeaceOverlay();
        BOB.getInstance().getMainRenderer().getGamePanel().setPeace(conference.getUUID());
    }

    public void removePeaces(UUID uuid) {
        peaces.removeIf(c -> c.getUUID().equals(uuid));
        if( BOB.getInstance().getMainRenderer() == null) return;
        PeaceMenuOverlay.setWaiting(false);
        BOB.getInstance().getMainRenderer().getGamePanel().setPaused(false);
        BOB.getInstance().getMainRenderer().getGamePanel().removePeaceOverlay();
    }

    public PeaceConference getBy(UUID uuid) {
        return peaces.stream().filter(p -> p.getUUID().equals(uuid)).findFirst().orElse(null);
    }

    public void clear() {
        peaces.clear();
        if( BOB.getInstance().getMainRenderer() == null) return;
        PeaceMenuOverlay.setWaiting(false);
        BOB.getInstance().getMainRenderer().getGamePanel().setPaused(false);
        BOB.getInstance().getMainRenderer().getGamePanel().removePeaceOverlay();
    }

    public void nextRound() {
        PeaceMenuOverlay.setWaiting(false);
        PeaceMenuOverlay.removeWaitingPopup();
    }
}
