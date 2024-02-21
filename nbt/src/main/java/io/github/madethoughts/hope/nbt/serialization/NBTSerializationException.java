package io.github.madethoughts.hope.nbt.serialization;

public class NBTSerializationException extends RuntimeException {
    NBTSerializationException(String error, Object... args) {
        super("Serialization exception: %s".formatted(error.formatted(args)));
    }

    NBTSerializationException(String msg, Throwable throwable) {
        super(msg, throwable);
    }
}
