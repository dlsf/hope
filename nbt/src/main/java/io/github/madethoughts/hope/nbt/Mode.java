package io.github.madethoughts.hope.nbt;

public enum Mode {

    /**
     * In network mode, the root compounds name will be omitted.
     * This is the notchains' implementation behaviour for nbt data send over the network (protocol) since protocol version 764 (mc 1.20.2).
     */
    NETWORK,

    /**
     * In file mode, the root compounds name will be read
     */
    FILE,
}
