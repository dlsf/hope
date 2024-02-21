package io.github.madethoughts.hope.nbt;

import io.github.madethoughts.hope.nbt.tree.*;

public enum Type {
    END(null, 0),
    BYTE(TagByte.class, 1),
    SHORT(TagShort.class, 2),
    INT(TagInt.class, 3),
    LONG(TagLong.class, 4),
    FLOAT(TagFloat.class, 5),
    DOUBLE(TagDouble.class, 6),
    BYTE_ARRAY(TagByteArray.class, 7),
    STRING(TagString.class, 8),
    LIST(TagList.class, 9),
    COMPOUND(TagCompound.class, 10),
    INT_ARRAY(TagIntArray.class, 11),
    LONG_ARRAY(TagLongArray.class, 12);


    private final int id;
    private final Class<?> klass;

    Type(Class<?> klass, int id) {
        this.id = id;
        this.klass = klass;
    }

    public static Type byId(int id) {
        for (var type : Type.values()) {
            if (id == type.id) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown  id %s".formatted(id));
    }

    public static Type byTagClass(Class<? extends Tag> klass) {
        for (var type : Type.values()) {
            if (type.klass == klass) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown class %s".formatted(klass));
    }

    public int id() {
        return id;
    }

    public Class<?> klass() {
        return klass;
    }
}
