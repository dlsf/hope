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

import io.github.madethoughts.hope.network.Connection;
import io.github.madethoughts.hope.network.NetworkingException;
import io.github.madethoughts.hope.network.packets.clientbound.status.PingResponse;
import io.github.madethoughts.hope.network.packets.clientbound.status.StatusResponse;
import io.github.madethoughts.hope.network.packets.serverbound.ServerboundPacket;
import io.github.madethoughts.hope.network.packets.serverbound.status.PingRequest;
import io.github.madethoughts.hope.network.packets.serverbound.status.StatusRequest;

public class StatusHandler implements PacketHandler<ServerboundPacket.StatusPacket> {
    private final Connection connection;

    public StatusHandler(Connection connection) {this.connection = connection;}

    @Override
    public void handle(ServerboundPacket.StatusPacket packet) throws NetworkingException {
        connection.queuePacket(switch (packet) {
            // TODO: 2/24/23 add real values and config here
            case StatusRequest() -> new StatusResponse(
                    new StatusResponse.Version("1.19.3", 761),
                    new StatusResponse.Players(
                            10,
                            0
                    ),
                    "test hahah",
                    new byte[0],
                    false,
                    false
            );
            case PingRequest(var payload) -> new PingResponse(payload);
        });
    }
}
