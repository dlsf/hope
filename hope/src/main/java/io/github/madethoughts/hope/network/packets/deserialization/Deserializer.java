package io.github.madethoughts.hope.network.packets.deserialization;

import java.nio.ByteBuffer;

/**
 Used to deserialize packets
 */
public class Deserializer {

    /**
     Tries to deserialize a packet from bytes.
     When the result is {@link DeserializerResult.PacketDeserialized}

     @param buffer the buffer holding the bytes
     @return the result of the deserialization
     */
    public DeserializerResult tryDeserialize(ByteBuffer buffer) {
        return new DeserializerResult.MoreBytesNeeded(10);
    }
}
