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

package io.github.madethoughts.hope.network;

import io.github.madethoughts.hope.network.packets.Packet;
import io.github.madethoughts.hope.network.packets.deserialization.Deserializer;
import io.github.madethoughts.hope.network.packets.deserialization.DeserializerResult;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 This pipelines waits for new clients to connect, reads/deserializes their packets and puts them in
 a queue to be taken by a receiver (the game loop)
 */
public final class Pipeline implements AutoCloseable {

    // TODO: 2/9/23 Choose channel size, or add config option
    private final BlockingQueue<Packet> packetQueue = new LinkedBlockingQueue<>(32);
    private final Map<SocketAddress, SocketChannel> connections = new ConcurrentHashMap<>();

    private final Deserializer deserializer = new Deserializer();
    private final ServerSocketChannel socketChannel;
    private final Thread listenerThread = Thread.ofVirtual()
                                                .name("PacketListenerThread")
                                                .unstarted(this::listenForConnections);

    private Pipeline(ServerSocketChannel socketChannel) {
        this.socketChannel = socketChannel;
    }

    /**
     Constructs a new packet pipeline including a {@link ServerSocketChannel} and starts listening
     for connections and packets.

     @param address the address the socket is bound to
     @return the Pipeline used to listen for packets
     @throws IOException      see {@link ServerSocketChannel#open()}, {@link SocketChannel#bind(SocketAddress)}
     @throws RuntimeException some exception from one of the virtual threads
     */
    public static Pipeline openAndListen(SocketAddress address) throws IOException {
        var channel = ServerSocketChannel.open();
        channel.socket().bind(address);
        var handler = new Pipeline(channel);
        handler.listenerThread.start();
        return handler;
    }

    private void listen(SocketChannel clientConnection) {
        try (clientConnection) {
            // TODO: 2/9/23 choose better size
            var buffer = ByteBuffer.allocateDirect(100);
            int num;
            int neededBytes = 0;
            do {
                num = clientConnection.read(buffer);

                if (buffer.position() < neededBytes) continue;

                System.out.printf("Got some bytes %s%n", buffer);
                switch (deserializer.tryDeserialize(buffer)) {
                    case DeserializerResult.UnknownPacket(var id) -> throw new IllegalStateException("Packet unknown %s".formatted(id));
                    case DeserializerResult.PacketDeserialized(var packet) -> packetQueue.put(packet);
                    case DeserializerResult.MoreBytesNeeded(var size) -> {
                        if (size > buffer.capacity()) {
                            buffer = ByteBuffer.allocateDirect(size);
                        }
                    }
                }
            } while (num != -1);
        } catch (IOException | InterruptedException e) {
            // TODO: 2/9/23 logging
            throw new RuntimeException(e);
        }
    }

    private void listenForConnections() {
        while (socketChannel.isOpen()) {
            try {
                var channel = socketChannel.accept();
                connections.put(channel.getRemoteAddress(), channel);
                Thread.startVirtualThread(() -> listen(channel));
            } catch (IOException e) {
                // TODO: 2/9/23 logging
                throw new RuntimeException(e);
            }

        }
    }

    /**
     @return the SocketAddress the socket is listening on
     @throws IOException see {@link SocketChannel#getLocalAddress()}
     */
    public SocketAddress address() throws IOException {
        return socketChannel.getLocalAddress();
    }

    @Override
    public void close() throws IOException {
        socketChannel.close();
    }
}
