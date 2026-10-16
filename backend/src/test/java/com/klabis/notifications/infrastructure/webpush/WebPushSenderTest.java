package com.klabis.notifications.infrastructure.webpush;

import com.klabis.notifications.NotificationDeviceTestDataBuilder;
import com.klabis.notifications.PushMessage;
import com.klabis.notifications.application.VapidProperties;
import com.klabis.notifications.domain.NotificationDevice;
import com.klabis.notifications.domain.PushKeys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.Flow;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("WebPushSender")
class WebPushSenderTest {

    private final List<HttpRequest> requests = new ArrayList<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private int responseStatus = 201;
    private NotificationDevice device;
    private WebPushSender sender;
    private VapidProperties properties;

    @BeforeEach
    void setUp() throws Exception {
        KeyPair vapid = generateKeyPair();
        properties = new VapidProperties(base64Url(uncompressed((ECPublicKey) vapid.getPublic())),
                base64Url(rawScalar((ECPrivateKey) vapid.getPrivate())), "mailto:admin@example.org");

        KeyPair browser = generateKeyPair();
        byte[] authSecret = new byte[16];
        new SecureRandom().nextBytes(authSecret);
        device = NotificationDeviceTestDataBuilder.aDevice()
                .withEndpoint("https://push.example.org/send/abc")
                .withKeys(new PushKeys(base64Url(uncompressed((ECPublicKey) browser.getPublic())), base64Url(authSecret)))
                .build();

        sender = new WebPushSender(properties, request -> {
            requests.add(request);
            return responseStatus;
        }, objectMapper);
    }

    @Test
    @DisplayName("posts an aes128gcm encrypted VAPID-signed request with TTL to the device endpoint")
    void postsEncryptedSignedRequest() {
        int status = sender.send(device, new PushMessage("Notifikace fungují", "Tělo", "/notifications"));

        assertThat(status).isEqualTo(201);
        assertThat(requests).hasSize(1);
        HttpRequest request = requests.getFirst();
        assertThat(request.uri().toString()).isEqualTo("https://push.example.org/send/abc");
        assertThat(request.method()).isEqualTo("POST");
        assertThat(request.headers().firstValue("Content-Encoding")).contains("aes128gcm");
        assertThat(request.headers().firstValue("TTL")).contains("86400");
        assertThat(request.headers().firstValue("Urgency")).contains("normal");
        assertThat(request.headers().firstValue("Authorization").orElseThrow())
                .startsWith("vapid t=")
                .contains(", k=" + properties.publicKey());
    }

    @Test
    @DisplayName("does not leak the plain text payload")
    void bodyIsEncrypted() {
        sender.send(device, new PushMessage("UniqueTitleXyz", "UniqueBodyXyz", "/events/1"));

        String body = new String(bodyBytes(requests.getFirst()), StandardCharsets.ISO_8859_1);
        assertThat(body).doesNotContain("UniqueTitleXyz").doesNotContain("UniqueBodyXyz");
    }

    @Test
    @DisplayName("payload JSON carries title, body, url and a tag derived from the target path")
    void payloadContent() {
        String json = sender.payload(new PushMessage("Titulek", "Text", "/events/1"));

        var tree = objectMapper.readTree(json);
        assertThat(tree.get("title").asString()).isEqualTo("Titulek");
        assertThat(tree.get("body").asString()).isEqualTo("Text");
        assertThat(tree.get("url").asString()).isEqualTo("/events/1");
        assertThat(tree.get("tag").asString()).isEqualTo("/events/1");
    }

    @Test
    @DisplayName("falls back to the root url when the message has no target")
    void payloadWithoutTarget() {
        var tree = objectMapper.readTree(sender.payload(new PushMessage("Titulek", "Text", null)));

        assertThat(tree.get("url").asString()).isEqualTo("/");
        assertThat(tree.get("tag").asString()).isEqualTo("klabis");
    }

    @Test
    @DisplayName("keeps the payload below 2 KB")
    void payloadIsBounded() {
        String json = sender.payload(new PushMessage("č".repeat(5000), "ě".repeat(5000), "/events/1"));

        assertThat(json.getBytes(StandardCharsets.UTF_8).length).isLessThan(2048);
    }

    @Test
    @DisplayName("returns the push service status so callers can react to it")
    void returnsStatus() {
        responseStatus = 410;

        assertThat(sender.send(device, new PushMessage("t", "b", null))).isEqualTo(410);
    }

    @Test
    @DisplayName("never throws when the transport fails")
    void transportFailureIsSwallowed() {
        WebPushSender failing = new WebPushSender(
                properties,
                request -> {
                    throw new java.io.IOException("down");
                }, objectMapper);

        assertThat(failing.send(device, new PushMessage("t", "b", null))).isEqualTo(WebPushSender.FAILED);
    }

    private static byte[] bodyBytes(HttpRequest request) {
        var publisher = request.bodyPublisher().orElseThrow();
        var out = new java.io.ByteArrayOutputStream();
        publisher.subscribe(new Flow.Subscriber<>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(java.nio.ByteBuffer item) {
                byte[] chunk = new byte[item.remaining()];
                item.get(chunk);
                out.writeBytes(chunk);
            }

            @Override
            public void onError(Throwable throwable) {
            }

            @Override
            public void onComplete() {
            }
        });
        return out.toByteArray();
    }

    private static KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        return generator.generateKeyPair();
    }

    private static byte[] uncompressed(ECPublicKey key) {
        byte[] x = fixed(key.getW().getAffineX().toByteArray());
        byte[] y = fixed(key.getW().getAffineY().toByteArray());
        byte[] out = new byte[65];
        out[0] = 4;
        System.arraycopy(x, 0, out, 1, 32);
        System.arraycopy(y, 0, out, 33, 32);
        return out;
    }

    private static byte[] rawScalar(ECPrivateKey key) {
        return fixed(key.getS().toByteArray());
    }

    private static byte[] fixed(byte[] value) {
        if (value.length == 32) {
            return value;
        }
        if (value.length > 32) {
            return Arrays.copyOfRange(value, value.length - 32, value.length);
        }
        byte[] out = new byte[32];
        System.arraycopy(value, 0, out, 32 - value.length, value.length);
        return out;
    }

    private static String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
