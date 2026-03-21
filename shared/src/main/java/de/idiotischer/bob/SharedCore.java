package de.idiotischer.bob;

import de.craftsblock.craftscore.event.ListenerRegistry;
import de.idiotischer.bob.networking.packet.PacketRegistry;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public class SharedCore {
    private final PacketRegistry registry;
    private Path configs;
    private final ListenerRegistry listenerRegistry = new ListenerRegistry();

    public SharedCore() {
        this.registry = new PacketRegistry(this);

        try {
            configs = Paths.get(Objects.requireNonNull(getClass()
                    .getClassLoader()
                    .getResource("config/")).toURI());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

    }

    public PacketRegistry getRegistry() {
        return registry;
    }

    public Path getConfigs() {
        return configs;
    }

    public ListenerRegistry getListenerRegistry() {
        return listenerRegistry;
    }
}
