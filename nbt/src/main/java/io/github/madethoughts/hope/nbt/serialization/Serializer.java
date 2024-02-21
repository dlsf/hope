package io.github.madethoughts.hope.nbt.serialization;

import io.github.madethoughts.hope.nbt.Compression;
import io.github.madethoughts.hope.nbt.Mode;
import io.github.madethoughts.hope.nbt.Type;
import io.github.madethoughts.hope.nbt.tree.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import static java.util.FormatProcessor.FMT;

public final class Serializer {
    private final RootCompound tree;
    private final Mode mode;

    private final ByteBuffer numberBuffer = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN);
    private final ByteArrayOutputStream finalOutputStream = new ByteArrayOutputStream();
    private final OutputStream out;

    private Serializer(Mode mode, Compression compression, RootCompound tree) {
        try {
            this.tree = tree;
            this.mode = mode;
            this.out = switch (compression) {
                case NONE -> finalOutputStream;
                case GZIP -> new GZIPOutputStream(finalOutputStream);
            };
        } catch (IOException e) {
            throw wrappedError(e);
        }
    }

    /**
     * Serializes a NBT Tree to an array of bytes according to the passed mode, optionally compressing it
     * @param mode the mode to be used
     * @param compression the compression to be used
     * @param tree the tree to be serialized
     * @return the serialized bytes
     */
    public static byte[] serialize(Mode mode, Compression compression, RootCompound tree) {
        return new Serializer(mode, compression, tree).serializeTree();
    }

    private NBTSerializationException errorArrayToBig() {
        return new NBTSerializationException("array is to big");
    }

    private NBTSerializationException wrappedError(Exception e) {
        return new NBTSerializationException("Unexpected error during tree processing.", e);
    }

    private byte[] serializeTree() {
        try (out) {
            switch (mode) {
                case NETWORK -> out.write(Type.COMPOUND.id());
                case FILE -> {
                    out.write(Type.COMPOUND.id());
                    unwrappedString(tree.name());
                }
            }

            write(tree.compound());

            if (out instanceof GZIPOutputStream gzipOutputStream) {
                gzipOutputStream.finish();
            }
            return finalOutputStream.toByteArray();
        } catch (IOException ioException) {
            throw wrappedError(ioException);
        }
    }

    private void unwrappedString(String value) throws IOException {
        var bytes = value.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > Short.MAX_VALUE)
            throw new NBTSerializationException(FMT."String length exceed maximum of \{Short.MAX_VALUE}");
        writeShort((short) bytes.length);
        out.write(bytes);
    }

    private void writeShort(short value) throws IOException {
        numberBuffer.clear();
        numberBuffer.putShort(value);
        out.write(numberBuffer.array(), 0, 2);
    }

    private void writeLong(long value) throws IOException {
        numberBuffer.clear();
        numberBuffer.putLong(value);
        out.write(numberBuffer.array(), 0, 8);
    }

    private void writeInt(int value) throws IOException {
        numberBuffer.clear();
        numberBuffer.putInt(value);
        out.write(numberBuffer.array(), 0, 4);
    }

    private void writeFloat(float value) throws IOException {
        numberBuffer.clear();
        numberBuffer.putFloat(value);
        out.write(numberBuffer.array(), 0, 4);
    }

    private void writeDouble(double value) throws IOException {
        numberBuffer.clear();
        numberBuffer.putDouble(value);
        out.write(numberBuffer.array(), 0, 8);
    }

    private void write(Tag current) {
        try {
            switch (current) {
                case TagCompound(Map<String, Tag> tags) -> {
                    for (var entry : tags.entrySet()) {
                        String name = entry.getKey();
                        Tag wrappedTag = entry.getValue();

                        out.write(Type.byTagClass(wrappedTag.getClass()).id());
                        unwrappedString(name);
                        write(wrappedTag);
                    }

                    out.write(Type.END.id());
                }
                case TagByte(byte value) -> out.write(value);
                case TagShort(short value) -> writeShort(value);
                case TagInt(int value) -> writeInt(value);
                case TagLong(long value) -> writeLong(value);
                case TagFloat(float value) -> writeFloat(value);
                case TagDouble(double value) -> writeDouble(value);
                case TagByteArray(byte[] value) -> {
                    writeInt(value.length);
                    out.write(value);
                }
                case TagString(String value) -> unwrappedString(value);
                case TagList(List<Tag> tags) -> {
                    byte id = (byte) (tags.isEmpty()
                            ? Type.END.id()
                            : Type.byTagClass(tags.getFirst().getClass()).id());

                    out.write(id);
                    writeInt(tags.size());
                    tags.forEach(this::write);
                }
                case TagIntArray(int[] value) -> {
                    if (((long) value.length * Integer.BYTES > Integer.MAX_VALUE)) throw errorArrayToBig();
                    writeInt(value.length);

                    ByteBuffer buffer = ByteBuffer.allocate(value.length * Integer.BYTES);
                    buffer.asIntBuffer().put(value);
                    out.write(buffer.array());
                }
                case TagLongArray(long[] value) -> {
                    if (((long) value.length * Long.BYTES > Integer.MAX_VALUE)) throw errorArrayToBig();
                    writeInt(value.length);

                    ByteBuffer buffer = ByteBuffer.allocate(value.length * Long.BYTES);
                    buffer.asLongBuffer().put(value);
                    out.write(buffer.array());
                }
            }
        } catch (IOException e) {
            throw wrappedError(e);
        }
    }
}
