package de.idiotischer.bob.networking.packet.impl;

import de.craftsblock.craftscore.buffer.BufferUtil;
import de.idiotischer.bob.focus.Focus;
import de.idiotischer.bob.focus.FocusTree;
import de.idiotischer.bob.modifiers.ModifierResolver;
import de.idiotischer.bob.networking.packet.Packet;

import java.nio.ByteBuffer;

public class FocusTreeSyncPacket implements Packet {

    private FocusTree tree;

    private String data;

    public FocusTreeSyncPacket() {}

    public FocusTreeSyncPacket(FocusTree tree) {
        this.tree = tree;
    }

    @Override
    public void write(ByteBuffer buffer) {
        BufferUtil.of(buffer).putUtf(tree.toDataString());
    }

    @Override
    public void read(ByteBuffer buffer) {
        data = BufferUtil.of(buffer).getUtf();
    }

    public FocusTree getTree(ModifierResolver r) {
        return FocusTree.fromDataString(data, r);
    }
}
