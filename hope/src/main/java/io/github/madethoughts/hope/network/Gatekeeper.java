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

import io.github.madethoughts.hope.Server;
import io.github.madethoughts.hope.configuration.NetworkingConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * This gatekeeper waits for new clients to connect, reads/deserializes their packets and puts them in
 * a queue to be taken by a receiver (the game loop)
 *
 * @see PacketReceiver
 * @see PacketSender
 */
public final class Gatekeeper implements AutoCloseable, Consumer<Server> {

    private static final Logger log = LoggerFactory.getLogger(Gatekeeper.class);
    private final Map<SocketAddress, Connection> connections = new ConcurrentHashMap<>();
    private final ServerSocketChannel socketChannel;

    private Gatekeeper(ServerSocketChannel socketChannel) {
        this.socketChannel = socketChannel;
    }

    /**
     * Constructs a new packet pipeline including a {@link ServerSocketChannel} and starts listening
     * for connections and packets.
     *
     * @param config the {@link NetworkingConfig} to be used
     * @return the Pipeline used to listen for packets
     * @throws IOException      see {@link ServerSocketChannel#open()}, {@link SocketChannel#bind(SocketAddress)}
     * @throws RuntimeException some exception from one of the virtual threads
     */
    public static Gatekeeper open(NetworkingConfig config) throws IOException {
        var channel = ServerSocketChannel.open();
        channel.socket().bind(new InetSocketAddress(config.host(), config.port()));
        return new Gatekeeper(channel);
    }

    @Override
    public void accept(Server server) {
        try {
            log.info("Listen for connections on %s".formatted(socketChannel.getLocalAddress()));
            while (socketChannel.isOpen()) {
                var clientChannel = socketChannel.accept();
                var remoteAddress = clientChannel.getRemoteAddress();

                log.info("New connection: %s".formatted(remoteAddress));

                var connection = new Connection(clientChannel, State.HANDSHAKE);
                connections.put(remoteAddress, connection);

                var sender = Thread.ofVirtual()
                                   .name("Sender for %s".formatted(remoteAddress))
                                   .start(new PacketSender(connection));

                Thread.startVirtualThread(new PacketReceiver(connection, sender))
                      .setName("Listener for %s".formatted(remoteAddress));
            }
        } catch (IOException e) {
            log.error("Unexpected exception in gatekeeper, shutting down server..", e);
            // shutdown server
            server.close();
        }
    }

    /**
     * @see SocketChannel#close()
     */
    @Override
    public void close() throws IOException {
        socketChannel.close();
    }
}
