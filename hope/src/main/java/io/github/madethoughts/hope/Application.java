/*
 *     Hope - A minecraft server reimplementation
 *     Copyright (C) 2023 Nick Hensel and contributors
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
            Thread.startVirtualThread(() -> {
                for (; ; ) {
                    try {
                        //                        var packet = socketHandler.packetQueue().take();
                        //                        Logger.getAnonymousLogger().info("Got packet (play): %s".formatted(packet));
                        Thread.sleep(100000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }).join();
        }
    }
}
