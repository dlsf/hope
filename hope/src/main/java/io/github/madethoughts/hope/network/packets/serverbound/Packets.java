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
import io.github.madethoughts.hope.network.packets.serverbound.configuration.ClientInformation;
import io.github.madethoughts.hope.network.packets.serverbound.configuration.FinishConfiguration;
import io.github.madethoughts.hope.network.packets.serverbound.configuration.PluginMessage;
import io.github.madethoughts.hope.network.packets.serverbound.handshake.Handshake;
import io.github.madethoughts.hope.network.packets.serverbound.login.EncryptionResponse;
import io.github.madethoughts.hope.network.packets.serverbound.login.LoginAcknowledged;
import io.github.madethoughts.hope.network.packets.serverbound.login.LoginStart;
import io.github.madethoughts.hope.network.packets.serverbound.status.PingRequest;
import io.github.madethoughts.hope.network.packets.serverbound.status.StatusRequest;

/**
 * List of all server bound packets, used to deserialize them.
 */
public enum Packets {
    // Handshake state
    HANDSHAKE(State.HANDSHAKE, 0x0, Handshake.DESERIALIZER),

    // Status state
    STATUS_REQUEST(State.STATUS, 0x0, _ -> new StatusRequest()),
    PING_REQUEST(State.STATUS, 0x1, PingRequest.DESERIALIZER),

    // Login state
    LOGIN_START(State.LOGIN, 0x0, LoginStart.DESERIALIZER),
    ENCRYPTION_RESPONSE(State.LOGIN, 0x1, EncryptionResponse.DESERIALIZER),
    LOGIN_ACKNOWLEDGED(State.LOGIN, 0x03, _ -> new LoginAcknowledged()),

    // Configuration state
    PLUGIN_MESSAGE(State.CONFIGURATION, 0x01, PluginMessage.DESERIALIZER),
    CLIENT_INFORMATION(State.CONFIGURATION, 0x00, ClientInformation.DESERIALIZER),
    FINISH_CONFIGURATION(State.CONFIGURATION, 0x02, _ -> new FinishConfiguration()),

    ;

    // bypass copying array each time
    private static final Packets[] VALUES = values();

    private final State state;
    private final int id;
    private final Deserializer<?> deserializer;

    Packets(State state, int id, Deserializer<?> deserializer) {
        this.state = state;
        this.id = id;
        this.deserializer = deserializer;
    }

    /**
     * Tries to deserialize bytes into a packet by a given state and id.
     *
     * @param state the current protocol state
     * @param id    the packet's id
     * @param data  the packet's data, it should be enough bytes for the packet to get deserialized. No size
     *              checks are made.
     * @return {@link DeserializerResult.PacketDeserialized} if the packet got deserialized successful
     * or {@link DeserializerResult.UnknownPacket} if the packet is unknown
     * @throws ResizableByteBuffer.TypeDeserializationException if some error occurred while deserialization
     */
    public static DeserializerResult tryDeserialize(State state, int id, ResizableByteBuffer data) {
        for (var value : VALUES) {
            if (value.state == state && value.id == id) {
                var packet = value.deserializer.tryDeserialize(data);
                return new DeserializerResult.PacketDeserialized(packet);
            }
        }
        return new DeserializerResult.UnknownPacket(state, id);
    }
}
