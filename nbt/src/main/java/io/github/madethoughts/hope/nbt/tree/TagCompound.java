package io.github.madethoughts.hope.nbt.tree;

import java.util.Map;

public record TagCompound(
        Map<String, Tag> values
) implements Tag {
}
