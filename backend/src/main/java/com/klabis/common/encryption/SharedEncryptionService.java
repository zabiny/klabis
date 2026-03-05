package com.klabis.common.encryption;

import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.iv.RandomIvGenerator;

/**
 * Shared encryption service for consistent encryption/decryption operations.
 * Uses a single encryptor instance to ensure encrypted values can be decrypted.
 */
class SharedEncryptionService implements com.klabis.common.encryption.EncryptionService {

    private final PooledPBEStringEncryptor encryptor;

    public SharedEncryptionService(String encryptionPassword, String algorithm) {

        this.encryptor = new PooledPBEStringEncryptor();
        this.encryptor.setPoolSize(2);
        this.encryptor.setPassword(encryptionPassword);
        this.encryptor.setAlgorithm(algorithm);
        this.encryptor.setIvGenerator(new RandomIvGenerator());
        this.encryptor.setStringOutputType("BASE64");
    }

    @Override
    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        return encryptor.encrypt(plaintext);
    }

    @Override
    public String decrypt(String encrypted) {
        if (encrypted == null) {
            return null;
        }
        return encryptor.decrypt(encrypted);
    }
}
