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

import io.github.madethoughts.hope.configuration.ServerConfig;
import io.github.madethoughts.hope.network.handler.HandshakeHandler;
import io.github.madethoughts.hope.network.handler.LoginHandler;
import io.github.madethoughts.hope.network.handler.PacketHandler;
import io.github.madethoughts.hope.network.handler.StatusHandler;
import io.github.madethoughts.hope.network.packets.serverbound.DeserializerResult;
import io.github.madethoughts.hope.network.packets.serverbound.ServerboundPacket;
import io.github.madethoughts.hope.network.packets.serverbound.handshake.Handshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.SocketChannel;

/**
 * This class is responsible for receiving and handling packets send to the sender by a specific connection.
 * All sorts of packets are handled by their corresponding {@link PacketHandler}. Note that {@link State#STATUS},
 * {@link State#HANDSHAKE} and {@link State#LOGIN} are handled independent of the server's ticks.
 * This receiver supports encrypted data, but no compressed packets.
 * All exceptions thrown while handling (before passed to the game loop), {@link SocketChannel#close()} or an
 * interrupt signal will cause this receiver to stop listening for data, interrupting the {@link PacketSender} threads
 * and closing the underlying {@link SocketChannel}.
 * If {@link PacketReceiver#run()} returns, the connection is closed.
 */
public final class PacketReceiver implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(PacketReceiver.class);

    private final Connection connection;
    private final ResizableByteBuffer buffer = ResizableByteBuffer.allocateDirect();

    private final PacketHandler<Handshake> handshakeHandler;
    private final PacketHandler<ServerboundPacket.StatusPacket> statusHandler;
    private final PacketHandler<ServerboundPacket.LoginPacket> loginHandler;

    private final Thread senderThread;

    public PacketReceiver(Connection connection, Thread senderThread, ServerConfig config) {
        this.connection = connection;
        handshakeHandler = new HandshakeHandler(connection);
        statusHandler = new StatusHandler(connection, config);
        loginHandler = new LoginHandler(connection);
        this.senderThread = senderThread;
    }

    /**
     * Reads and handles packets.
     * If this method returns, the connection is closed.
     */
    @Override
    public void run() {
        try (var channel = connection.socketChannel()) {
            var address = channel.getRemoteAddress();
            var neededBytes = 0;
            while (channel.isOpen() && channel.read(buffer.nioBuffer()) != -1) {
                if (buffer.position() < neededBytes) continue;

                var decryptor = connection.decryptor();
                if (decryptor != null) {
                    buffer.flip();
                    decryptor.update(buffer.nioBuffer());
                }

                // deserialize packet(s)
                outer:
                while (true) {
                    buffer.flip();
                    switch (ServerboundPacket.tryDeserialize(connection.state(), buffer)) {
                        case DeserializerResult.UnknownPacket(var state, var id) ->
                                log.error("Unknown packet %s : %s for %s".formatted(state, id, address));
                        case DeserializerResult.PacketDeserialized(var packet) -> {
                            log.debug("Got packet {} for {}", packet, address);
                            switch (packet) {
                                case Handshake handshake -> handshakeHandler.handle(handshake);
                                case ServerboundPacket.StatusPacket statusPacket -> statusHandler.handle(statusPacket);
                                case ServerboundPacket.LoginPacket loginPacket -> loginHandler.handle(loginPacket);
                            }
                        }
                        case DeserializerResult.MoreBytesNeeded(var size) -> {
                            // don't override data already written in the buffer
                            var pos = buffer.limit();
                            buffer.clear();
                            buffer.position(pos);
                            neededBytes = size;
                            break outer;
                        }
                    }
                }

                buffer.ensureCapacity(neededBytes);
            }
        } catch (AsynchronousCloseException ignored) {
        } catch (Throwable e) {
            log.error("Unexpected error in packet receiver, closing connection.", e);
        } finally {
            // interrupt sender thread to stop blocking for incoming packets
            senderThread.interrupt();

            log.info("Connection closed.");
        }
    }
}
