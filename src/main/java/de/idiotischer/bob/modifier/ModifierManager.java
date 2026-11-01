package de.idiotischer.bob.modifier;

import de.idiotischer.bob.BOB;
import de.idiotischer.bob.modifiers.Modifier;
import de.idiotischer.bob.modifiers.ModifierResolver;
import de.idiotischer.bob.networking.packet.impl.pp.RequestPacket;
import de.idiotischer.bob.networking.packet.impl.pp.Type;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class ModifierManager implements ModifierResolver {

    private CompletableFuture<Void> awaitingFuture = new CompletableFuture<>();
    private final Set<Modifier> modifiers = new HashSet<>();

    public ModifierManager() {
        //reload();
    }

    public CompletableFuture<Void> reload() {
        awaitingFuture = new CompletableFuture<>();

        modifiers.clear();

        BOB.getInstance().getSendTool().send(BOB.getInstance().getClient().getChannel(), new RequestPacket(Type.MODIFIERS_SYNC, ""));

        return awaitingFuture;
    }

    public void finishReload() {
        BOB.getInstance().getCountryManager().reload();

        if(awaitingFuture == null || awaitingFuture.isDone()) return;
        awaitingFuture.complete(null);
    }

    @Override
    public Modifier by(String id) {
        return modifiers.stream().filter(mod -> mod.getId().equals(id)).findFirst().orElse(null);
    }

    public void addModifier(Modifier modifier) {
        modifiers.add(modifier);
    }
}
