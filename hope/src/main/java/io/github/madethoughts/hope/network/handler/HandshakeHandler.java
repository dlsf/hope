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
import io.github.madethoughts.hope.network.packets.serverbound.handshake.Handshake;

public class HandshakeHandler implements PacketHandler<Handshake> {
    private final Connection connection;

    public HandshakeHandler(Connection connection) {this.connection = connection;}

    @Override
    public void handle(Handshake packet) {
        // TODO: 3/10/23 protocol version validation
        connection.state(packet.nextState());
    }
}
