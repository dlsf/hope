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

import io.github.madethoughts.hope.network.ResizableByteBuffer;
import io.github.madethoughts.hope.network.packets.clientbound.ClientboundPacket;

public record EncryptionRequest(
        byte[] serverPublicKey,
        byte[] verifyToken
) implements ClientboundPacket {

    public static final String SERVER_ID = "";

    @Override
    public void serialize(ResizableByteBuffer buffer) {
        buffer.writeString(SERVER_ID); // server id, should be empty
        buffer.writeVarInt(serverPublicKey.length);
        buffer.writeArray(serverPublicKey);
        buffer.writeVarInt(verifyToken.length);
        buffer.writeArray(verifyToken);
    }

    @Override
    public int id() {
        return 1;
    }
}
