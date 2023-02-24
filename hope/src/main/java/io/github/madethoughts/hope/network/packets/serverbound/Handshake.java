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

import io.github.madethoughts.hope.network.State;
import io.github.madethoughts.hope.network.packets.Deserializer;
import io.github.madethoughts.hope.network.packets.DeserializerResult;
import io.github.madethoughts.hope.network.packets.clientbound.Types;

public record Handshake(
        int protocolNumber,
        String serverAddress,
        int serverPort,
        State nextState
) implements ServerboundPacket.HandshakePacket {

    public static final Deserializer DESERIALIZER = buffer -> {
        try {
            var packet = new Handshake(
                    Types.readVarInt(buffer),
                    Types.readUtf8(buffer),
                    Types.readUShort(buffer),
                    State.deserialize(buffer, State.STATUS, State.LOGIN)
            );
            return new DeserializerResult.PacketDeserialized(packet);
        } catch (Types.TypeDeserializationException e) {
            return new DeserializerResult.Failed(e.getMessage());
        }
    };
}
