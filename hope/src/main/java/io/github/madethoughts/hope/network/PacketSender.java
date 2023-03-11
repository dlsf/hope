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
import io.github.madethoughts.hope.network.packets.clientbound.status.PingResponse;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;

public class PacketSender implements Runnable {

    private static final Logger log = Logger.getLogger(PacketSender.class.getName());

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
        var channel = connection.socketChannel();
        SocketAddress address = null;
        try (channel) {
            address = channel.getRemoteAddress();

            while (channel.isOpen()) {
                var packet = packetQueue.take();
                deserialize(packet);

                var encryptor = connection.encryptor();
                if (encryptor != null) {
                    lengthBuffer.flip();
                    encryptor.update(lengthBuffer);
                    buffer.flip();
                    encryptor.update(buffer.nioBuffer());
                }

                channel.write(lengthBuffer.flip());
                channel.write(buffer.nioBuffer().flip());

                log.info("Send %s to %s || Encrypted: %s".formatted(packet, channel.getRemoteAddress(),
                        connection.encryptor() != null
                ));

                if (connection.state() == State.STATUS && packet instanceof PingResponse) {
                    channel.shutdownInput();
                }
            }
        } catch (InterruptedException ignored) { // likely to be thrown by PacketReceiver
        } catch (Throwable e) {
            log.info("Unexpected exception in PacketSender for %s: %s".formatted(address, e));
        }
        log.info("Closed sender for %s".formatted(address));
    }

    private void deserialize(ClientboundPacket packet) {
        buffer.clear();
        buffer.writeVarInt(packet.id());
        packet.serialize(buffer);

        lengthBuffer.clear();
        ResizableByteBuffer.writeVarInt(lengthBuffer, buffer.position());
    }
}
