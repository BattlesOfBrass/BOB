package de.idiotischer.bob.networking.packet.impl.pp;

import de.craftsblock.craftscore.buffer.BufferUtil;

import java.nio.ByteBuffer;

//unused for now
public class ReplyPacket implements de.idiotischer.bob.networking.packet.Packet {

    private String message = "";
    private Type requestType;

    public ReplyPacket() {
    }

    public ReplyPacket(Type requestType) {
        this.requestType = requestType;
    }

    public ReplyPacket(Type requestType, String message) {
        this.requestType = requestType;
        this.message = message;
    }

    public Type getRequestType() {
        return requestType;
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