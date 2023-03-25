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

package io.github.madethoughts.hope.network.handler;

import io.github.madethoughts.hope.VersionedConstants;
import io.github.madethoughts.hope.configuration.ServerConfig;
import io.github.madethoughts.hope.network.Connection;
import io.github.madethoughts.hope.network.NetworkingException;
import io.github.madethoughts.hope.network.packets.clientbound.login.LoginDisconnect;
import io.github.madethoughts.hope.network.packets.clientbound.status.StatusResponse;
import io.github.madethoughts.hope.network.packets.serverbound.handshake.Handshake;

/**
 * This class is responsible for handling the handshake packet.
 */
public class HandshakeHandler implements PacketHandler<Handshake> {
    public static final String LOGIN_KICK_MESSAGE =
            "Your version unsupported, only the version %s is supported. Please update your game!";
    private final Connection connection;
    private final ServerConfig serverConfig;

    public HandshakeHandler(Connection connection, ServerConfig serverConfig) {
        this.connection = connection;
        this.serverConfig = serverConfig;
    }

    @Override
    public void handle(Handshake packet) throws NetworkingException {
        if (packet.protocolNumber() != VersionedConstants.PROTOCOL_VERSION) {
            connection.queuePacket(switch (packet.nextState()) {
                case STATUS -> new StatusResponse(serverConfig, -1); // player count doesn't matter here
                case LOGIN -> new LoginDisconnect(LOGIN_KICK_MESSAGE.formatted(VersionedConstants.VERSION));
                default -> throw new NetworkingException("Unexpected state after handshake");
            });
        }

        connection.state(packet.nextState());
    }
}
