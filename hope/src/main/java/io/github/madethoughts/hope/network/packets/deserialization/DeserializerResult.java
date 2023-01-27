package io.github.madethoughts.hope.network.packets.deserialization;

import io.github.madethoughts.hope.network.packets.Packet;

import java.nio.ByteBuffer;

/**
 Results of the {@link Deserializer#tryDeserialize(ByteBuffer)} method
 */
// all value classes
public sealed interface DeserializerResult {
    /**
     @param neededSize the amount of bytes this packet needs, including the header's length field
     */
    record MoreBytesNeeded(int neededSize) implements DeserializerResult {}

    /**
     @param packet the deserialized packet
     */
    record PacketDeserialized(Packet packet) implements DeserializerResult {}

    /**
     @param id the if of the unknown packet
     */
    record UnknownPacket(int id) implements DeserializerResult {}
}
