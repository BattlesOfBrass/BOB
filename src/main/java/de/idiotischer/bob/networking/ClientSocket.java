package de.idiotischer.bob.networking;

import de.idiotischer.bob.BOB;
import de.idiotischer.bob.networking.packet.impl.PingPacket;
import de.idiotischer.bob.util.HostUtil;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
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

            //channel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
            //channel.setOption(StandardSocketOptions.TCP_NODELAY, true);

            if(hostUtil.isUseSpecifications()) channel.bind(new InetSocketAddress("localhost", hostUtil.getLocalPort()));

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
                    BOB.getInstance().setHost(false);
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

                BOB.getInstance().setHost(false);

                try {
                    channel.close();
                } catch (Exception ignored) {}
            }
        });
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
}
