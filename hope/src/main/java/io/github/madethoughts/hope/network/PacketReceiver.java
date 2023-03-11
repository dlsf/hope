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

import io.github.madethoughts.hope.network.handler.HandshakeHandler;
import io.github.madethoughts.hope.network.handler.LoginHandler;
import io.github.madethoughts.hope.network.handler.PacketHandler;
import io.github.madethoughts.hope.network.handler.StatusHandler;
import io.github.madethoughts.hope.network.packets.serverbound.DeserializerResult;
import io.github.madethoughts.hope.network.packets.serverbound.ServerboundPacket;
import io.github.madethoughts.hope.network.packets.serverbound.handshake.Handshake;

import java.net.SocketAddress;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.SocketChannel;
import java.util.logging.Logger;

/**
 * This class is responsible for receiving and handling packets send to the sender by a specific connection.
 * All sorts of packets are handled by their corresponding {@link PacketHandler}. Note that {@link State#STATUS},
 * {@link State#HANDSHAKE} and {@link State#LOGIN} are handled independent of the server's ticks.
 * This receiver supports encrypted data, but no compressed packets.
 * All exceptions thrown while handling (before passed to the game loop), {@link SocketChannel#close()} or an
 * interrupt signal will cause this receiver to stop listening for data, interrupting the {@link PacketSender} threads
 * and closing the underlying {@link SocketChannel}
 */
public final class PacketReceiver implements Runnable {

    private static final Logger log = Logger.getLogger(PacketReceiver.class.getName());

    private final Connection connection;
    private final ResizableByteBuffer buffer = ResizableByteBuffer.allocateDirect();

    private final PacketHandler<Handshake> handshakeHandler;
    private final PacketHandler<ServerboundPacket.StatusPacket> statusHandler;
    private final PacketHandler<ServerboundPacket.LoginPacket> loginHandler;

    private final Thread senderThread;

    public PacketReceiver(Connection connection, Thread senderThread) {
        this.connection = connection;
        handshakeHandler = new HandshakeHandler(connection);
        statusHandler = new StatusHandler(connection);
        loginHandler = new LoginHandler(connection);
        this.senderThread = senderThread;
    }

    @Override
    public void run() {
        var channel = connection.socketChannel();
        SocketAddress address = null;

        try (channel) {
            address = channel.getRemoteAddress();

            var neededBytes = 0;
            while (channel.isOpen() && channel.read(buffer.nioBuffer()) != -1) {
                if (buffer.position() < neededBytes) continue;

                var decryptor = connection.decryptor();
                if (decryptor != null) {
                    buffer.flip();
                    decryptor.update(buffer.nioBuffer());
                }

                neededBytes = deserializeAndHandle();

                buffer.ensureCapacity(neededBytes);
            }

            // interrupt sender thread to stop blocking for incoming packets
            senderThread.interrupt();

            log.info("Disconnected %s".formatted(channel.getRemoteAddress()));
        } catch (AsynchronousCloseException ignored) {
        } catch (Throwable e) {
            log.info("Unexpected exception in PacketReceiver for %s: %s".formatted(address, e));
        }
        log.info("Closed receiver for %s".formatted(address));
    }

    /**
     * Tries to deserialize all packets currently represented by the read bytes.
     *
     * @return the bytes needed for the current or next packet
     * @throws NetworkingException if any checked exception was thrown
     */
    private int deserializeAndHandle() throws NetworkingException {
        while (true) {
            buffer.flip();
            switch (ServerboundPacket.tryDeserialize(connection.state(), buffer)) {
                case DeserializerResult.UnknownPacket(var ustate, var id) ->
                        throw new UnsupportedOperationException("unknown packet %s: %s".formatted(ustate, id));
                case DeserializerResult.PacketDeserialized(var packet) -> {
                    log.info("Got packet: %s".formatted(packet));
                    switch (packet) {
                        case Handshake handshake -> handshakeHandler.handle(handshake);
                        case ServerboundPacket.StatusPacket statusPacket -> statusHandler.handle(statusPacket);
                        case ServerboundPacket.LoginPacket loginPacket -> loginHandler.handle(loginPacket);
                    }
                    // try to deserialize left bytes
                    continue;
                }
                case DeserializerResult.MoreBytesNeeded(var size) -> {
                    // don't override data already written in the buffer
                    var pos = buffer.limit();
                    buffer.clear();
                    buffer.position(pos);

                    return size;
                }
            }
            return 1;
        }
    }
}
