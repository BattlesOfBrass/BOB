package de.idiotischer.bob.networking.packet.impl;

import de.craftsblock.cnet.modules.packets.common.networker.Networker;
import de.craftsblock.craftscore.buffer.BufferUtil;
import de.idiotischer.bob.networking.packet.Packet;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.Date;

//ig periodically schicken damit man nd immer synced, aber die time so alle 60s nochmal gesynced wird zur sicherheit
public class TimeSyncPacket implements Packet, de.craftsblock.cnet.modules.packets.common.packet.Packet {

    private Date date;
    public TimeSyncPacket() {}

    public TimeSyncPacket(Date date) {
        this.date = date;
    }

    @Override
    public void write(ByteBuffer buffer) {
        buffer.putLong(date.getTime());
    }

    @Override
    public void read(ByteBuffer buffer) {
        date = new Date(buffer.getLong());
    }

    @Override
    public void handle(Networker networker) {

    }

    @Override
    public void write(@NotNull BufferUtil buffer) {

    }
}
