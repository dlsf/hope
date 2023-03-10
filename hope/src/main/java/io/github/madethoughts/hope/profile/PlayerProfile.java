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

package io.github.madethoughts.hope.profile;

import org.json.JSONObject;

import java.util.UUID;

public record PlayerProfile(
        UUID uuid,
        String name
) {
    public static PlayerProfile fromJson(String json) {
        var jsonObject = new JSONObject(json);
        var uuid = uuidFromHex(jsonObject.getString("id"));
        var name = jsonObject.getString("name");

        return new PlayerProfile(uuid, name);
    }

    public static UUID uuidFromHex(String hex) {
        // adjust uuid format
        var formattedUUID = hex.substring(0, 8) +
                            '-' +
                            hex.substring(8, 12) +
                            '-' +
                            hex.substring(12, 16) +
                            '-' +
                            hex.substring(16, 20) +
                            '-' +
                            hex.substring(20, 30);
        return UUID.fromString(formattedUUID);
    }
}
