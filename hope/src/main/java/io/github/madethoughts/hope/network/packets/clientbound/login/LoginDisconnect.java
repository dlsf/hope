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

package io.github.madethoughts.hope.network.packets.clientbound.login;

import io.github.madethoughts.hope.Server;
import io.github.madethoughts.hope.network.ResizableByteBuffer;
import io.github.madethoughts.hope.network.packets.clientbound.ClientboundPacket;
import net.kyori.adventure.text.Component;

public record LoginDisconnect(Component reason) implements ClientboundPacket {
    @Override
    public void serialize(ResizableByteBuffer buffer) {
        buffer.writeString(Server.GSON.toJson(reason));
    }

    @Override
    public int id() {
        return 0;
    }
}
