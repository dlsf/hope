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

import io.github.madethoughts.hope.network.McCipher;
import io.github.madethoughts.hope.network.packets.Deserializer;
import io.github.madethoughts.hope.network.packets.serverbound.ServerboundPacket;

public record EncryptionResponse(
        byte[] sharedSecretValue,
        byte[] verifyToken
) implements ServerboundPacket.LoginPacket {
    public static final Deserializer<EncryptionResponse> DESERIALIZER = buffer -> new EncryptionResponse(
            buffer.readArray(buffer.readVarInt()),
            buffer.readArray(buffer.readVarInt())
    );

    public byte[] decryptedSharedValue() {
        return McCipher.decryptBytes(McCipher.serverKey.getPrivate(), sharedSecretValue);
    }

    public byte[] decryptedVerifyToken() {
        return McCipher.decryptBytes(McCipher.serverKey.getPrivate(), verifyToken);
    }
}
