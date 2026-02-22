package com.klabis.common.encryption;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

/**
 * Spring Data JDBC Converter for encrypting sensitive string data
 * using Jasypt encryption.
 * <p>
 * This converter ensures that sensitive data is encrypted at rest in the database
 * while maintaining transparent encryption/decryption for the application.
 * <p>
 * Converts {@link EncryptedString} domain type to encrypted {@link String} for database storage.
 *
 * Uses SharedEncryptionService to ensure consistent encryption/decryption.
 */
@WritingConverter
class EncryptedStringToStringConverter implements Converter<EncryptedString, String> {

    private final EncryptionService encryptionService;

    public EncryptedStringToStringConverter(EncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    @Override
    public String convert(EncryptedString source) {
        if (source == null) {
            return null;
        }
        return encryptionService.encrypt(source.value());
    }
}
