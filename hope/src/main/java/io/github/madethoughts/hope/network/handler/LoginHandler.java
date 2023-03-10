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

package io.github.madethoughts.hope.network.handler;

import io.github.madethoughts.hope.network.Connection;
import io.github.madethoughts.hope.network.McCipher;
import io.github.madethoughts.hope.network.State;
import io.github.madethoughts.hope.network.packets.clientbound.login.EncryptionRequest;
import io.github.madethoughts.hope.network.packets.clientbound.login.LoginSuccess;
import io.github.madethoughts.hope.network.packets.serverbound.ServerboundPacket;
import io.github.madethoughts.hope.network.packets.serverbound.login.EncryptionResponse;
import io.github.madethoughts.hope.network.packets.serverbound.login.LoginStart;
import io.github.madethoughts.hope.profile.PlayerProfile;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class LoginHandler implements PacketHandler<ServerboundPacket.LoginPacket> {

    public static final HttpClient httpClient = HttpClient.newHttpClient();

    public static final String MOJANG_HASJOINED_URL =
            "https://sessionserver.mojang.com/session/minecraft/hasJoined?username=%s&serverId=%s";

    private final Connection connection;
    private LoginStart loginStart;

    public LoginHandler(Connection connection) {this.connection = connection;}

    @Override
    public void handle(ServerboundPacket.LoginPacket packet) {
        try {
            switch (packet) {
                case LoginStart start -> handleLoginStart(start);
                case EncryptionResponse response -> handleEncryptionResponse(response);
            }
        } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException | IOException | InvalidKeyException |
                 InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleEncryptionResponse(EncryptionResponse packet)
            throws InvalidAlgorithmParameterException, InvalidKeyException, NoSuchAlgorithmException, IOException,
            InterruptedException {
        var verifyToken = packet.decryptedVerifyToken();

        if (!Arrays.equals(verifyToken, McCipher.verifyToken)) {
            throw new AssertionError("Mismatched verify token");
        }

        var sharedSecret = packet.decryptedSharedValue();
        var secretKey = new SecretKeySpec(sharedSecret, McCipher.ENCRYPTION_FAMILY);

        connection.encryptor(new McCipher(secretKey, sharedSecret, Cipher.ENCRYPT_MODE));
        connection.decryptor(new McCipher(secretKey, sharedSecret, Cipher.DECRYPT_MODE));

        var playerProfile = sendJoinedRequest(sharedSecret);
        connection.playerProfile(playerProfile);

        connection.queuePacket(new LoginSuccess(playerProfile.uuid(), playerProfile.name()));
        connection.state(State.PLAY);
    }

    private void handleLoginStart(LoginStart packet) throws InterruptedException {
        loginStart = packet;
        connection.queuePacket(new EncryptionRequest(
                McCipher.serverKey.getPublic().getEncoded(),
                McCipher.verifyToken
        ));
    }

    private PlayerProfile sendJoinedRequest(byte[] sharedSecret)
            throws NoSuchAlgorithmException, IOException, InterruptedException {

        // compute hash
        var digest = MessageDigest.getInstance("SHA-1");
        digest.update(EncryptionRequest.SERVER_ID.getBytes(StandardCharsets.US_ASCII));
        digest.update(sharedSecret);
        digest.update(McCipher.serverKey.getPublic().getEncoded());
        var hash = new BigInteger(digest.digest()).toString(16);

        var request = HttpRequest.newBuilder()
                                 .GET()
                                 .uri(URI.create(MOJANG_HASJOINED_URL.formatted(loginStart.playerName(), hash)))
                                 .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return PlayerProfile.fromJson(response.body());
    }
}
