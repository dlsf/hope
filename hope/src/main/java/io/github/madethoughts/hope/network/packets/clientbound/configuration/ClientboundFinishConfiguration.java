package io.github.madethoughts.hope.network.packets.clientbound.configuration;

import io.github.madethoughts.hope.network.ResizableByteBuffer;
import io.github.madethoughts.hope.network.packets.clientbound.ClientboundPacket;

public record ClientboundFinishConfiguration() implements ClientboundPacket {
    @Override
    public void serialize(ResizableByteBuffer buffer) {

    }

    @Override
    public int id() {
        return 0x02;
    }
}
