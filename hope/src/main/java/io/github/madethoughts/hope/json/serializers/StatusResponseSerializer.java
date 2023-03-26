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

package io.github.madethoughts.hope.json.serializers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import io.github.madethoughts.hope.network.packets.clientbound.status.StatusResponse;

import java.lang.reflect.Type;
import java.util.Base64;

public final class StatusResponseSerializer implements JsonSerializer<StatusResponse> {

    @Override
    public JsonElement serialize(StatusResponse src, Type typeOfSrc, JsonSerializationContext context) {
        var faviconString = "data:image/png;base64," + Base64.getEncoder().encodeToString(src.favicon());

        var json = new JsonObject();
        json.add("version", context.serialize(src.version()));
        json.add("players", context.serialize(src.players()));
        json.add("description", context.serialize(src.chat()));
        json.addProperty("favicon", faviconString);
        json.addProperty("previewsChat", src.previewChat());
        json.addProperty("enforcesSecureChat", src.enforcesSecureChat());
        return json;
    }

    public static final class PlayersSerializer implements JsonSerializer<StatusResponse.Players> {

        @Override
        public JsonElement serialize(StatusResponse.Players src, Type typeOfSrc, JsonSerializationContext context) {
            var json = new JsonObject();
            json.addProperty("max", src.max());
            json.addProperty("online", src.online());
            return json;
        }
    }

    public static final class VersionSerializer implements JsonSerializer<StatusResponse.Version> {

        @Override
        public JsonElement serialize(StatusResponse.Version src, Type typeOfSrc, JsonSerializationContext context) {
            var json = new JsonObject();
            json.addProperty("name", src.name());
            json.addProperty("protocol", src.protocol());
            return json;
        }
    }
}
