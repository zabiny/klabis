package com.klabis.common.encryption;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;

@Configuration
public class EncryptionConfiguration {

    @Value("${klabis.encryption.password}")
    private String encryptionPassword;
    @Value("${klabis.encryption.salt}")
    private String encryptionSalt;

    @Bean
    public EncryptionService sharedEncryptionService() {
        return new SharedEncryptionService(encryptionPassword, encryptionSalt);
    }

    @Bean
    public Converter<EncryptedString, String> decryptionConverter() {
        return new EncryptedStringToStringConverter(sharedEncryptionService());
    }

    @Bean
    public Converter<String, EncryptedString> encryptionConverter() {
        return new StringToEncryptedStringConverter(sharedEncryptionService());
    }
}
