package de.idiotischer.bob.networking.packet.impl;

import de.idiotischer.bob.networking.packet.Packet;

import java.nio.ByteBuffer;
import java.util.Date;

//ig periodically schicken damit man nd immer synced, aber die time so alle 60s nochmal gesynced wird zur sicherheit
public class TimeSyncPacket implements Packet {

    private Date date;

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
}
