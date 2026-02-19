package com.klabis.config.encryption;

import com.klabis.common.encryption.EncryptedString;
import com.klabis.common.encryption.EncryptionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

/**
 * Spring Data JDBC ReadingConverter for decrypting sensitive string data
 * from database using Jasypt encryption.
 * <p>
 * Converts encrypted {@link String} from database to {@link EncryptedString} domain type.
 * <p>
 * Uses SharedEncryptionService to ensure consistent encryption/decryption.
 */
@ReadingConverter
class StringToEncryptedStringConverter implements Converter<String, EncryptedString> {

    private final EncryptionService encryptionService;

    public StringToEncryptedStringConverter(EncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    @Override
    public EncryptedString convert(String dbData) {
        if (dbData == null) {
            return null;
        }
        String decrypted = encryptionService.decrypt(dbData);
        return EncryptedString.of(decrypted);
    }
}
