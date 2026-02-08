package com.klabis.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

/**
 * SSL configuration for integration tests.
 * <p>
 * This configuration disables SSL certificate validation for HTTPS connections
 * during testing, allowing tests to work with self-signed certificates.
 * </p>
 *
 * <p><b>Security Warning:</b> This configuration is ONLY for testing and should
 * NEVER be used in production. It trusts all certificates, including self-signed
 * and expired certificates.
 * </p>
 *
 * <p><b>Test Keystore:</b>
 * <ul>
 *   <li>Location: {@code classpath:keystore/test-keystore.p12}</li>
 *   <li>Password: {@code test1234}</li>
 *   <li>Alias: {@code localhost}</li>
 * </ul>
 * </p>
 *
 * <p><b>Usage:</b> This configuration is automatically applied when running tests
 * with the {@code test} profile. Tests can then make HTTPS requests to
 * {@code https://localhost:8443} without certificate validation errors.
 * </p>
 *
 * @see org.springframework.boot.web.client.RestTemplateBuilder
 * @see javax.net.ssl.SSLContext
 */
@TestConfiguration
@Profile("test")
public class TestSslConfiguration {

    /**
     * Creates a RestTemplate bean that trusts all SSL certificates.
     * <p>
     * This RestTemplate can make HTTPS requests to localhost:8443 with self-signed
     * certificates without throwing SSL validation exceptions.
     * </p>
     *
     * @return RestTemplate configured to trust all certificates
     * @throws RuntimeException if SSL context initialization fails
     */
    @Bean
    @Primary
    public RestTemplate testRestTemplate() {
        try {
            // Create SSL context that trusts all certificates
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{createTrustAllTrustManager()}, null);

            // Set default SSL context for all HTTPS connections
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());

            // Disable hostname verification for tests
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);

            return new RestTemplate();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create test RestTemplate with SSL configuration", e);
        }
    }

    /**
     * Creates a TrustManager that trusts all X.509 certificates.
     * <p>
     * <b>WARNING:</b> This is extremely insecure and should ONLY be used for testing.
     * All certificates are trusted without validation.
     * </p>
     *
     * @return TrustManager that trusts all certificates
     */
    private TrustManager createTrustAllTrustManager() {
        return new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) {
                // Trust all client certificates
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) {
                // Trust all server certificates
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        };
    }
}
