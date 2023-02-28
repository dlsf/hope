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

public enum State {
    HANDSHAKE,
    STATUS,
    LOGIN,
    PLAY;

    public static State deserialize(ResizableByteBuffer buffer, State... permitted) {
        final var errorMsg = "Unexpected state";

        var state = (State) switch (buffer.readVarInt()) {
            case 0 -> HANDSHAKE;
            case 1 -> STATUS;
            case 2 -> LOGIN;

            default -> ResizableByteBuffer.throwSerdeException(errorMsg);
        };
        for (var current : permitted) {
            if (current == state) return state;
        }
        return ResizableByteBuffer.throwSerdeException(errorMsg);
    }
}
