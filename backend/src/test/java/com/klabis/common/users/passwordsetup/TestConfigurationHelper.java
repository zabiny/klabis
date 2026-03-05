package com.klabis.common.users.passwordsetup;

import com.klabis.common.ClubProperties;
import com.klabis.common.users.application.PasswordSetupProperties;

public class TestConfigurationHelper {

    public static final int DEFAULT_TOKEN_EXPIRATION_HOURS = 4;
    public static final String DEFAULT_BASE_URL = "https://localhost:8443";
    public static final String DEFAULT_CLUB_NAME = "Klabis";

    private TestConfigurationHelper() {
    }

    public static PasswordSetupProperties createDefaultPasswordSetupProperties() {
        return createPasswordSetupProperties(DEFAULT_TOKEN_EXPIRATION_HOURS, DEFAULT_BASE_URL);
    }

    public static PasswordSetupProperties createPasswordSetupProperties(int expirationHours, String baseUrl) {
        PasswordSetupProperties properties = new PasswordSetupProperties();
        PasswordSetupProperties.Token token = new PasswordSetupProperties.Token();
        token.setExpirationHours(expirationHours);
        properties.setToken(token);
        properties.setBaseUrl(baseUrl);
        return properties;
    }

    public static ClubProperties createDefaultClubProperties() {
        return createClubProperties(DEFAULT_CLUB_NAME);
    }

    public static ClubProperties createClubProperties(String clubName) {
        ClubProperties clubProperties = new ClubProperties();
        clubProperties.setName(clubName);
        clubProperties.setCode("ZBM");
        return clubProperties;
    }
}
