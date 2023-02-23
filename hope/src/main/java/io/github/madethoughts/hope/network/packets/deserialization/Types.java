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

package io.github.madethoughts.hope.network.packets.deserialization;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public class Types {
    private Types() {}

    public static int varInt(ByteBuffer buffer) throws TypeDeserializationException {
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        int value = 0;
        int position = 0;

        byte current;
        do {
            current = buffer.get();
            value |= (current & 0x7F) << position;
            position += 7;

            if (position >= 32) throwSerdeException("VarInt is bigger than 32 bits!");

        } while ((current & 0x80) != 0);

        buffer.order(ByteOrder.BIG_ENDIAN);
        return value;
    }

    public static String string(ByteBuffer buffer) throws TypeDeserializationException {
        int length = varInt(buffer);
        var bytes = new byte[length];
        buffer.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static int unsignedShort(ByteBuffer buffer) {
        return Short.toUnsignedInt(buffer.getShort());
    }

    public static <T> T throwSerdeException(String message) throws TypeDeserializationException {
        throw new TypeDeserializationException(message);
    }

    public static class TypeDeserializationException extends Exception {
        private TypeDeserializationException(String message) {
            super(message);
        }
    }
}
