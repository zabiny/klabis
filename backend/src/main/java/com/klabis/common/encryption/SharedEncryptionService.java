package com.klabis.common.encryption;

import org.springframework.security.crypto.encrypt.AesGcmBytesEncryptor;
import org.springframework.security.crypto.encrypt.BytesEncryptor;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

class SharedEncryptionService implements EncryptionService {

    private final BytesEncryptor encryptor;

    SharedEncryptionService(String password, String hexSalt) {
        this.encryptor = AesGcmBytesEncryptor.withPassword(password, requireValidHexSalt(hexSalt)).build();
    }

    private static String requireValidHexSalt(String hexSalt) {
        boolean valid = hexSalt != null && !hexSalt.isEmpty() && HexFormat.isHexDigit(hexSalt.charAt(0))
                && hexSalt.length() % 2 == 0 && hexSalt.chars().allMatch(HexFormat::isHexDigit);
        if (!valid) {
            throw new IllegalArgumentException(
                    "klabis.encryption.salt must be a non-empty hex string with an even number of characters "
                            + "(generate one with: openssl rand -hex 16)");
        }
        return hexSalt;
    }

    @Override
    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        byte[] encrypted = encryptor.encrypt(plaintext.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(encrypted);
    }

    @Override
    public String decrypt(String encrypted) {
        if (encrypted == null) {
            return null;
        }
        byte[] decrypted = encryptor.decrypt(HexFormat.of().parseHex(encrypted));
        return new String(decrypted, StandardCharsets.UTF_8);
    }
}
