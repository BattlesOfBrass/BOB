package de.idiotischer.bob.networking.packet.impl;

import de.craftsblock.cnet.modules.packets.common.networker.Networker;
import de.craftsblock.cnet.modules.packets.common.packet.Packet;
import de.craftsblock.craftscore.buffer.BufferUtil;
import de.idiotischer.bob.scenario.Scenario;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ScenariosSyncPacket implements Packet, de.idiotischer.bob.networking.packet.Packet {

    private final List<ScenarioSyncPacket> scenarios = new ArrayList<>();

    public ScenariosSyncPacket() {}

    public ScenariosSyncPacket(List<ScenarioSyncPacket> scenarios) {
        this.scenarios.addAll(scenarios);
    }

    public static ScenariosSyncPacket fromScenarios(Set<Scenario> scenarios) {
        ScenariosSyncPacket packet = new ScenariosSyncPacket();

        for (Scenario scenario : scenarios) {
            packet.scenarios.add(new ScenarioSyncPacket(scenario));
        }

        return packet;
    }

    public static @NotNull ScenariosSyncPacket fromScenarios(@NotNull List<Scenario> scenarios) {
        ScenariosSyncPacket packet = new ScenariosSyncPacket();

        for (Scenario scenario : scenarios) {
            packet.scenarios.add(new ScenarioSyncPacket(scenario));
        }

        return packet;
    }

    public List<ScenarioSyncPacket> getScenarioPackets() {
        return scenarios;
    }

    //public List<Scenario> getScenarios() {
    //    return scenarios.stream().map(s -> s.);
    //}

    @Override
    public void write(BufferUtil buffer) {
        buffer.getRaw().putInt(scenarios.size());

        for (ScenarioSyncPacket scenarioPacket : scenarios) {
            scenarioPacket.write(buffer);
        }
    }

    @Override
    public void write(ByteBuffer buffer) {
        buffer.putInt(scenarios.size());

        for (ScenarioSyncPacket scenarioPacket : scenarios) {
            scenarioPacket.write(buffer);
        }
    }

    @Override
    public void read(ByteBuffer buffer) {
        scenarios.clear();

        int size = buffer.getInt();
        for (int i = 0; i < size; i++) {
            ScenarioSyncPacket packet = new ScenarioSyncPacket();
            packet.read(buffer);
            scenarios.add(packet);
        }
    }

    @Override
    public void handle(Networker networker) {

    }
}