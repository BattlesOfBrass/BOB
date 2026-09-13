package de.idiotischer.bob.networking.packet.impl.pp;

import de.craftsblock.cnet.modules.packets.common.networker.Networker;
import de.craftsblock.cnet.modules.packets.common.packet.Packet;
import de.craftsblock.craftscore.buffer.BufferUtil;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

public class RequestPacket implements Packet, de.idiotischer.bob.networking.packet.Packet {

    private String message = "";
    private Type requestType;

    public RequestPacket() {
    }

    public RequestPacket(Type requestType) {
        this.requestType = requestType;
    }

    public RequestPacket(Type requestType, String message) {
        this.requestType = requestType;
        this.message = message;
    }

    public Type getRequestType() {
        return requestType;
    }

    @Override
    public void handle(Networker networker) {
    }

    @Override
    public void write(@NotNull BufferUtil buffer) {
        buffer.putUtf(requestType.name());
    }

    @Override
    public void write(ByteBuffer buffer) {
        BufferUtil util = BufferUtil.of(buffer);
        util.putUtf(message);
        util.putUtf(requestType.name());
    }

    @Override
    public void read(ByteBuffer buffer) {
        BufferUtil bufferUtil = BufferUtil.of(buffer);

        this.message = bufferUtil.getUtf();
        String name = bufferUtil.getUtf();

        try {
            this.requestType = Type.valueOf(name);
        } catch (IllegalArgumentException e) {
            this.requestType = Type.ERROR;
        }
    }

    public String getMessage() {
        return message;
    }
}