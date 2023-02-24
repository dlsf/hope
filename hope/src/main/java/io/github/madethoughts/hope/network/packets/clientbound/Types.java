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

package io.github.madethoughts.hope.network.packets.clientbound;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public class Types {
    public static final int VARINT_MAX_SIZE = 5;

    private static final int VARINT_SEGMENT = 0x7F;
    private static final int VARINT_CONTINUE = 0x80;

    private Types() {}

    public static int readVarInt(ByteBuffer buffer) throws TypeDeserializationException {
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        int value = 0;
        int position = 0;

        byte current;
        do {
            current = buffer.get();
            value |= (current & VARINT_SEGMENT) << position;
            position += 7;

            if (position >= 32) throwSerdeException("VarInt is bigger than 32 bits!");

        } while ((current & VARINT_CONTINUE) != 0);

        buffer.order(ByteOrder.BIG_ENDIAN);
        return value;
    }

    public static void writeVarInt(ByteBuffer buffer, int value) {
        while (true) {
            if ((value & ~VARINT_SEGMENT) == 0) {
                buffer.put((byte) value);
            }

            buffer.put((byte) ((value & VARINT_SEGMENT) | VARINT_CONTINUE));
            value >>>= 7;
        }
    }

    public static int varIntSize(int value) {
        var unsigned = Integer.toUnsignedLong(value);
        if (unsigned <= 0x7F) return 1;
        if (unsigned <= 0x1FFF) return 2;
        if (unsigned <= 0x1FFFFFF) return 3;
        if (unsigned <= 0xFFFFFFF) return 4;
        return 5;
    }

    public static String readUtf8(ByteBuffer buffer) throws TypeDeserializationException {
        int length = readVarInt(buffer);
        var bytes = new byte[length];
        buffer.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static void writeUtf8(ByteBuffer buffer, byte[] value) {
        writeVarInt(buffer, value.length);
        buffer.put(value);
    }

    public static int utf8Size(byte[] value) {
        return varIntSize(value.length) + value.length;
    }

    public static int readUShort(ByteBuffer buffer) {
        return Short.toUnsignedInt(buffer.getShort());
    }

    public static long readLong(ByteBuffer buffer) {
        return buffer.getLong();
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
