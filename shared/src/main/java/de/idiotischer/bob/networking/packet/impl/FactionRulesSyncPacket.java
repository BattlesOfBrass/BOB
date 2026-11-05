package de.idiotischer.bob.networking.packet.impl;

import de.idiotischer.bob.faction.Faction;
import de.idiotischer.bob.networking.packet.Packet;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class FactionRulesSyncPacket implements Packet {

    private List<FactionRuleSyncPacket> packets = new ArrayList<>();

    public FactionRulesSyncPacket() {}

    public FactionRulesSyncPacket(List<FactionRuleSyncPacket> packets) {
        this.packets = packets;
    }

    public static FactionRulesSyncPacket fromFactionRules(Set<Faction.FactionRule> factions) {
        return new FactionRulesSyncPacket(factions.stream().map(FactionRuleSyncPacket::new).toList());
    }

    public static FactionRulesSyncPacket fromFactionRules(List<Faction.FactionRule> factions) {
        return new FactionRulesSyncPacket(factions.stream().map(FactionRuleSyncPacket::new).toList());
    }

    @Override
    public void write(ByteBuffer buffer) {
        buffer.putInt(packets.size());

        packets.forEach(packet -> packet.write(buffer));
    }

    @Override
    public void read(ByteBuffer buffer) {
        packets.clear();

        int size = buffer.getInt();
        for (int i = 0; i < size; i++) {
            FactionRuleSyncPacket packet = new FactionRuleSyncPacket();
            packet.read(buffer);
            packets.add(packet);
        }
    }

    public List<FactionRuleSyncPacket> getPackets() {
        return packets;
    }
}
