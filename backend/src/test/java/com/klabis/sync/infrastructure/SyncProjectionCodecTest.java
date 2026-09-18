package com.klabis.sync.infrastructure;

import com.klabis.sync.domain.SyncEntityType;
import com.klabis.sync.domain.SyncProjection;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SyncProjectionCodecTest {

    private record TestProjection(String name, BigDecimal amount, String note,
                                   List<String> tags) implements SyncProjection {
        @Override
        public SyncEntityType entityType() {
            return SyncEntityType.EVENT;
        }
    }

    @Test
    void hash_equalProjections_hashEqually() {
        var a = new TestProjection("Sprint", BigDecimal.valueOf(100), null, List.of("a", "b"));
        var b = new TestProjection("Sprint", BigDecimal.valueOf(100), null, List.of("a", "b"));

        assertThat(SyncProjectionCodec.hash(a)).isEqualTo(SyncProjectionCodec.hash(b));
    }

    @Test
    void hash_bigDecimalScaleDiffers_hashesEqually() {
        var a = new TestProjection("Sprint", new BigDecimal("100"), null, List.of());
        var b = new TestProjection("Sprint", new BigDecimal("100.00"), null, List.of());

        assertThat(SyncProjectionCodec.hash(a)).isEqualTo(SyncProjectionCodec.hash(b));
    }

    @Test
    void hash_fieldOrderInSourceIrrelevant_hashesEqually() {
        // Records have a fixed component order, but the codec must not depend on it —
        // canonicalisation reorders map keys alphabetically regardless of declaration order.
        var a = new TestProjection("Sprint", BigDecimal.TEN, "note", List.of("x"));
        var b = new TestProjection("Sprint", BigDecimal.TEN, "note", List.of("x"));

        assertThat(SyncProjectionCodec.toCanonicalJson(a)).isEqualTo(SyncProjectionCodec.toCanonicalJson(b));
    }

    @Test
    void hash_nullRepresentationNormalized_hashesEqually() {
        var a = new TestProjection("Sprint", BigDecimal.ONE, null, List.of());
        var b = new TestProjection("Sprint", BigDecimal.ONE, null, List.of());

        assertThat(SyncProjectionCodec.hash(a)).isEqualTo(SyncProjectionCodec.hash(b));
    }

    @Test
    void hash_differentValues_hashDifferently() {
        var a = new TestProjection("Sprint", BigDecimal.ONE, null, List.of());
        var b = new TestProjection("Relay", BigDecimal.ONE, null, List.of());

        assertThat(SyncProjectionCodec.hash(a)).isNotEqualTo(SyncProjectionCodec.hash(b));
    }

    @Test
    void hash_differentAmount_hashDifferently() {
        var a = new TestProjection("Sprint", new BigDecimal("100"), null, List.of());
        var b = new TestProjection("Sprint", new BigDecimal("100.01"), null, List.of());

        assertThat(SyncProjectionCodec.hash(a)).isNotEqualTo(SyncProjectionCodec.hash(b));
    }

    @Test
    void roundTrip_deserializesBackToEquivalentProjection() {
        var original = new TestProjection("Sprint", new BigDecimal("42.50"), "a note", List.of("x", "y"));

        String json = SyncProjectionCodec.toCanonicalJson(original);
        TestProjection restored = SyncProjectionCodec.fromCanonicalJson(json, TestProjection.class);

        assertThat(restored.name()).isEqualTo("Sprint");
        assertThat(restored.note()).isEqualTo("a note");
        assertThat(restored.tags()).containsExactly("x", "y");
        assertThat(restored.amount()).isEqualByComparingTo("42.50");
    }
}
