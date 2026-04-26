package de.idiotischer.bob;

import de.idiotischer.bob.event.ClientConnectEvent;
import de.idiotischer.bob.player.Player;
import de.idiotischer.bob.util.AddressUtil;
import de.idiotischer.bob.util.HostUtil;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

//server PW adden
public class ServerSocket {

    private AsynchronousChannelGroup workerGroup;
    private AsynchronousServerSocketChannel channel;

    //immer erster ist der peer bzw hoster
    private final Set<AsynchronousSocketChannel> clients = Collections.synchronizedSet(new HashSet<>());

    private HostUtil hostUtil = new HostUtil();

    private boolean local;

    public ServerSocket(boolean local) {
        this.local = local;

        loadDetails();

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

        //TODO: vor production removen
        //try {
        //    Thread.sleep(400000);
        //} catch (InterruptedException e) {
        //    throw new RuntimeException(e);
        //}
    }

    public void setLocal(boolean local) {
        this.local = local;
    }

    public boolean isLocal() {
        return local;
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

                    clients.add(clientChannel);

                    Player p = Server.getInstance().getPlayerManager().createPlayer(AddressUtil.getRemoteAddress(clientChannel));
                    Server.getInstance().getPlayerManager().addPlayer(p);

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
                if (!(exc instanceof AsynchronousCloseException) &&
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

        clientChannel.read(initialBuffer, initialBuffer, new CompletionHandler<>() {
            @Override
            public void completed(Integer result, ByteBuffer buffer) {
                if (result == -1) {
                    cleanup(clientChannel);
                    return;
                }

                buffer.flip();

                while (buffer.hasRemaining()) {
                    buffer.mark();
                    Object packet = Server.getInstance().getCore().getRegistry()
                            .getDecoder().code(buffer, clientChannel);

                    if (packet == null) {
                        buffer.reset();
                        break;
                    }
                }

                buffer.compact();

                if (buffer.position() == buffer.capacity()) {
                    ByteBuffer expandedBuffer = ByteBuffer.allocate(buffer.capacity() * 2);
                    buffer.flip();
                    expandedBuffer.put(buffer);

                    buffer = expandedBuffer;

                    System.out.println("Buffer expanded to " + buffer.capacity() + " bytes to fit large packet.");
                }

                clientChannel.read(buffer, buffer, this);
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

    private void cleanup(AsynchronousSocketChannel clientChannel) {
        clients.remove(clientChannel);
        try {
            clientChannel.close();
        } catch (Exception ignored) {}
    }

    public void shutdown() {
        try {
            workerGroup.shutdownNow();
            clients.forEach(client -> {
                try {
                    client.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            clients.clear();
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
        return Collections.unmodifiableSet(clients);
    }

    public HostUtil getHostUtil() {
        return hostUtil;
    }
}
