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

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ThreadLocalRandom;

@SuppressWarnings("PublicStaticArrayField")
public final class McCipher {

    public static final String ENCRYPTION_FAMILY = "AES";
    public static final String ENCRYPTION = "AES/CFB8/NoPadding";
    public static final KeyPair serverKey;
    // must not be modified
    public static final byte[] verifyToken;

    static {
        try {
            var generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(1024);
            serverKey = generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            // should not occur
            throw new RuntimeException(e);
        }

        verifyToken = new byte[4];
        ThreadLocalRandom.current().nextBytes(verifyToken);
    }

    private final Cipher cipher;

    public McCipher(Key key, byte[] iv, int mode) throws InvalidAlgorithmParameterException, InvalidKeyException {
        try {
            cipher = Cipher.getInstance(ENCRYPTION);
            cipher.init(mode, key, new IvParameterSpec(iv));
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            // should never occur
            throw new AssertionError(e);
        }
    }

    public static byte[] decryptBytes(Key key, byte[] encrypted) {
        try {
            var cipher = Cipher.getInstance(key.getAlgorithm());
            cipher.init(Cipher.DECRYPT_MODE, key);
            return cipher.doFinal(encrypted);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException |
                 BadPaddingException e) {
            // TODO: 3/10/23 logging
            throw new RuntimeException(e);
        }
    }

    public void update(ByteBuffer buffer) {
        try {
            cipher.update(buffer.duplicate(), buffer);
        } catch (ShortBufferException e) {
            // should not occur
            throw new AssertionError(e);
        }
    }

}
