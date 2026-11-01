package de.idiotischer.bob;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.luciad.imageio.webp.WebP;
import de.craftsblock.craftscore.event.ListenerRegistry;
import de.idiotischer.bob.networking.communication.SendTool;
import de.idiotischer.bob.networking.packet.PacketRegistry;
import de.idiotischer.bob.util.FileUtil;

import java.nio.file.Path;

public class SharedCore {

    private final PacketRegistry registry;
    private final Path configs;
    private final ListenerRegistry listenerRegistry = new ListenerRegistry();
    private final SendTool tool = new SendTool(this);

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final int maxNameLength = 16;
    private static final long TICK_NANOS = 550000000; //so you can see every minute
    private static int PAUSE_DELAY = 20000;

    private static final long MIN_COMPRESSION_SIZE = 256;
    private static final long MIN_SAVING = 16;

    public SharedCore() {
        WebP.loadNativeLibrary();

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

    public static int getMaxNameLength() {
        return maxNameLength;
    }

    public static long getTickNanos() {
        return TICK_NANOS;
    }

    //fetch from config
    public static int getPauseDelay() {
        return PAUSE_DELAY;
    }

    public static long getTickDelay(int speed) {
        return switch (speed) {
            case 1 -> SharedCore.getTickNanos();
            case 2 -> SharedCore.getTickNanos() / speed / 3;
            case 3 -> SharedCore.getTickNanos() / speed / 8;
            case 4 -> SharedCore.getTickNanos() / speed / 20;
            case 5 -> SharedCore.getTickNanos() / speed / 40;
            default -> SharedCore.getTickNanos();
        };
    }

    public static long getMinCompressionSize() {
        return MIN_COMPRESSION_SIZE;
    }

    public static long getMinSaving() {
        return MIN_SAVING;
    }
}
