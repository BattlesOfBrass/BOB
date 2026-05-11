package de.idiotischer.bob;

import de.idiotischer.bob.event.ClientConnectEvent;
import de.idiotischer.bob.player.Player;
import de.idiotischer.bob.player.ServerPlayer;
import de.idiotischer.bob.util.AddressUtil;
import de.idiotischer.bob.util.HostUtil;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

//TODO: server PW adden
//TODO: client timeout und threshold adden usw damit man nd botten kann
public class ServerSocket {

    private AsynchronousChannelGroup workerGroup;
    private AsynchronousServerSocketChannel channel;

    private final HostUtil hostUtil = new HostUtil();
    private ScheduledExecutorService timeoutScheduler;

    private boolean local;
    private final Map<AsynchronousSocketChannel, Long> lastAction = new ConcurrentHashMap<>();
    private static final long threshold = 30000;

    public ServerSocket(boolean local) {
        this.local = local;

        start();

        //TODO: vor production removen
        //try {
        //    Thread.sleep(400000);
        //} catch (InterruptedException e) {
        //    throw new RuntimeException(e);
        //}
    }

    public void start() {
        loadDetails();
        startTimeoutChecker();

        try {
            workerGroup = AsynchronousChannelGroup.withFixedThreadPool(3, Thread::new);
            channel = AsynchronousServerSocketChannel.open(workerGroup);

            //channel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
            //channel.setOption(StandardSocketOptions.TCP_NODELAY, true);

            channel.bind(new InetSocketAddress("localhost", hostUtil.getRemotePort()));

            startAccepting();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public void setLocal(boolean local) {
        this.local = local;
    }

    public boolean isLocal() {
        return local;
    }

    private void startTimeoutChecker() {
        this.timeoutScheduler = Executors.newSingleThreadScheduledExecutor();

        timeoutScheduler.scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();
            lastAction.forEach((channel, time) -> {
                if (now - time > threshold) {
                    if(Server.getInstance().isDebug()) System.out.println("Client timed out: " + channel);
                    cleanup(channel);
                }
            });
        }, 10, 10, TimeUnit.SECONDS);
    }

    private void startAccepting() {
        channel.accept(null, new CompletionHandler<AsynchronousSocketChannel, Void>() {
            @Override
            public void completed(AsynchronousSocketChannel clientChannel, Void attachment) {
                try {
                    InetSocketAddress remoteAddress = (InetSocketAddress) clientChannel.getRemoteAddress();

                    if (isLocal() && !remoteAddress.getAddress().isLoopbackAddress()) {
                        System.out.println("Blocked non-local client: " + remoteAddress);
                        clientChannel.close();
                        if (channel.isOpen()) {
                            channel.accept(null, this);
                        }
                        return;
                    }

                    ClientConnectEvent event = new ClientConnectEvent(clientChannel);

                    Server.getInstance().getCore().getListenerRegistry().call(event);

                    if(event.isCancelled()) {
                        //clients.remove(clientChannel);
                        clientChannel.close();
                    }

                    Player p = Server.getInstance().getPlayerManager().createPlayer(clientChannel, AddressUtil.getRemoteAddress(clientChannel));
                    Server.getInstance().getPlayerManager().addPlayer(p);

                    lastAction.put(clientChannel, System.currentTimeMillis());

                    System.out.println("New client connected: " + remoteAddress);

                    if (channel.isOpen()) {
                        channel.accept(null, this);
                    }

                    readFromClient(clientChannel);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void failed(Throwable exc, Void attachment) {
                if (/*!(exc instanceof AsynchronousCloseException) &&*/
                        !(exc instanceof ClosedChannelException)) {
                    exc.printStackTrace();
                }

                if (channel.isOpen()) {
                    channel.accept(null, this);
                }
            }
        });
    }

    private void readFromClient(AsynchronousSocketChannel clientChannel) {
        ByteBuffer initialBuffer = ByteBuffer.allocate(8192);

        if (clientChannel != null && clientChannel.isOpen()) {
            clientChannel.read(initialBuffer, initialBuffer, new CompletionHandler<>() {
                @Override
                public void completed(Integer result, ByteBuffer buffer) {
                    if (result == -1) {
                        cleanup(clientChannel);
                        return;
                    }

                    lastAction.put(clientChannel, System.currentTimeMillis());

                    buffer.flip();
                    try {
                        while (buffer.hasRemaining()) {
                            buffer.mark();
                            Object packet = Server.getInstance().getCore().getRegistry()
                                    .getDecoder().code(buffer, clientChannel);

                            if (packet == null) {
                                buffer.reset();
                                break;
                            }
                        }

                        if (!clientChannel.isOpen()) return;

                        buffer.compact();

                        ByteBuffer nextBuffer = buffer;
                        if (buffer.position() == buffer.capacity()) {
                            nextBuffer = ByteBuffer.allocate(buffer.capacity() * 2);
                            buffer.flip();
                            nextBuffer.put(buffer);
                        }

                        clientChannel.read(nextBuffer, nextBuffer, this);
                    } catch (Exception e) {
                        e.printStackTrace();
                        cleanup(clientChannel);
                    }
                }

                @Override
                public void failed(Throwable exc, ByteBuffer attachment) {
                    if (exc instanceof AsynchronousCloseException) {
                        return;
                    }

                    String msg = exc.getMessage();
                    if (msg == null || (!msg.contains("Connection reset") && !msg.contains("closed"))) {
                        exc.printStackTrace();
                    }

                    cleanup(clientChannel);
                }
            });
        }
    }

    private void cleanup(AsynchronousSocketChannel clientChannel) {
        lastAction.remove(clientChannel);
        Server.getInstance().getPlayerManager().removePlayer(clientChannel);
        try {
            if (clientChannel.isOpen()) {
                clientChannel.close();
            }
        } catch (Exception ignored) {}
    }

    public void shutdown() {
        try {
            timeoutScheduler.shutdownNow();
            workerGroup.shutdownNow();
            Server.getInstance().getPlayerManager().getPlayers().forEach(client -> {
                try {
                    client.clientChannel().close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            Server.getInstance().getPlayerManager().clear();
            channel.close();
        } catch (Exception ignored) {}
    }

    public void loadDetails() {
        hostUtil.reload();
    }

    public AsynchronousServerSocketChannel getChannel() {
        return channel;
    }

    public Set<AsynchronousSocketChannel> getClients() {
        return Server.getInstance().getPlayerManager().getPlayers().stream().filter(Player::authorized).map(Player::clientChannel).collect(Collectors.toUnmodifiableSet());
    }

    public HostUtil getHostUtil() {
        return hostUtil;
    }
}
