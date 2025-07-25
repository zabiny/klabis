package club.klabis.shared.config.authserver.generatejwtkeys;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

public class JKWKeyGenerator {
    public static void main(String[] args) throws IOException {
        generateNewJwkKeyForOauthServer();
    }

    // TODO: for PROD this must be replaced with keys which are not on GITHUB!
    public static final String AUTH_SERVER_JWK_KEYS_RESOURCE_PATH = "/authServer/oauth2_server_keys.jwk";
    public static final String AUTH_SERVER_JWK_KEYS_RESOURCE_FILE_PATH = "src/main/resources%s".formatted(AUTH_SERVER_JWK_KEYS_RESOURCE_PATH);

    public static void generateNewJwkKeyForOauthServer() throws IOException {

        File jwkFile = new File(AUTH_SERVER_JWK_KEYS_RESOURCE_FILE_PATH);

        FileOutputStream fos = new FileOutputStream(jwkFile);
        try (OutputStreamWriter writer = new OutputStreamWriter(fos)) {
            String jwkContent = loadFromFile().toString(false);
            System.out.println(jwkContent);
            writer.write(jwkContent);
            writer.flush();
        }
    }

    private static KeyPair generateRsaKey() {
        KeyPair keyPair;
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            keyPair = keyPairGenerator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
        return keyPair;
    }

    private static JWKSet loadFromFile() {
        KeyPair keyPair = generateRsaKey();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();

        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();

        return new JWKSet(rsaKey);
    }
}
