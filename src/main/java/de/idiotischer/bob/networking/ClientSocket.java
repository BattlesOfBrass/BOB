package de.idiotischer.bob.networking;

import de.idiotischer.bob.BOB;
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

public class ClientSocket {
    private AsynchronousChannelGroup workerGroup;
    private AsynchronousSocketChannel channel;

    private final HostUtil hostUtil = new HostUtil();

    public ClientSocket() {
        loadDetails();

        if(!hostUtil.isMultiplayerEnabled()) return;

        try {
            workerGroup = AsynchronousChannelGroup.withFixedThreadPool(3, Thread::new);
            channel = AsynchronousSocketChannel.open(workerGroup);
        } catch(IOException e) {
            e.printStackTrace();
        }

        //channel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
        //channel.setOption(StandardSocketOptions.TCP_NODELAY, true);
        int port = hostUtil.getLocalPort();

        while (true) {
            try {
                if (hostUtil.isUseSpecifications()) {
                    channel.bind(new InetSocketAddress("localhost", port));
                }
                break;
            } catch (BindException e) {
                port++;
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
        }

        channel.connect(new InetSocketAddress(hostUtil.getHost(), hostUtil.getRemotePort()), null, new CompletionHandler<Void, Void>() {
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
    }

    public void listen() {
        if (channel == null || !channel.isOpen()) return;

        ByteBuffer buffer = ByteBuffer.allocate(8192);

        channel.read(buffer, buffer, new CompletionHandler<>() {
            @Override
            public void completed(Integer bytesRead, ByteBuffer attachment) {
                if (bytesRead == -1) {
                    handleDisconnect();
                    return;
                }

                attachment.flip();

                while (attachment.hasRemaining()) {
                    attachment.mark();

                    Object packet = BOB.getInstance().getSharedCore().getRegistry()
                            .getDecoder().code(attachment, channel);

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

                channel.read(attachment, attachment, this);
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

                handleDisconnect();
            }

        });
    }

    private void handleDisconnect() {
        if(!channel.isOpen()) return;

        BOB.getInstance().setHost(false);
        BOB.getInstance().getPlayerManager().removeExceptAddress(AddressUtil.getThisAddress(channel));
        try {
            channel.close();
        } catch (Exception ignored) {}

        if(BOB.getInstance().isDebug()) System.out.println("Disconnected from server.");
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

    public void shutdown() {
        try {
            handleDisconnect();
            workerGroup.shutdownNow();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
