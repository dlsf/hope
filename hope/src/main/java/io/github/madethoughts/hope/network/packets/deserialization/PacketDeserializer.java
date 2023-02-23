/*
 *     Hope - A minecraft server reimplementation
 *     Copyright (C) 2023 Nick Hensel and contributors
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.madethoughts.hope.network.packets.deserialization;

import io.github.madethoughts.hope.network.State;
import io.github.madethoughts.hope.network.packets.Packets;

import java.nio.ByteBuffer;

/**
 Used to deserialize packets
 */
public class PacketDeserializer {

    /**
     Tries to deserialize a packet from bytes.
     When the result is {@link DeserializerResult.PacketDeserialized}
     the serialization was successful.

     @param state  the client's current state
     @param buffer the buffer holding the bytes
     @return the result of the deserialization
     */
    public DeserializerResult tryDeserialize(State state, ByteBuffer buffer) {
        var oldPos = buffer.position();
        try {
            buffer.position(0);
            var length = Types.varInt(buffer);

            var totalSize = buffer.position() + length;
            if (oldPos - buffer.position() < length) {
                buffer.position(oldPos);
                return new DeserializerResult.MoreBytesNeeded(length);
            }

            var id = Types.varInt(buffer);
            var deserialized = Packets.deserialize(state, id, buffer);
            buffer.compact();
            buffer.position(oldPos - totalSize);
            return deserialized;
        } catch (Types.TypeDeserializationException e) {
            return new DeserializerResult.Failed(e.getMessage());
        }
    }
}
