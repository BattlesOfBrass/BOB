package de.idiotischer.bob.networking;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import de.idiotischer.bob.networking.packet.impl.PingPacket;

import java.io.FileReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

public class ClientSocket {
    private AsynchronousChannelGroup workerGroup;
    private AsynchronousSocketChannel channel;

    private int port = 3995;
    private int remotePort = 2776;
    private String host = "127.0.0.1";
    private boolean multiplayerEnabled = false;
    private boolean useSpecifications = false;


    public ClientSocket() {
        loadDetails();

        if(!multiplayerEnabled) return;

        try {
            workerGroup = AsynchronousChannelGroup.withFixedThreadPool(3, Thread::new);
            channel = AsynchronousSocketChannel.open(workerGroup);

            //channel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
            //channel.setOption(StandardSocketOptions.TCP_NODELAY, true);

            if(useSpecifications) channel.bind(new InetSocketAddress("localhost", port));

            channel.connect(new InetSocketAddress(host, remotePort), null, new CompletionHandler<Void, Void>() {
                @Override
                public void completed(Void result, Void attachment) {
                    System.out.println("Connected to server!");
                    listen();
                }

                @Override
                public void failed(Throwable exc, Void attachment) {
                    if(exc.getMessage().contains("Connection refused")) return;
                    if(exc.getMessage().contains("Connection reset by peer")) return;

                    exc.printStackTrace();
                }
            });

            BOB.getInstance().getSendTool().send(channel, new PingPacket());
            //MOM.getInstance().getSendTool().sendTo(channel, new PingPacket());
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public void listen() {
        if(channel == null || !channel.isOpen()) return;

        ByteBuffer buffer = ByteBuffer.allocate(1024);

        channel.read(buffer, buffer, new CompletionHandler<>() {
            @Override
            public void completed(Integer bytesRead, ByteBuffer attachment) {
                if (bytesRead == -1) {
                    try {
                        channel.close();
                    } catch (Exception ignored) {}
                    System.out.println("Disconnected from server.");
                    return;
                }

                attachment.flip();

                BOB.getInstance().getSharedCore().getRegistry().getDecoder().code(attachment);

                attachment.clear();

                channel.read(attachment, attachment, this);
            }

            @Override
            public void failed(Throwable exc, ByteBuffer attachment) {
                exc.printStackTrace();
                try {
                    channel.close();
                } catch (Exception ignored) {}
            }
        });
    }

    public void loadDetails() {
        try (JsonReader reader = new JsonReader(new FileReader(BOB.getInstance().getSharedCore().getConfigs().resolve("host.json").toFile()))) {
            JsonElement root = BOB.getInstance().getScenarioSceneLoader().getGson().fromJson(reader, JsonElement.class);

            root.getAsJsonObject().entrySet().forEach(entry -> {
                JsonObject countryElement = entry.getValue().getAsJsonObject();

                if(entry.getKey().equals("remote")) {
                    remotePort = countryElement.get("remotePort").getAsInt();
                    host = countryElement.get("remoteHost").getAsString();
                }
                if(entry.getKey().equals("local")) {
                    port = countryElement.get("localPort").getAsInt();
                    useSpecifications = countryElement.get("useSpecifications").getAsBoolean();
                    multiplayerEnabled = countryElement.get("multiplayerEnabled").getAsBoolean();
                }
            });
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public int getPort() {
        return port;
    }

    public int getRemotePort() {
        return remotePort;
    }

    public String getHost() {
        return host;
    }

    public AsynchronousSocketChannel getChannel() {
        return channel;
    }
}
