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
import io.github.madethoughts.hope.profile.PlayerProfile;

import java.nio.channels.SocketChannel;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Connection {
    private final SocketChannel socketChannel;
    private final BlockingQueue<ClientboundPacket> clientboundPackets = new LinkedBlockingQueue<>(6);
    private State state;
    private McCipher decryptor;
    private McCipher encryptor;
    private PlayerProfile playerProfile;

    public Connection(SocketChannel socketChannel, State state) {
        this.socketChannel = socketChannel;
        this.state = state;
    }

    public SocketChannel socketChannel() {
        return socketChannel;
    }

    public State state() {
        return state;
    }

    public void state(State state) {
        this.state = state;
    }

    public BlockingQueue<ClientboundPacket> clientboundPackets() {
        return clientboundPackets;
    }

    public void queuePacket(ClientboundPacket packet) throws InterruptedException {
        clientboundPackets.put(packet);
    }

    public McCipher decryptor() {
        return decryptor;
    }

    public void decryptor(McCipher decryptor) {
        this.decryptor = decryptor;
    }

    public McCipher encryptor() {
        return encryptor;
    }

    public void encryptor(McCipher encryptor) {
        this.encryptor = encryptor;
    }

    public PlayerProfile playerProfile() {
        return playerProfile;
    }

    public void playerProfile(PlayerProfile playerProfile) {
        this.playerProfile = playerProfile;
    }
}
