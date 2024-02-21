package io.github.madethoughts.hope.network.packets.serverbound.login;

import io.github.madethoughts.hope.network.packets.serverbound.ServerboundPacket;

public record LoginAcknowledged() implements ServerboundPacket.LoginPacket {
}
