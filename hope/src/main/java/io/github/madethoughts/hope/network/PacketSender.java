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

import io.github.madethoughts.hope.network.packets.clientbound.ClientboundPacket;
import io.github.madethoughts.hope.network.packets.clientbound.login.LoginDisconnect;
import io.github.madethoughts.hope.network.packets.clientbound.status.PingResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.BlockingQueue;

/**
 * This class is responsible for serializing and sending packets waiting in the {@link Connection#clientboundPackets()}
 * queue.
 * The sender supports encryption but no compression.
 * If any exception is thrown while serializing or sending, the sender stops listening for new packets in the queue
 * and closes the underlying {@link SocketChannel}, which will cause the {@link PacketReceiver} to stop.
 */
public class PacketSender implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(PacketSender.class);

    private final Connection connection;
    private final BlockingQueue<ClientboundPacket> packetQueue;

    private final ByteBuffer lengthBuffer = ByteBuffer.allocate(5);

    private final ResizableByteBuffer buffer = ResizableByteBuffer.allocateDirect();

    public PacketSender(Connection connection) {
        this.connection = connection;
        packetQueue = connection.clientboundPackets();
    }

    @Override
    public void run() {
        try (var channel = connection.socketChannel()) {
            while (channel.isOpen()) {
                var packet = packetQueue.take();

                // deserialize packet
                buffer.clear();
                buffer.writeVarInt(packet.id());
                packet.serialize(buffer);

                lengthBuffer.clear();
                ResizableByteBuffer.writeVarInt(lengthBuffer, buffer.position());

                // encrypt packet
                var encryptor = connection.encryptor();
                if (encryptor != null) {
                    lengthBuffer.flip();
                    encryptor.update(lengthBuffer);
                    buffer.flip();
                    encryptor.update(buffer.nioBuffer());
                }

                // write packet
                channel.write(lengthBuffer.flip());
                channel.write(buffer.nioBuffer().flip());

                log.debug("Send {} || Encrypted: {}", packet, connection.encryptor() != null);

                // closing connection if PingResponse is sent
                if (packet instanceof PingResponse || packet instanceof LoginDisconnect) {
                    channel.close();
                }
            }
        } catch (InterruptedException ignored) { // likely to be caused by PacketReceiver
        } catch (Throwable e) {
            log.error("Unexpected exception in packet sender, closing connection", e);
        }
    }
}
