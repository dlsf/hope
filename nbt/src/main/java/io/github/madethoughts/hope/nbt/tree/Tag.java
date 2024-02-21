package io.github.madethoughts.hope.nbt.tree;

public sealed interface Tag permits TagByte, TagByteArray, TagCompound, TagDouble, TagFloat, TagInt, TagIntArray, TagList, TagLong, TagLongArray, TagShort, TagString {
}
