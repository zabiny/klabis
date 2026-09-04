package com.klabis.sync.infrastructure;

import com.klabis.sync.domain.SyncHash;
import com.klabis.sync.domain.SyncProjection;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Canonical JSON serialisation of a {@link SyncProjection}, used both for storage
 * (the encrypted projection columns) and for hashing (design.md D3, D13).
 * <p>
 * Canonical means: stable field order (alphabetical, via
 * {@link MapperFeature#SORT_PROPERTIES_ALPHABETICALLY}) and normalised numeric
 * representation — a {@link BigDecimal} of {@code 100} and one of {@code 100.00} must
 * serialise identically, otherwise every pass would report a phantom change on amount
 * fields whenever the two sides parse into differently-scaled values. This is achieved
 * by writing every {@link BigDecimal} through {@link BigDecimal#stripTrailingZeros()}
 * (with a floor at scale 0, so {@code 0} does not become {@code 0E-18}).
 */
public final class SyncProjectionCodec {

    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    private SyncProjectionCodec() {
    }

    /**
     * Serialises a projection to its canonical JSON form.
     */
    public static String toCanonicalJson(SyncProjection projection) {
        return MAPPER.writeValueAsString(canonicalizedFields(projection));
    }

    /**
     * Deserialises canonical JSON back into a concrete projection type.
     */
    public static <T extends SyncProjection> T fromCanonicalJson(String json, Class<T> type) {
        return MAPPER.readValue(json, type);
    }

    /**
     * Breaks a projection into its named fields, for per-field divergence attribution
     * (design.md D14 response shape) — computed on the decrypted projection already
     * held in memory, never from anything stored (design.md D13). Numeric values are
     * canonicalised the same way as for hashing, so a field-level comparison does not
     * report a phantom change on differently-scaled amounts.
     */
    public static java.util.Map<String, Object> toFieldMap(SyncProjection projection) {
        return canonicalizedFields(projection);
    }

    /**
     * Hashes a projection over its whole canonical serialisation — never per field
     * (design.md D13): a per-field digest of a value from a small keyspace would be
     * brute-forceable.
     */
    public static SyncHash hash(SyncProjection projection) {
        String canonicalJson = toCanonicalJson(projection);
        return SyncHash.of(sha256Hex(canonicalJson));
    }

    /**
     * Converts a projection to its canonicalised field map once, so that JSON
     * serialisation, field-map extraction and hashing all work from the same
     * conversion instead of each independently calling {@code MAPPER.convertValue}
     * plus {@link #canonicalizeNumbers} (design.md D3, D13) — the result must stay
     * bit-identical to what each caller produced before, since the hash is persisted
     * and a divergent result here would cause false-positive conflicts.
     */
    @SuppressWarnings("unchecked")
    private static java.util.Map<String, Object> canonicalizedFields(SyncProjection projection) {
        return (java.util.Map<String, Object>) canonicalizeNumbers(MAPPER.convertValue(projection, Object.class));
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Object canonicalizeNumbers(Object value) {
        if (value instanceof BigDecimal decimal) {
            BigDecimal stripped = decimal.stripTrailingZeros();
            return stripped.scale() < 0 ? stripped.setScale(0) : stripped;
        }
        if (value instanceof java.util.Map<?, ?> map) {
            java.util.Map<String, Object> result = new java.util.TreeMap<>();
            for (var entry : map.entrySet()) {
                result.put(String.valueOf(entry.getKey()), canonicalizeNumbers(entry.getValue()));
            }
            return result;
        }
        if (value instanceof java.util.List<?> list) {
            return list.stream().map(SyncProjectionCodec::canonicalizeNumbers).toList();
        }
        return value;
    }
}
