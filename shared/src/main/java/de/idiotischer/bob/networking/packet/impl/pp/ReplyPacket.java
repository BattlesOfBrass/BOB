package de.idiotischer.bob.networking.packet.impl.pp;

import de.craftsblock.craftscore.buffer.BufferUtil;

import java.nio.ByteBuffer;

//unused for now
public class ReplyPacket implements de.idiotischer.bob.networking.packet.Packet {

    private String message = "";
    private Type replyType;

    public ReplyPacket() {
    }

    public ReplyPacket(Type replyType) {
        this.replyType = replyType;
    }

    public ReplyPacket(Type replyType, String message) {
        this.replyType = replyType;
        this.message = message;
    }

    public Type getReplyType() {
        return replyType;
    }

    @Override
    public void write(ByteBuffer buffer) {
        BufferUtil util = BufferUtil.of(buffer);
        util.putUtf(message);
        util.putUtf(replyType.name());
    }

    @Override
    public void read(ByteBuffer buffer) {
        BufferUtil bufferUtil = BufferUtil.of(buffer);

        this.message = bufferUtil.getUtf();
        String name = bufferUtil.getUtf();

        try {
            this.replyType = Type.valueOf(name);
        } catch (IllegalArgumentException e) {
            this.replyType = Type.ERROR;
        }
    }

    public String getMessage() {
        return message;
    }
}