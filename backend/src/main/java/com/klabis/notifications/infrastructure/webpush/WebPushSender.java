package com.klabis.notifications.infrastructure.webpush;

import com.klabis.notifications.PushMessage;
import com.klabis.notifications.application.VapidProperties;
import com.klabis.notifications.domain.NotificationDevice;
import com.zerodeplibs.webpush.PushSubscription;
import com.zerodeplibs.webpush.VAPIDKeyPair;
import com.zerodeplibs.webpush.VAPIDKeyPairs;
import com.zerodeplibs.webpush.httpclient.StandardHttpClientRequestPreparer;
import com.zerodeplibs.webpush.key.PrivateKeySources;
import com.zerodeplibs.webpush.key.PublicKeySources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;

import java.math.BigInteger;
import java.net.http.HttpRequest;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPrivateKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

class WebPushSender {

    enum Result {DELIVERED, GONE, FAILED}

    private static final Logger log = LoggerFactory.getLogger(WebPushSender.class);
    private static final String DEFAULT_TAG = "klabis";
    private static final int MAX_TITLE_LENGTH = 100;
    private static final int MAX_BODY_LENGTH = 400;

    private final VapidProperties properties;
    private final PushTransport transport;
    private final ObjectMapper objectMapper;
    private final VAPIDKeyPair vapidKeyPair;

    WebPushSender(VapidProperties properties, PushTransport transport, ObjectMapper objectMapper) {
        this.properties = properties;
        this.transport = transport;
        this.objectMapper = objectMapper;
        this.vapidKeyPair = VAPIDKeyPairs.of(
                PrivateKeySources.ofECPrivateKey(decodePrivateKey(properties.privateKey())),
                PublicKeySources.ofUncompressedBytes(Base64.getUrlDecoder().decode(properties.publicKey())));
    }

    Result send(NotificationDevice device, PushMessage message) {
        try {
            PushSubscription subscription = new PushSubscription();
            subscription.setEndpoint(device.getEndpoint().url());
            subscription.setKeys(new PushSubscription.Keys());
            subscription.getKeys().setP256dh(device.getKeys().p256dh());
            subscription.getKeys().setAuth(device.getKeys().auth());

            HttpRequest request = StandardHttpClientRequestPreparer.getBuilder()
                    .pushSubscription(subscription)
                    .vapidJWTExpiresAfter(12, TimeUnit.HOURS)
                    .vapidJWTSubject(properties.subject())
                    .pushMessage(payload(message))
                    .ttl(24, TimeUnit.HOURS)
                    .urgencyNormal()
                    .build(vapidKeyPair)
                    .toRequest();

            int status = transport.post(request);
            if (status >= 200 && status < 300) {
                return Result.DELIVERED;
            }
            if (status == 404 || status == 410) {
                return Result.GONE;
            }
            log.warn("Push service answered {} for device {}", status, device.getId());
            return Result.FAILED;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Push to device {} interrupted", device.getId());
            return Result.FAILED;
        } catch (Exception e) {
            log.warn("Push to device {} failed: {}", device.getId(), e.toString());
            return Result.FAILED;
        }
    }

    String payload(PushMessage message) {
        String target = message.targetPath();
        String url = Objects.requireNonNullElse(target, "/");
        String tag = Objects.requireNonNullElse(target, DEFAULT_TAG);
        return objectMapper.writeValueAsString(Map.of(
                "title", abbreviate(message.title(), MAX_TITLE_LENGTH),
                "body", abbreviate(message.body(), MAX_BODY_LENGTH),
                "url", url,
                "tag", tag));
    }

    private static String abbreviate(String text, int maxLength) {
        return text.length() <= maxLength ? text : text.substring(0, maxLength - 1) + "…";
    }

    private static ECPrivateKey decodePrivateKey(String base64Url) {
        try {
            AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC");
            parameters.init(new ECGenParameterSpec("secp256r1"));
            ECParameterSpec curve = parameters.getParameterSpec(ECParameterSpec.class);
            BigInteger scalar = new BigInteger(1, Base64.getUrlDecoder().decode(base64Url));
            return (ECPrivateKey) KeyFactory.getInstance("EC").generatePrivate(new ECPrivateKeySpec(scalar, curve));
        } catch (Exception e) {
            throw new IllegalStateException("Invalid VAPID private key", e);
        }
    }
}
