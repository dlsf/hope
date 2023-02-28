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

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.OptionalInt;

public class ResizableByteBuffer {
    public static final int START_CAPACITY = 1028;
    // 2 mebibyte
    public static final int MAX_CAPACITY = 2097152;
    public static final Charset CHARSET = StandardCharsets.UTF_8;
    private static final int VARINT_SEGMENT = 0x7F;
    private static final int VARINT_CONTINUE = 0x80;
    private ByteBuffer buffer = ByteBuffer.allocateDirect(START_CAPACITY);

    // we have to write the length varints in ByteBuffers direct, so this is a util here
    public static void writeVarInt(ByteBuffer buffer, int value) {
        while (true) {
            if ((value & ~VARINT_SEGMENT) == 0) {
                buffer.put((byte) value);
                return;
            }

            buffer.put((byte) ((value & VARINT_SEGMENT) | VARINT_CONTINUE));
            value >>>= 7;
        }
    }

    public static <T> T throwSerdeException(String message) {
        throw new TypeDeserializationException(message);
    }

    public void grow() {
        ensureCapacity(buffer.capacity() * 2);
    }

    public void ensureCapacity(int size) {
        // we don't have to do anything
        if (buffer.capacity() > size) return;

        if (size < 1) throw new IllegalArgumentException("Size must be positive.");
        var growSize = Integer.highestOneBit(size) * 2;
        if (growSize < 0 || growSize > MAX_CAPACITY) throw new IllegalArgumentException("Size is too big.");
        buffer = ByteBuffer.allocateDirect(growSize)
                           .put(buffer.flip());
    }

    public void writeOp(Runnable runnable) {
        while (true) {
            buffer.mark();
            try {
                runnable.run();
                return;
            } catch (BufferOverflowException e) {
                buffer.reset();
                grow();
            }
        }
    }

    public int readVarInt() {
        int value = 0;
        int position = 0;
        byte current;
        do {
            current = buffer.get();
            value |= (current & VARINT_SEGMENT) << position;

            if (position >= 32) throwSerdeException("VarInt is bigger than 32 bits!");
            position += 7;

        } while ((current & VARINT_CONTINUE) != 0);

        return value;
    }

    public OptionalInt tryReadVarInt() {
        buffer.mark();
        try {
            return OptionalInt.of(readVarInt());
        } catch (BufferUnderflowException e) {
            buffer.reset();
            return OptionalInt.empty();
        }
    }

    public void writeArray(byte[] bytes) {
        writeOp(() -> buffer.put(bytes));
    }

    public byte[] readArray(int size) {
        var bytes = new byte[size];
        buffer.get(bytes);
        return bytes;
    }

    public String readString() {
        var size = readVarInt();
        var bytes = readArray(size);
        return new String(bytes, CHARSET);
    }

    public void writeString(String val) {
        var bytes = val.getBytes(CHARSET);
        writeVarInt(bytes.length);
        writeArray(bytes);
    }

    public void writeVarInt(int val) {
        writeOp(() -> writeVarInt(buffer, val));
    }

    public int readUShort() {
        return Short.toUnsignedInt(buffer.getShort());
    }

    public long readLong() {
        return buffer.getLong();
    }

    public void writeLong(long val) {
        writeOp(() -> buffer.putLong(val));
    }

    public int position() {
        return buffer.position();
    }

    public void position(int pos) {
        buffer.position(pos);
    }

    public int remaining() {
        return buffer.remaining();
    }

    public ByteBuffer nioBuffer() {
        return buffer;
    }

    public void flip() {
        buffer.flip();
    }

    public void clear() {
        buffer.clear();
    }

    public void compact() {
        buffer.compact();
    }

    public int limit() {
        return buffer.limit();
    }

    public static final class TypeDeserializationException extends RuntimeException {
        private TypeDeserializationException(String message) {
            super(message);
        }
    }

}
