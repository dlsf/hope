package io.github.madethoughts.hope.network.packets.clientbound.configuration;

import io.github.madethoughts.hope.network.ResizableByteBuffer;
import io.github.madethoughts.hope.network.packets.clientbound.ClientboundPacket;
import io.github.madethoughts.hope.network.packets.serverbound.ServerboundPacket;

public record RegistryData(
        byte[] data
) implements ClientboundPacket {
    @Override
    public void serialize(ResizableByteBuffer buffer) {
        // TODO: implement that right
        buffer.writeArray(data);

    }

    @Override
    public int id() {
        return 0x05;
    }
}
