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

package io.github.madethoughts.hope.network.packets.serverbound.login;

import io.github.madethoughts.hope.network.packets.serverbound.Deserializer;
import io.github.madethoughts.hope.network.packets.serverbound.ServerboundPacket;

import java.util.UUID;

public record LoginStart(
        String playerName,
        UUID uuid
) implements ServerboundPacket.LoginPacket {
    public static final Deserializer<LoginStart> DESERIALIZER = buffer -> new LoginStart(
            buffer.readString(16),
            buffer.readUUID()
    );
}
