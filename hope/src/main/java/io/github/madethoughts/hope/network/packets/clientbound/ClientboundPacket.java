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

package io.github.madethoughts.hope.network.packets.clientbound;

import io.github.madethoughts.hope.network.ResizableByteBuffer;
import io.github.madethoughts.hope.network.packets.clientbound.login.EncryptionRequest;
import io.github.madethoughts.hope.network.packets.clientbound.login.LoginDisconnect;
import io.github.madethoughts.hope.network.packets.clientbound.login.LoginSuccess;
import io.github.madethoughts.hope.network.packets.clientbound.status.PingResponse;
import io.github.madethoughts.hope.network.packets.clientbound.status.StatusResponse;

/**
 * Indicated that a packet is client bound and is serializable.
 */
public sealed interface ClientboundPacket
        permits EncryptionRequest, LoginDisconnect, LoginSuccess, PingResponse, StatusResponse {

    void serialize(ResizableByteBuffer buffer);

    int id();
}
