package io.github.madethoughts.hope.nbt.deserialization;

public class NBTDeserializationException extends RuntimeException {
    NBTDeserializationException(int position, String error, Object... args) {
        super("Deserialization exception at position %s: %s".formatted(position, error.formatted(args)));
    }

    NBTDeserializationException(String msg, Throwable throwable) {
        super(msg, throwable);
    }
}
