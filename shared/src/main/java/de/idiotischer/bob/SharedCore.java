package de.idiotischer.bob;

import com.google.gson.Gson;
import de.craftsblock.craftscore.event.ListenerRegistry;
import de.idiotischer.bob.networking.packet.PacketRegistry;
import de.idiotischer.bob.util.FileUtil;

import java.nio.file.Path;

public class SharedCore {

    private final PacketRegistry registry;
    private final Path configs;
    private final ListenerRegistry listenerRegistry = new ListenerRegistry();

    public static final Gson GSON = new Gson();

    public SharedCore() {
        this.registry = new PacketRegistry(this);

        configs = FileUtil.getConfigDir();
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
