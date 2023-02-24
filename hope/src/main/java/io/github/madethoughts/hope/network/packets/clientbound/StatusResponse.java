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

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public record StatusResponse(
        Version version,
        Players players,
        // todo support chat object
        String descriptionText,
        byte[] favicon,
        boolean previewChat,
        boolean enforcesSecureChat
) implements ClientboundPacket {
    @Override
    public void serialize(ByteBuffer buffer) {
        Types.writeUtf8(buffer, computeJson());
    }

    @Override
    public int computeSize() {
        return Types.utf8Size(computeJson());
    }

    @Override
    public int id() {
        return 0;
    }

    private byte[] computeJson() {
        var faviconString = "data:image/png;base64," + Base64.getEncoder().encodeToString(favicon());

        var json = new JSONObject()
                .put("version", new JSONObject()
                        .put("name", version.name())
                        .put("protocol", version.protocol())
                )
                .put("players", new JSONObject()
                        .put("max", players.max())
                        .put("online", players.online())
                        .put("sample", new JSONArray())
                )
                .put("description", new JSONObject()
                        .put("text", descriptionText())
                )
                .put("favicon", faviconString)
                .put("previewsChat", previewChat())
                .put("enforcesSecureChat", enforcesSecureChat());

        return json.toString().getBytes(StandardCharsets.UTF_8);
    }

    public record Version(
            String name,
            int protocol
    ) {}

    // TODO: 2/23/23 implement sample array
    public record Players(
            int max,
            int online
    ) {}
}
