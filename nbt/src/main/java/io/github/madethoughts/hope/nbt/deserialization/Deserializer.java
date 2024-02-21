package io.github.madethoughts.hope.nbt.deserialization;

import io.github.madethoughts.hope.nbt.Compression;
import io.github.madethoughts.hope.nbt.Mode;
import io.github.madethoughts.hope.nbt.Type;
import io.github.madethoughts.hope.nbt.tree.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;

public final class Deserializer {
    private final Mode mode;
    private final Compression compression;

    private ByteBuffer buffer;

    private Deserializer(Mode mode, Compression compression, ByteBuffer buffer) {
        buffer.order(ByteOrder.BIG_ENDIAN);
        this.mode = mode;
        this.buffer = buffer;
        this.compression = compression;
    }

    /**
     * Deserializes the given perhaps compressed {@link ByteBuffer} to a nbt tree
     * @param mode the mode to be used
     * @param compression the type of compression to be used
     * @param buffer the buffer to be deserialized
     * @return the deserialized nbt data
     */
    public static RootCompound deserialize(Mode mode, Compression compression, ByteBuffer buffer) {
        return new Deserializer(mode, compression, buffer).deserializeBytes();
    }

    /**
     * Deserializes the given perhaps compressed {@code  byte[]} to a nbt tree
     * @param mode the mode to be used
     * @param compression the type of compression to be used
     * @param bytes the bytes to be deserialized
     * @return the deserialized nbt data
     */
    public static RootCompound deserialize(Mode mode, Compression compression, byte[] bytes) {
        return deserialize(mode, compression, ByteBuffer.wrap(bytes));
    }

    private NBTDeserializationException error(String msg) {
        return new NBTDeserializationException(buffer.position(), msg);
    }

    private RootCompound deserializeBytes() {
        decompress();
        if (Type.byId(buffer.get()) != Type.COMPOUND) throw error("NBT Data has to start with a compound");

        String name = switch (mode) {
            case NETWORK -> "";
            case FILE -> unwrappedString();
        };

        var payload = compound();
        return new RootCompound(name, payload);
    }

    private void decompress() {
        try {
            buffer = switch (compression) {
                case NONE -> buffer;
                case GZIP -> {
                    try (var inputStream = new GZIPInputStream(new ByteBufferBackedInputStream(buffer))) {
                        try (var outputStream = new ByteArrayOutputStream()) {
                            inputStream.transferTo(outputStream);
                            yield ByteBuffer.wrap(outputStream.toByteArray());
                        }
                    }
                }
            };
        } catch (IOException ioException) {
            throw new NBTDeserializationException("An Exception occurred during decompression", ioException);
        }
    }

    private Tag deserialize(Type type) {
        return switch (type) {
            case END -> throw error("TAG_END isn't allowed to be wrapped in a named tag");
            case BYTE -> new TagByte(buffer.get());
            case SHORT -> new TagShort(buffer.getShort());
            case INT -> new TagInt(buffer.getInt());
            case LONG -> new TagLong(buffer.getLong());
            case FLOAT -> new TagFloat(buffer.getFloat());
            case DOUBLE -> new TagDouble(buffer.getDouble());
            case STRING -> new TagString(unwrappedString());
            case LIST -> list();
            case COMPOUND -> compound();
            case BYTE_ARRAY -> {
                int length = buffer.getInt();
                var bytes = new byte[length];
                buffer.get(bytes);
                yield new TagByteArray(bytes);
            }
            case INT_ARRAY -> {
                int length = buffer.getInt();
                var ints = new int[length];
                buffer.asIntBuffer().get(ints);
                yield new TagIntArray(ints);
            }
            case LONG_ARRAY -> {
                int length = buffer.getInt();
                var longs = new long[length];
                buffer.asLongBuffer().get(longs);
                yield new TagLongArray(longs);
            }
        };
    }

    private String unwrappedString() {
        // unsigned short
        int length = buffer.getShort();
        var bytes = new byte[length];
        buffer.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private TagCompound compound() {
        var tags = new HashMap<String, Tag>();
        while (Type.byId(buffer.get(buffer.position())) != Type.END) {
            Type type = Type.byId(buffer.get());
            String name = unwrappedString();
            Tag payload = deserialize(type);

            tags.put(name, payload);
        }
        buffer.get();
        return new TagCompound(tags);
    }

    private TagList list() {
        var tags = new ArrayList<Tag>();
        var type = Type.byId(buffer.get());
        int length = buffer.getInt();
        for (int i = 0; i < length; i++) {
            tags.add(deserialize(type));
        }
        return new TagList(tags);
    }
}
