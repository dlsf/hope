package io.github.madethoughts.hope.network.packets.serverbound.configuration;

import io.github.madethoughts.hope.network.packets.serverbound.Deserializer;
import io.github.madethoughts.hope.network.packets.serverbound.ServerboundPacket;

import java.util.Locale;

public record ClientInformation() implements ServerboundPacket.ConfigurationPacket {
    public static final Deserializer<ClientInformation> DESERIALIZER = buffer -> {
        var locale = buffer.readString(16);
        var viewDistance = buffer.readByte();
        int chatMode = buffer.readVarInt();
        boolean chatColors = buffer.readBoolean();
        byte displayedSkinParts = buffer.readByte();
        int mainHand = buffer.readVarInt();
        boolean enableTextFiltering = buffer.readBoolean();
        boolean allowServerListing = buffer.readBoolean();
        return new ClientInformation();
    };
}
