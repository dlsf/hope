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

package io.github.madethoughts.hope;

import java.io.IOException;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * The programms main entry
 */
public final class Application {

    private static final Logger log = Logger.getLogger(Application.class.getName());

    public static void main(String[] args) throws IOException {
        var loggingConfig = Application.class.getResourceAsStream("/logging.properties");
        LogManager.getLogManager().readConfiguration(loggingConfig);

        log.finest("test");

        try (var server = Server.setup()) {
            // something went wrong, error should be logged already
            if (server == null) return;
            server.run();
        } catch (Throwable e) {
            log.throwing(Application.class.getName(), "main", e);
        }
    }
}
