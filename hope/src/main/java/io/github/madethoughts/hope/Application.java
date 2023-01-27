package io.github.madethoughts.hope;

import io.github.madethoughts.hope.network.Pipeline;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 The programms main entry
 */
public final class Application {
    private Application() {
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        var socketHandler = Pipeline.openAndListen(new InetSocketAddress(25565));
        try (socketHandler) {
            System.out.printf("Listening on %n", socketHandler.address());
            for (; ; ) ;
        }
    }
}
