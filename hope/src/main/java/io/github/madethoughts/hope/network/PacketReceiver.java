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

import io.github.madethoughts.hope.network.packets.DeserializerResult;
import io.github.madethoughts.hope.network.packets.clientbound.PingResponse;
import io.github.madethoughts.hope.network.packets.clientbound.StatusResponse;
import io.github.madethoughts.hope.network.packets.serverbound.Handshake;
import io.github.madethoughts.hope.network.packets.serverbound.PingRequest;
import io.github.madethoughts.hope.network.packets.serverbound.ServerboundPacket;
import io.github.madethoughts.hope.network.packets.serverbound.StatusRequest;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.logging.Logger;

public final class PacketReceiver {

    private static final Logger log = Logger.getLogger(PacketReceiver.class.getName());

    private final Pipeline pipeline;
    private final Connection connection;
    private ByteBuffer buffer = ByteBuffer.allocate(32);

    public PacketReceiver(Pipeline pipeline, Connection connection) {
        this.pipeline = pipeline;
        this.connection = connection;
    }

    public void start() {
        var channel = connection.socketChannel();
        try (channel) {
            int byteNum;
            int neededBytes = 0;
            do {
                byteNum = channel.read(buffer);
                if (buffer.position() < neededBytes) continue;

                neededBytes = deserializeAndHandle();
            } while (byteNum != -1);
        } catch (IOException e) {
            // TODO: 2/9/23 logging
            throw new RuntimeException(e);
        }
    }

    private int deserializeAndHandle() {
        var state = connection.state();

        switch (pipeline.tryDeserialize(state, buffer)) {
            case DeserializerResult.UnknownPacket(var ustate, var id) ->
                    throw new IllegalStateException("Packetnknown %s: %s".formatted(ustate, id));
            case DeserializerResult.PacketDeserialized(var packet) -> {
                log.info("Got packet: %s".formatted(packet));
                switch (state) {
                    case HANDSHAKE -> connection.state(((Handshake) packet).nextState());
                    case STATUS -> handleStatus(packet);
                    case LOGIN -> throw new UnsupportedOperationException("todo login"); // TODO: 2/24/23
                    case PLAY -> throw new UnsupportedOperationException("todo play"); // TODO: 2/24/23
                }
                deserializeAndHandle(); // try to deserialize left bytes, if any. tail recursion is ok here
            }
            case DeserializerResult.MoreBytesNeeded(var size) -> {
                adjustBuffer(size);
                return size;
            }
            case DeserializerResult.Failed failed -> {
                log.info("failed: %s".formatted(failed.reason()));
            }
        }
        return 0;
    }

    private void adjustBuffer(int size) {
        if (size > buffer.capacity()) {
            buffer = ByteBuffer.allocate(size)
                               .put(buffer.rewind());
        }
    }

    private void handleStatus(ServerboundPacket packet) {
        try {
            switch (packet) {
                // TODO: 2/24/23 add real values and config here
                case StatusRequest() -> connection.queuePacket(new StatusResponse(
                        new StatusResponse.Version("1.19.3", 761),
                        new StatusResponse.Players(
                                10,
                                0
                        ),
                        "test hahah",
                        new byte[0],
                        false,
                        false
                ));
                case PingRequest(var payload) -> connection.queuePacket(new PingResponse(payload));
                case default -> throw new IllegalStateException("wrong status packet: %s".formatted(packet));
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
