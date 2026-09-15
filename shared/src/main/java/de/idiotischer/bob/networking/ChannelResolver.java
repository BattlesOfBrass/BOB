package de.idiotischer.bob.networking;

import java.nio.channels.AsynchronousSocketChannel;
import java.util.Set;

public interface ChannelResolver {
    Set<AsynchronousSocketChannel> channels();
}
