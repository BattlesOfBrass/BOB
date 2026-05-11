package de.idiotischer.bob.networking;

import de.idiotischer.bob.BOB;
import de.idiotischer.bob.ServerSocket;
import de.idiotischer.bob.auth.Credentials;
import de.idiotischer.bob.networking.packet.impl.LoginPacket;
import de.idiotischer.bob.networking.packet.impl.PingPacket;
import de.idiotischer.bob.util.AddressUtil;
import de.idiotischer.bob.util.HostUtil;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class ClientSocket {

    private AsynchronousChannelGroup workerGroup;
    private AsynchronousSocketChannel channel;

    private final HostUtil hostUtil = new HostUtil();

    private volatile boolean connected = false;
    private volatile boolean reconnecting = false;
    private int reconnectCount = 0;
    private InetSocketAddress lastAttempted;
    private final int max_reconnection = 6;

    private long lastAttemptTime = 0L;
    private final long reconnect_delay = TimeUnit.SECONDS.toMillis(30);

    public ClientSocket() {
        loadDetails();

        try {
            workerGroup = AsynchronousChannelGroup.withFixedThreadPool(3, Thread::new);
        } catch (IOException e) {
            e.printStackTrace();
        }

        start(new InetSocketAddress(hostUtil.getHost(), hostUtil.getRemotePort()));
    }

    public void start(InetSocketAddress address) {
        start("","",address, null);
    }

    public void start(String name, String pw,InetSocketAddress address, Consumer<Boolean> callback) {
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastAttemptTime > reconnect_delay) {
            if (reconnectCount >= max_reconnection) {
                if(BOB.getInstance().isDebug()) System.out.println("Connection CD finished. Resetting limit for " + address);
            }
            reconnectCount = 0;
        }

        if (reconnectCount >= max_reconnection) {
            long waitRemaining = (reconnect_delay - (currentTime - lastAttemptTime)) / 1000;
            if(BOB.getInstance().isDebug()) System.out.println("Reconnected too often to " + address + ".");
            if(BOB.getInstance().isDebug()) System.out.println("Please wait " + Math.max(0, waitRemaining) + "s before trying again.");
            System.out.println("Can't reconnect anymore! Took too many tries!"); //vorrübergehend als message an den user

            if (callback != null) callback.accept(false);
            return;
        }

        if (address.equals(lastAttempted)) {
            reconnectCount++;
        } else {
            reconnectCount = 1;
            lastAttempted = address;
        }

        lastAttemptTime = currentTime;

        if (!hostUtil.isMultiplayerEnabled()) {
            if (callback != null) callback.accept(false);
            return;
        }

        try {
            channel = AsynchronousSocketChannel.open(workerGroup);
        } catch (IOException e) {
            e.printStackTrace();
            if (callback != null) callback.accept(false);
            return;
        }

        int port = hostUtil.getLocalPort();

        boolean bound = false;

        for (int i = 0; i < 100; i++) {
            try {
                if (hostUtil.isUseSpecifications()) {
                    channel.bind(new InetSocketAddress("localhost", port));
                }

                bound = true;
                break;

            } catch (BindException e) {
                port++;
            } catch (Exception e) {

                e.printStackTrace();
                break;
            }
        }

        if (!bound) {
            if(BOB.getInstance().isDebug()) System.out.println("Failed to bind socket after 100 attempts.");

            if (callback != null) {
                callback.accept(false);
            }

            return;
        }

        System.out.println("Connecting to " + address);

        channel.connect(address, null, new CompletionHandler<>() {

            @Override
            public void completed(Void result, Object attachment) {
                connected = true;
                reconnecting = false;

                System.out.println("Connected to server!");

                BOB.getInstance().getSendTool().send(channel, new LoginPacket(new Credentials(name, pw)));

                BOB.getInstance().getSendTool().send(channel, new PingPacket());

                if (callback != null) {
                    callback.accept(true);
                }

                listen();
            }

            @Override
            public void failed(Throwable exc, Object attachment) {

                connected = false;

                System.out.println("Connection failed: " + exc);

                if (callback != null) {
                    callback.accept(false);
                }
            }
        });
    }

    public void listen() {

        if (channel == null || !channel.isOpen()) {
            return;
        }

        ByteBuffer buffer = ByteBuffer.allocate(8192);

        channel.read(buffer, buffer, new CompletionHandler<>() {
            @Override
            public void completed(Integer bytesRead, ByteBuffer attachment) {
                if (bytesRead == -1) {
                    handleDisconnect(true);
                    return;
                }

                connected = true;
                reconnecting = false;
                reconnectCount = 0;

                attachment.flip();

                while (attachment.hasRemaining()) {

                    attachment.mark();

                    Object packet = BOB.getInstance()
                            .getSharedCore()
                            .getRegistry()
                            .getDecoder()
                            .code(attachment, channel);

                    if (packet == null) {
                        attachment.reset();
                        break;
                    }
                }

                attachment.compact();

                if (attachment.position() == attachment.capacity()) {
                    ByteBuffer expanded = ByteBuffer.allocate(attachment.capacity() * 2);

                    attachment.flip();
                    expanded.put(attachment);

                    attachment = expanded;
                }

                if (channel != null && channel.isOpen()) {
                    channel.read(attachment, attachment, this);
                }
            }

            @Override
            public void failed(Throwable exc, ByteBuffer attachment) {

                if (exc instanceof AsynchronousCloseException) {
                    return;
                }

                String msg = exc.getMessage();

                if (msg == null
                        || (!msg.contains("Connection reset")
                        && !msg.contains("closed"))) {

                    exc.printStackTrace();
                }

                handleDisconnect(true);
            }
        });
    }

    private void handleDisconnect(boolean reconnect) {
        connected = false;

        if (channel != null) {
            try {

                if (channel.isOpen()) {

                    BOB.getInstance().setHost(false);

                    BOB.getInstance()
                            .getPlayerManager()
                            .removeExceptAddress(
                                    AddressUtil.getThisAddress(channel)
                            );

                    channel.close();
                }

            } catch (Exception ignored) {
            }
        }

        if (BOB.getInstance().isDebug()) {
            System.out.println("Disconnected from server.");
        }

        if (!reconnect || reconnecting) {
            return;
        }

        reconnecting = true;

        try {

            if (BOB.getInstance().isDebug()) {
                System.out.println("Switching to local server");
            }

            ServerSocket server = BOB.getInstance().getLocalServer().getServerSocket();
            //if(BOB.getInstance().getScenarioSceneLoader() != null) BOB.getInstance().getScenarioSceneLoader().requestCurrent();

            if (server == null) {
                reconnecting = false;
                return;
            }

            if (!server.getChannel().isOpen()) {
                server.start();

                try {
                    Thread.sleep(200);
                } catch (InterruptedException ignored) {}
            }

            reconnect("","",AddressUtil.getThisAddress(server.getChannel()), null);

        } catch (Exception e) {
            reconnecting = false;
            e.printStackTrace();
        }
    }

    public void reconnect(String name, String pw, InetSocketAddress address,
                          Consumer<Boolean> callback) {
        closeCurrentChannel();

        start(name,pw,address, callback);
    }

    private void closeCurrentChannel() {

        connected = false;

        if (channel == null) {
            return;
        }

        try {

            if (channel.isOpen()) {
                channel.close();
            }

        } catch (Exception ignored) {
        }
    }

    public void shutdown() {

        connected = false;
        reconnecting = false;

        closeCurrentChannel();

        try {

            if (workerGroup != null
                    && !workerGroup.isShutdown()) {

                workerGroup.shutdownNow();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadDetails() {
        hostUtil.reload();
    }

    public int getPort() {
        return hostUtil.getLocalPort();
    }

    public int getRemotePort() {
        return hostUtil.getRemotePort();
    }

    public String getHost() {
        return hostUtil.getHost();
    }

    public AsynchronousSocketChannel getChannel() {
        return channel;
    }

    public boolean isConnected() {
        return connected;
    }
}