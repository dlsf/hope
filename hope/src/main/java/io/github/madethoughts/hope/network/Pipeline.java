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

import io.github.madethoughts.hope.network.packets.deserialization.DeserializerResult;
import io.github.madethoughts.hope.network.packets.deserialization.PacketDeserializer;
import io.github.madethoughts.hope.network.packets.serverbound.Handshake;
import io.github.madethoughts.hope.network.packets.serverbound.ServerboundPacket;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

/**
 This pipelines waits for new clients to connect, reads/deserializes their packets and puts them in
 a queue to be taken by a receiver (the game loop)
 */
public final class Pipeline implements AutoCloseable {

    private static final Logger log = Logger.getAnonymousLogger();

    // TODO: 2/9/23 Choose channel size, or add config option
    private final BlockingQueue<ServerboundPacket> packetQueue = new LinkedBlockingQueue<>(32);
    private final Map<SocketAddress, Connection> connections = new ConcurrentHashMap<>();

    private final PacketDeserializer deserializer = new PacketDeserializer();
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

    private void listen(Connection connection) {
        var channel = connection.socketChannel();
        try (channel) {
            var buffer = ByteBuffer.allocateDirect(32);

            int num;
            int neededBytes = 0;
            do {
                num = channel.read(buffer);
                if (buffer.position() < neededBytes) continue;
                boolean anotherRun;
                do {
                    anotherRun = false;
                    var state = connection.state();

                    switch (deserializer.tryDeserialize(state, buffer)) {
                        case DeserializerResult.UnknownPacket(var ustate, var id) -> throw new IllegalStateException(
                                "Packetnknown %s: %s".formatted(ustate, id));
                        case DeserializerResult.PacketDeserialized(var packet) -> {
                            Logger.getAnonymousLogger().info("Got packet: %s".formatted(packet));
                            switch (state) {
                                case HANDSHAKE -> connection.state(((Handshake) packet).nextState());
                                case STATUS -> throw new RuntimeException("todo status");
                                case LOGIN -> throw new RuntimeException("todo login");
                                case PLAY -> packetQueue.put(packet);
                            }
                            anotherRun = true;
                        }
                        case DeserializerResult.MoreBytesNeeded(var size) -> {
                            if (size > buffer.capacity()) {
                                buffer = ByteBuffer.allocateDirect(size);
                            }
                        }
                        case DeserializerResult.Failed failed -> {
                            log.info("failed: %s".formatted(channel));
                        }
                    }
                } while (anotherRun);
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
                log.info("New connection: %s".formatted(channel.getRemoteAddress()));
                var connection = new Connection(channel, State.HANDSHAKE);
                connections.put(channel.getRemoteAddress(), connection);
                Thread.startVirtualThread(() -> listen(connection))
                      .setName("Listener for %s".formatted(channel.getRemoteAddress()));
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

    public BlockingQueue<ServerboundPacket> packetQueue() {
        return packetQueue;
    }
}
