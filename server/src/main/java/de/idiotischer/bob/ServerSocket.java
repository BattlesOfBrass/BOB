package de.idiotischer.bob;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import de.idiotischer.bob.util.FileUtil;
import de.idiotischer.bob.util.HostUtil;

import java.io.FileReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ServerSocket {

    private AsynchronousChannelGroup workerGroup;
    private AsynchronousServerSocketChannel channel;

    //immer erster ist der peer bzw hoster
    private final Set<AsynchronousSocketChannel> clients = Collections.synchronizedSet(new HashSet<>());

    private HostUtil hostUtil = new HostUtil();

    public ServerSocket() {
        loadDetails();

        try {
            workerGroup = AsynchronousChannelGroup.withFixedThreadPool(3, Thread::new);
            channel = AsynchronousServerSocketChannel.open(workerGroup);

            //channel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
            //channel.setOption(StandardSocketOptions.TCP_NODELAY, true);

            channel.bind(new InetSocketAddress("localhost", hostUtil.getLocalPort()));

            startAccepting();
        } catch(Exception e) {
            e.printStackTrace();
        }

        //TODO: vor production removen
        try {
            Thread.sleep(400000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void startAccepting() {
        channel.accept(null, new CompletionHandler<AsynchronousSocketChannel, Void>() {
            @Override
            public void completed(AsynchronousSocketChannel clientChannel, Void attachment) {
                clients.add(clientChannel);

                System.out.println("New client connected: " + clientChannel);

                channel.accept(null, this);

                readFromClient(clientChannel);
            }

            @Override
            public void failed(Throwable exc, Void attachment) {
                exc.printStackTrace();
                channel.accept(null, this);
            }
        });
    }

    private void readFromClient(AsynchronousSocketChannel clientChannel) {
        ByteBuffer buffer = ByteBuffer.allocate(1024);

        clientChannel.read(buffer, buffer, new CompletionHandler<>() {
            @Override
            public void completed(Integer result, ByteBuffer attachment) {
                if (result == -1) {
                    clients.remove(clientChannel);
                    try {
                        clientChannel.close();
                    } catch (Exception ignored) {}
                    return;
                }

                attachment.flip();

                Server.getInstance().getCore().getRegistry().getDecoder().code(attachment);

                attachment.clear();

                clientChannel.read(attachment, attachment, this);
            }

            @Override
            public void failed(Throwable exc, ByteBuffer attachment) {
                if(!exc.getMessage().contains("Connection refused") || !exc.getMessage().contains("Connection reset by peer"))
                    exc.printStackTrace();

                clients.remove(clientChannel);
                try {
                    clientChannel.close();
                } catch (Exception ignored) {}
            }
        });
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
}
