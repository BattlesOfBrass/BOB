package de.idiotischer.bob.networking.packet.codec;

import java.nio.channels.AsynchronousChannel;
import java.nio.channels.AsynchronousSocketChannel;

public interface Coder<T, R> {
    T code(R r, AsynchronousSocketChannel c);
}
