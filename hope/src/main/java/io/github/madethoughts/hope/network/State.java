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

package io.github.madethoughts.hope.network;

import io.github.madethoughts.hope.network.packets.deserialization.Types;

import java.nio.ByteBuffer;

public enum State {
    HANDSHAKE,
    STATUS,
    LOGIN,
    PLAY;

    public static State deserialize(ByteBuffer buffer, State... permitted) throws Types.TypeDeserializationException {
        final var errorMsg = "Unexpected state";

        var state = (State) switch (Types.varInt(buffer)) {
            case 0 -> HANDSHAKE;
            case 1 -> STATUS;
            case 2 -> LOGIN;

            default -> Types.throwSerdeException(errorMsg);
        };
        for (var current : permitted) {
            if (current == state) return state;
        }
        return Types.throwSerdeException(errorMsg);
    }
}
