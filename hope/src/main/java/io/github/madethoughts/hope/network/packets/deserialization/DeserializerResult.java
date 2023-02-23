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
import io.github.madethoughts.hope.network.packets.serverbound.ServerboundPacket;

import java.nio.ByteBuffer;

/**
 Results of the {@link PacketDeserializer#tryDeserialize(State, ByteBuffer)} method
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
    record PacketDeserialized(ServerboundPacket packet) implements DeserializerResult {}

    /**
     @param id the if of the unknown packet
     */
    record UnknownPacket(State state, int id) implements DeserializerResult {}

    /**
     @param reason why the deserialization failed
     */
    record Failed(String reason) implements DeserializerResult {}
}
