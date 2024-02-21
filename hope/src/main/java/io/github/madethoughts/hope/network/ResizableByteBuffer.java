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
import java.util.UUID;

/**
 * A thread unsafe ByteBuffer that resizes its underlying nio ByteBuffer if needed.
 * The capacity will always be a power of 2.
 * Note: This implementation isn't really tested, use it at your own risk.
 * All data types are implemented according to the minecraft protocol.
 */
public final class ResizableByteBuffer {
    public static final int START_CAPACITY = 1028;
    // 2 mebibyte
    public static final int MAX_CAPACITY = 2097152;
    public static final Charset CHARSET = StandardCharsets.UTF_8;
    private static final int VARINT_SEGMENT = 0x7F;
    private static final int VARINT_CONTINUE = 0x80;
    private ByteBuffer buffer;

    private ResizableByteBuffer(ByteBuffer start) {
        this.buffer = start;
    }

    /**
     * @return a new ResizeableByteBuffer that uses nio direct bytebuffers
     */
    public static ResizableByteBuffer allocateDirect() {
        return new ResizableByteBuffer(ByteBuffer.allocateDirect(START_CAPACITY));
    }

    // we have to write the length varints in ByteBuffers direct, so this is a util here

    /**
     * Writes a varint to a bytebuffer.
     *
     * @param buffer the bytebuffer to be written
     * @param value  the varint value
     */
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

    /**
     * Util method to throw a {@link TypeDeserializationException}
     *
     * @param message the message/reason of the exception
     * @param <T>     just some generic, so it simulates to return something (will never, if always throws)
     * @return the exception
     */
    public static <T> T throwSerdeException(String message) {
        throw new TypeDeserializationException(message);
    }

    /**
     * grows the buffer by doubling the capacity
     */
    private void grow() {
        ensureCapacity(buffer.capacity() * 2);
    }

    /**
     * Ensures that the buffer has the given size as a minimum.
     *
     * @param size the needed buffer size
     */
    public void ensureCapacity(int size) {
        // we don't have to do anything
        if (buffer.capacity() > size) return;

        if (size < 1) throw new IllegalArgumentException("Size must be positive.");
        var growSize = Integer.highestOneBit(size) * 2;
        if (growSize < 0 || growSize > MAX_CAPACITY) throw new IllegalArgumentException("Size is too big.");
        buffer = (buffer.isDirect() ? ByteBuffer.allocateDirect(growSize) : ByteBuffer.allocate(growSize))
                .put(buffer.flip());
    }

    /**
     * Tries to write something to the buffer if a {@link BufferOverflowException} occurs, the buffer get resized
     * and the operation repeats.
     * Note that this method must be never call itself.
     *
     * @param runnable the task
     */
    private void writeOp(Runnable runnable) {
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

    /**
     * Writes a varint to this buffer.
     *
     * @param val the int value to be written
     * @see ResizableByteBuffer#writeVarInt(ByteBuffer, int)
     */
    public void writeVarInt(int val) {
        writeOp(() -> writeVarInt(buffer, val));
    }

    /**
     * Reads a varint from this buffer.
     *
     * @return the int value of this varint
     * @throws BufferUnderflowException     if there are not enough bytes for a varint
     * @throws TypeDeserializationException if the int is bigger than 32 bits
     */
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

    /**
     * Tries to read a varint from this buffer.
     * If it fails, the buffer will be reset to the previous state.
     *
     * @return an empty optional if no int could be read
     */
    public OptionalInt tryReadVarInt() {
        buffer.mark();
        try {
            return OptionalInt.of(readVarInt());
        } catch (BufferUnderflowException e) {
            buffer.reset();
            return OptionalInt.empty();
        }
    }

    /**
     * Writes a byte array to the buffer
     *
     * @param bytes the bytes to be written
     */
    public void writeArray(byte[] bytes) {
        writeOp(() -> buffer.put(bytes));
    }

    /**
     * Reads a byte array from this buffer
     *
     * @param size the size of the byte array
     * @return the read byte array
     * @throws BufferUnderflowException if there aren't enough bytes
     */
    public byte[] readArray(int size) {
        var bytes = new byte[size];
        buffer.get(bytes);
        return bytes;
    }

    /**
     * Writes a length prefixed string to this buffer
     *
     * @param val the string to be written
     * @see ResizableByteBuffer#writeVarInt(int)
     * @see ResizableByteBuffer#writeArray(byte[])
     */
    public void writeString(String val) {
        var bytes = val.getBytes(CHARSET);
        writeVarInt(bytes.length);
        writeArray(bytes);
    }

    /**
     * Reads a length prefixes string from this buffer
     *
     * @param maxSize the maximum amount of characters of this string. Must be less than 32767 and will only be
     *                checked roughly: prefixLength > (maxsize * 4) -> error
     * @return the read string
     * @throws BufferUnderflowException     if there are not enough bytes
     * @throws TypeDeserializationException if the string is too big or the varint read failed
     * @see ResizableByteBuffer#readVarInt()
     */
    public String readString(int maxSize) {
        var size = readVarInt();
        if (size > maxSize * 4 || maxSize > 32767) throwSerdeException("String is too big");
        var bytes = readArray(size);
        return new String(bytes, CHARSET);
    }

    /**
     * Reads an unsiged short from this buffer, represented as an int
     *
     * @return the integer value of this short
     * @throws BufferUnderflowException if there aren't enough bytes
     */
    public int readUShort() {
        return Short.toUnsignedInt(buffer.getShort());
    }

    /**
     * Reads a long from this buffer
     *
     * @return the long
     * @throws BufferUnderflowException if there aren't enough bytes in this buffer
     */
    public long readLong() {
        return buffer.getLong();
    }

    /**
     * Writes a long to this buffer
     *
     * @param val the long
     */

    public void writeLong(long val) {
        writeOp(() -> buffer.putLong(val));
    }

    /**
     * Reads an on byte boolean value from this buffer. The boolean is true if the byte unequal zero
     *
     * @return the read boolean
     */
    public boolean readBoolean() {
        return buffer.get() != 0;
    }

    /**
     * writes an uuid to this byte buffer using two longs
     *
     * @param uuid the uuid to be written
     * @see ResizableByteBuffer#writeLong(long)
     */

    public void writeUUID(UUID uuid) {
        writeLong(uuid.getLeastSignificantBits());
        writeLong(uuid.getMostSignificantBits());
    }

    /**
     * reads an uuid from this buffer, that is stored in two longs
     *
     * @return the read uuid
     * @see ResizableByteBuffer#readLong()
     */
    public UUID readUUID() {
        return new UUID(readLong(), readLong());
    }

    /**
     * read a byte form this buffer
     * @return the read byte
     */
    public byte readByte() {
        return buffer.get();
    }

    // -------------------------------------------------------------------

    /**
     * @return the buffer's current position.
     * @see ByteBuffer#position()
     */

    public int position() {
        return buffer.position();
    }

    /**
     * Sets the buffer's current position.
     *
     * @param pos the position
     * @see ByteBuffer#position(int)
     */

    public void position(int pos) {
        buffer.position(pos);
    }

    /**
     * @return the buffer's remaing space
     * @see ByteBuffer#remaining()
     */

    public int remaining() {
        return buffer.remaining();
    }

    /**
     * @return the underlying nio {@link ByteBuffer}
     */

    public ByteBuffer nioBuffer() {
        return buffer;
    }

    /**
     * flips the buffer
     *
     * @see ByteBuffer#flip()
     */
    public void flip() {
        buffer.flip();
    }

    /**
     * clears the buffer
     *
     * @see ByteBuffer#clear()
     */

    public void clear() {
        buffer.clear();
    }

    /**
     * compacts this buffer
     *
     * @see ByteBuffer#clear()
     */

    public void compact() {
        buffer.compact();
    }

    /**
     * @return the buffer's limit
     * @see ByteBuffer#limit()
     */

    public int limit() {
        return buffer.limit();
    }

    /**
     * An exception that indicated that something went wrong while reading/writing data from this buffer.
     * Note that this is not thrown if the buffer has to few bytes, instead a {@link BufferUnderflowException} is
     * used in this case.
     */
    public static final class TypeDeserializationException extends RuntimeException {
        private TypeDeserializationException(String message) {
            super(message);
        }
    }

}
