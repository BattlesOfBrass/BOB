package de.idiotischer.bob.util;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;

public class AddressUtil {
    public static InetSocketAddress getRemoteAddress(AsynchronousSocketChannel channel) {
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

    public static InetSocketAddress getThisAddress(AsynchronousSocketChannel channel) {
        if (channel == null) {
            return null;
        }

        try {
            SocketAddress addr = channel.getLocalAddress();

            if (addr instanceof InetSocketAddress) {
                return (InetSocketAddress) addr;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static InetSocketAddress getThisAddress(AsynchronousServerSocketChannel channel) {
        if (channel == null) {
            return null;
        }

        try {
            SocketAddress addr = channel.getLocalAddress();

            if (addr instanceof InetSocketAddress) {
                return (InetSocketAddress) addr;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
