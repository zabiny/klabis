package com.klabis.common.encryption;

public interface EncryptionService {
    String encrypt(String plaintext);

    String decrypt(String encrypted);
}
