package io.github.madethoughts.hope.network.handler;

import io.github.madethoughts.hope.network.Connection;
import io.github.madethoughts.hope.network.NetworkingException;
import io.github.madethoughts.hope.network.State;
import io.github.madethoughts.hope.network.packets.clientbound.configuration.ClientboundFinishConfiguration;
import io.github.madethoughts.hope.network.packets.serverbound.ServerboundPacket;
import io.github.madethoughts.hope.network.packets.serverbound.configuration.ClientInformation;
import io.github.madethoughts.hope.network.packets.serverbound.configuration.FinishConfiguration;
import io.github.madethoughts.hope.network.packets.serverbound.configuration.PluginMessage;

public class ConfigurationHandler implements PacketHandler<ServerboundPacket.ConfigurationPacket> {

    private final Connection connection;

    public ConfigurationHandler(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void handle(ServerboundPacket.ConfigurationPacket packet) throws NetworkingException {
        switch (packet) {
            case PluginMessage _ -> connection.queuePacket(new ClientboundFinishConfiguration());
            case ClientInformation _ -> {}
            case FinishConfiguration _ -> connection.state(State.PLAY);
        }
    }
}
