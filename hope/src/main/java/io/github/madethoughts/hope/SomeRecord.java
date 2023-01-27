package io.github.madethoughts.hope;

import jdk.internal.vm.annotation.Contended;

public record SomeRecord(
        @Contended
        int test,
        String lol
) {
}
