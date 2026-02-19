package com.klabis.config.encryption;

import com.klabis.common.encryption.EncryptedString;
import com.klabis.common.encryption.EncryptionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;

@Configuration
public class EncryptionConfiguration {

    @Value("${jasypt.encryptor.password}")
    private String encryptionPassword;
    @Value("${jasypt.encryptor.algorithm:PBEWithHmacSHA512AndAES_256}")
    private String algorithm;


    @Bean
    public EncryptionService sharedEncryptionService() {
        return new SharedEncryptionService(encryptionPassword, algorithm);
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
