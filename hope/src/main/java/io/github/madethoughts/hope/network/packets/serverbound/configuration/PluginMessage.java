package io.github.madethoughts.hope.network.packets.serverbound.configuration;

import io.github.madethoughts.hope.network.packets.serverbound.Deserializer;
import io.github.madethoughts.hope.network.packets.serverbound.ServerboundPacket;

public record PluginMessage(
        String identifier,
        byte[] data
) implements ServerboundPacket.ConfigurationPacket {
    public static final Deserializer<PluginMessage> DESERIALIZER = buffer -> new PluginMessage(
            buffer.readString(32767),
            buffer.readArray(buffer.remaining())
    );
}
