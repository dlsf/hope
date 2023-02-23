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

package io.github.madethoughts.hope.network.packets.deserialization;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TypesTest {

    @Test
    void testVarInt() throws Types.TypeDeserializationException {
        var bytes = new byte[]{(byte) 0b1000_1011, (byte) 0b0000_0011}; // 19

        var varInt = Types.varInt(ByteBuffer.wrap(bytes));
        assertEquals(395, varInt);
    }

    @Test
    void testString() throws Types.TypeDeserializationException {
        var string = "Test lol, 12345";
        var stringBytes = string.getBytes(StandardCharsets.UTF_8);
        var buffer = ByteBuffer.allocate(stringBytes.length + 1);
        buffer.put((byte) stringBytes.length);
        buffer.put(stringBytes);
        buffer.position(0);
        assertEquals(string, Types.string(buffer));
    }

    @Test
    void testUnsignedShort() {
        var i = 65535;
        var buffer = ByteBuffer.allocate(2);
        buffer.putShort((short) i);
        buffer.position(0);
        assertEquals(i, Types.unsignedShort(buffer));
    }
}
