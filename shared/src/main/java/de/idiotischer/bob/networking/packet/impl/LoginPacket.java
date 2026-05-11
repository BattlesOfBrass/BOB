package de.idiotischer.bob.networking.packet.impl;

import de.craftsblock.craftscore.buffer.BufferUtil;
import de.idiotischer.bob.auth.Credentials;
import de.idiotischer.bob.networking.packet.Packet;

import java.nio.ByteBuffer;

public class LoginPacket implements Packet {

    private Credentials credentials;

    public LoginPacket() {}

    public LoginPacket(Credentials credentials) {
        this.credentials = credentials;
    }

    @Override
    public void write(ByteBuffer buffer) {
        BufferUtil util = BufferUtil.of(buffer);
        util.putUtf(credentials.username());
        util.putUtf(credentials.lobbyPW());
    }

    @Override
    public void read(ByteBuffer buffer) {
        BufferUtil util = BufferUtil.of(buffer);

        String username = util.getUtf();
        String lobbyPW = util.getUtf();

        credentials = new Credentials(username, lobbyPW);
    }

    public Credentials getCredentials() {
        return credentials;
    }
}
