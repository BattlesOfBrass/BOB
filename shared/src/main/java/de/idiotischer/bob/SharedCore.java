package de.idiotischer.bob;

import com.google.gson.Gson;
import de.craftsblock.craftscore.buffer.BufferUtil;
import de.craftsblock.craftscore.event.ListenerRegistry;
import de.idiotischer.bob.networking.communication.SendTool;
import de.idiotischer.bob.networking.packet.PacketRegistry;
import de.idiotischer.bob.util.FileUtil;
import de.idiotischer.bob.util.MainConfigUtil;

import java.nio.file.Path;

public class SharedCore {

    private final PacketRegistry registry;
    private final Path configs;
    private final ListenerRegistry listenerRegistry = new ListenerRegistry();
    private final SendTool tool = new SendTool(this);

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

    public SendTool getTool() {
        return tool;
    }

    public ListenerRegistry getListenerRegistry() {
        return listenerRegistry;
    }
}
