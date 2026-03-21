package de.idiotischer.bob.networking.packet.codec;

public interface Coder<T, R> {
    T code(R r);
}
