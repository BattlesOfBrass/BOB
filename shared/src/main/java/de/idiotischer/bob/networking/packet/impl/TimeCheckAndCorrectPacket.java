package de.idiotischer.bob.networking.packet.impl;

import de.idiotischer.bob.networking.packet.Packet;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Date;

public class TimeCheckAndCorrectPacket implements Packet {
    private Date startDate;
    private long timeTicks;
    private int speed;

    public TimeCheckAndCorrectPacket() {}

    public TimeCheckAndCorrectPacket(long timeTicks, int speed) {
        this(timeTicks, speed, null);
    }

    public TimeCheckAndCorrectPacket(long timeTicks, int speed, Date startDate) {
        this.timeTicks = timeTicks;
        this.speed = speed;
        this.startDate = startDate;
    }

    @Override
    public void write(ByteBuffer buffer) {
        buffer.putLong(startDate != null ? startDate.getTime() : -1L);
        buffer.putLong(timeTicks);
        buffer.putInt(speed);
    }

    @Override
    public void read(ByteBuffer buffer) {
        long timestamp = buffer.getLong();
        startDate = timestamp == -1L ? null : new Date(timestamp);

        timeTicks = buffer.getLong();
        speed = buffer.getInt();
    }

    public long getTimeTicks() {
        return timeTicks;
    }

    public int getSpeed() {
        return speed;
    }

    public Date getStartDate() {
        return startDate;
    }
}