package de.idiotischer.bob.util;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.AsynchronousSocketChannel;

public class AdressUtil {
    public static InetSocketAddress getClientAddress(AsynchronousSocketChannel channel) {
        if (channel == null) {
            return null;
        }

        try {
            SocketAddress addr = channel.getRemoteAddress();

            if (addr instanceof InetSocketAddress) {
                return (InetSocketAddress) addr;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
