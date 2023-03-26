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

package io.github.madethoughts.hope.configuration;

import io.github.madethoughts.hope.configuration.processor.AbstractConfig;
import io.github.madethoughts.hope.configuration.processor.Configuration;
import io.github.madethoughts.hope.configuration.processor.Transformer;
import net.kyori.adventure.text.Component;
import org.tomlj.TomlTable;

import java.nio.file.Files;
import java.nio.file.Path;

import static io.github.madethoughts.hope.configuration.processor.Transformers.MINI_MESSAGE;

@Configuration(value = "config.toml", version = 2)
public abstract class ServerConfig implements AbstractConfig {

    public static final Path FAVICON_PATH = Path.of("server-icon.png");

    private byte[] favicon;

    public byte[] favicon() {
        return favicon;
    }

    @Override
    public void load(TomlTable tomlTable) throws Exception {
        favicon = Files.exists(FAVICON_PATH)
                  ? Files.readAllBytes(FAVICON_PATH)
                  : new byte[0];
    }

    public abstract int maxPlayers();

    @Transformer(MINI_MESSAGE)
    public abstract Component motd();

    public abstract NetworkingConfig networking();
}
