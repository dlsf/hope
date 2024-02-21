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
import io.github.madethoughts.hope.network.packets.clientbound.status.PingResponse;
import io.github.madethoughts.hope.network.packets.clientbound.status.StatusResponse;
import io.github.madethoughts.hope.network.packets.serverbound.ServerboundPacket;
import io.github.madethoughts.hope.network.packets.serverbound.status.PingRequest;
import io.github.madethoughts.hope.network.packets.serverbound.status.StatusRequest;

/**
 * This class is responsible for handling status packets, including sending ping and status responses.
 */
public class StatusHandler implements PacketHandler<ServerboundPacket.StatusPacket> {
    private final Connection connection;
    private final ServerConfig serverConfig;

    public StatusHandler(Connection connection, ServerConfig serverConfig) {
        this.connection = connection;
        this.serverConfig = serverConfig;
    }

    @Override
    public void handle(ServerboundPacket.StatusPacket packet) throws NetworkingException {
        connection.queuePacket(switch (packet) {
            // TODO: 3/26/23 previewChat, enforcesSecureChat, online players
            case StatusRequest _ -> new StatusResponse(
                    new StatusResponse.Version(VersionedConstants.VERSION, VersionedConstants.PROTOCOL_VERSION),
                    new StatusResponse.Players(serverConfig.maxPlayers(), -1), // values doesn't matter for now
                    serverConfig.motd(), serverConfig.favicon(), false, false
            );
            case PingRequest(var payload) -> new PingResponse(payload);
        });
    }
}
