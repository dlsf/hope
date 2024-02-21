package io.github.madethoughts.hope.nbt.tree;

import java.util.List;

public record TagList(
        List<Tag> values
) implements Tag {
}
