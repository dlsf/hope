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

package io.github.madethoughts.hope.network.packets.serverbound;

import io.github.madethoughts.hope.network.ResizableByteBuffer;
import io.github.madethoughts.hope.network.State;
import io.github.madethoughts.hope.network.packets.DeserializerResult;
import io.github.madethoughts.hope.network.packets.Packets;
import io.github.madethoughts.hope.network.packets.serverbound.handshake.Handshake;
import io.github.madethoughts.hope.network.packets.serverbound.status.PingRequest;
import io.github.madethoughts.hope.network.packets.serverbound.status.StatusRequest;

public sealed interface ServerboundPacket
        permits ServerboundPacket.HandshakePacket, ServerboundPacket.StatusPacket {

    DeserializerResult.MoreBytesNeeded NEED_SOME_BYTES = new DeserializerResult.MoreBytesNeeded(1);

    /**
     Tries to deserialize a packet from bytes.
     When the result is {@link DeserializerResult.PacketDeserialized}
     the serialization was successful.

     @param state  the client's current state
     @param buffer the buffer holding the bytes
     @return the result of the deserialization
     */
    static DeserializerResult tryDeserialize(State state, ResizableByteBuffer buffer) {
        try {
            var lengthOpt = buffer.tryReadVarInt();
            if (lengthOpt.isEmpty()) {
                return NEED_SOME_BYTES;
            }

            var length = lengthOpt.getAsInt();
            if (buffer.remaining() < length) {
                return new DeserializerResult.MoreBytesNeeded(length);
            }

            var id = buffer.readVarInt();
            var deserialized = Packets.tryDeserialize(state, id, buffer);
            buffer.compact();
            return deserialized;
        } catch (ResizableByteBuffer.TypeDeserializationException e) {
            return new DeserializerResult.Failed(e.getMessage());
        }
    }

    //    sealed interface LoginPacket extends ServerboundPacket {}
    //
    //    sealed interface PlayPacket extends ServerboundPacket {}

    sealed interface HandshakePacket extends ServerboundPacket permits Handshake {}

    sealed interface StatusPacket extends ServerboundPacket permits PingRequest, StatusRequest {}
}
