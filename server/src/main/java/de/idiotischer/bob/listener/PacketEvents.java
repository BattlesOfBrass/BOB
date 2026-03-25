package de.idiotischer.bob.listener;

import de.craftsblock.craftscore.event.EventHandler;
import de.craftsblock.craftscore.event.ListenerAdapter;
import de.idiotischer.bob.networking.packet.PacketRegistry;
import de.idiotischer.bob.networking.packet.impl.PingPacket;

public class PacketEvents implements ListenerAdapter {

    @EventHandler
    public void onPacketReceive(PacketRegistry.PacketReceiveEvent event) {
        if(event.getPacket() instanceof PingPacket) {
            System.out.println("Ping packet received");
        }
    }

}

