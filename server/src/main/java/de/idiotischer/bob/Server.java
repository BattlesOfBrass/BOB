package de.idiotischer.bob;

import de.idiotischer.bob.listener.PacketEvents;
import de.idiotischer.bob.networking.communication.SendTool;
import de.idiotischer.bob.util.FileUtil;

public class Server {
    private static Server instance;
    private final SharedCore core = new SharedCore();
    private SendTool sendTool;
    private ServerSocket serverSocket;

    static void main() {
        new Server();
    }

    public Server() {
        instance = this;

        FileUtil.replaceIfNotExistingAsync(this.getClass().getClassLoader()).join();

        registerListeners();

        init();
    }

    public void init() {
        sendTool = new SendTool(core);
        serverSocket = new ServerSocket();
    }

    private void registerListeners() {
        core.getListenerRegistry().register(new PacketEvents());
    }

    public static Server getInstance() {
        return instance;
    }

    public SharedCore getCore() {
        return core;
    }

    public SendTool getSendTool() {
        return sendTool;
    }

    public ServerSocket getServerSocket() {
        return serverSocket;
    }
}
