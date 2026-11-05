package de.idiotischer.bob.troop;

import de.idiotischer.bob.country.Country;
import de.idiotischer.bob.country.CountryResolver;
import de.idiotischer.bob.networking.ChannelResolver;
import de.idiotischer.bob.networking.communication.SendTool;
import de.idiotischer.bob.networking.packet.impl.pp.ReplyPacket;
import de.idiotischer.bob.networking.packet.impl.pp.Type;
import de.idiotischer.bob.state.State;
import de.idiotischer.bob.tile.Tile;
import de.idiotischer.bob.tile.TileResolver;
import it.unimi.dsi.fastutil.Pair;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

//TODO: make troops contain their uuid
public class Troop {

    private final String name;
    protected String template;
    private Tile tile;
    private Country controller; // remove direct reference, use string
    private Country owner; // remove direct reference, use string


    public Troop(String name,Tile tile, Country owner, Country controller, String template) {
        this.name = name;
        this.owner = owner;
        this.template = template;
        this.tile = tile;
        this.controller = controller;
    }

    public void setControllerServer(ChannelResolver r, SendTool t, UUID uuid, Country controller) {
        this.controller = controller;

        t.broadcast(r.channels(), new ReplyPacket(Type.TROOP_UPDATE, "id=" + uuid.toString() +";controller=" + controller.getAbbreviation()));
    }

    public void setControllerServer(ChannelResolver r, SendTool t, UUID uuid, String template) {
        this.template = template;

        t.broadcast(r.channels(), new ReplyPacket(Type.TROOP_UPDATE, "id=" + uuid.toString() +";template=" + template));
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public void setOwner(Country owner) {
        this.owner = owner;
    }

    public void setController(Country controller) {
        this.controller = controller;
    }

    public void setOwnerServer(ChannelResolver r, SendTool t, UUID uuid, Country owner) {
        this.owner = owner;

        t.broadcast(r.channels(), new ReplyPacket(Type.TROOP_UPDATE, "id=" + uuid.toString() + ";owner=" + owner.getAbbreviation()));
    }

    public String getName() {
        return name;
    }

    public boolean isVisible() {
        return true;
    }

    public String getTemplate() {
        return template; //again (like in all structures i build) this i
    }

    public Tile getTile() {
        return tile;
    }

    public void setTile(Tile tile) {
        this.tile = tile;
    }

    public Country getOwner() {
        return owner;
    }

    public Country getController() {
        return controller != null ? controller : owner;
    }

}
