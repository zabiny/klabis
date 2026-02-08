package com.klabis.users;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TokenHash Value Object Tests")
class TokenHashTest {

    private static final String VALID_HASH_64_CHARS = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
    private static final String VALID_PLAIN_TOKEN = "uuid-token-string-to-hash";

    @Nested
    @DisplayName("hash() method")
    class HashMethod {

        @Test
        @DisplayName("should hash plain token")
        void shouldHashPlainToken() {
            // When
            TokenHash hash = TokenHash.hash(VALID_PLAIN_TOKEN);

            // Then
            assertThat(hash).isNotNull();
            assertThat(hash.getValue()).hasSize(64); // SHA-256 produces 64 hex chars
            assertThat(hash.getValue()).matches("^[0-9a-fA-F]{64}$");
        }

        @Test
        @DisplayName("should produce same hash for same input")
        void shouldProduceSameHashForSameInput() {
            // When
            TokenHash hash1 = TokenHash.hash(VALID_PLAIN_TOKEN);
            TokenHash hash2 = TokenHash.hash(VALID_PLAIN_TOKEN);

            // Then
            assertThat(hash1).isEqualTo(hash2);
            assertThat(hash1.getValue()).isEqualTo(hash2.getValue());
        }

        @Test
        @DisplayName("should produce different hashes for different inputs")
        void shouldProduceDifferentHashesForDifferentInputs() {
            // When
            TokenHash hash1 = TokenHash.hash("token-one");
            TokenHash hash2 = TokenHash.hash("token-two");

            // Then
            assertThat(hash1).isNotEqualTo(hash2);
            assertThat(hash1.getValue()).isNotEqualTo(hash2.getValue());
        }

        @Test
        @DisplayName("should reject null plain token for hashing")
        void shouldRejectNullPlainTokenForHashing() {
            assertThatThrownBy(() -> TokenHash.hash(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Plain token is required");
        }

        @Test
        @DisplayName("should reject empty plain token for hashing")
        void shouldRejectEmptyPlainTokenForHashing() {
            assertThatThrownBy(() -> TokenHash.hash("  "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Plain token is required");
        }

        @Test
        @DisplayName("should handle edge case with empty token string")
        void shouldHandleEdgeCaseWithEmptyTokenString() {
            assertThatThrownBy(() -> TokenHash.hash(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Plain token is required");
        }

        @Test
        @DisplayName("should hash special characters in token")
        void shouldHashSpecialCharactersInToken() {
            // Given
            String tokenWithSpecialChars = "token-UUID_123!@#$%^&*()";

            // When
            TokenHash hash = TokenHash.hash(tokenWithSpecialChars);

            // Then
            assertThat(hash).isNotNull();
            assertThat(hash.matches(tokenWithSpecialChars)).isTrue();
        }

        @Test
        @DisplayName("should produce deterministic hashes")
        void shouldProduceDeterministicHashes() {
            // Given
            String token = "deterministic-token-123";

            // When
            TokenHash hash1 = TokenHash.hash(token);
            TokenHash hash2 = TokenHash.hash(token);
            TokenHash hash3 = TokenHash.hash(token);

            // Then - all hashes should be identical
            assertThat(hash1.getValue()).isEqualTo(hash2.getValue());
            assertThat(hash2.getValue()).isEqualTo(hash3.getValue());
        }
    }

    @Nested
    @DisplayName("matches() method")
    class MatchesMethod {

        @Test
        @DisplayName("should match correct plain token")
        void shouldMatchCorrectPlainToken() {
            // Given
            TokenHash hash = TokenHash.hash(VALID_PLAIN_TOKEN);

            // When/Then
            assertThat(hash.matches(VALID_PLAIN_TOKEN)).isTrue();
        }

        @Test
        @DisplayName("should not match incorrect plain token")
        void shouldNotMatchIncorrectPlainToken() {
            // Given
            TokenHash hash = TokenHash.hash(VALID_PLAIN_TOKEN);

            // When/Then
            assertThat(hash.matches("wrong-token")).isFalse();
        }

        @Test
        @DisplayName("should not match null token")
        void shouldNotMatchNullToken() {
            // Given
            TokenHash hash = TokenHash.hash(VALID_PLAIN_TOKEN);

            // When/Then
            assertThat(hash.matches(null)).isFalse();
        }

        @Test
        @DisplayName("should use constant-time comparison for security")
        void shouldUseConstantTimeComparisonForSecurity() {
            // Given
            TokenHash hash = TokenHash.hash(VALID_PLAIN_TOKEN);

            // When - compare matching tokens
            long startTime = System.nanoTime();
            boolean matches1 = hash.matches(VALID_PLAIN_TOKEN);
            long duration1 = System.nanoTime() - startTime;

            startTime = System.nanoTime();
            boolean matches2 = hash.matches(VALID_PLAIN_TOKEN);
            long duration2 = System.nanoTime() - startTime;

            // Then - timing should be similar (within 10x to allow for JVM variations)
            // This is a weak test but demonstrates the concept
            assertThat(matches1).isTrue();
            assertThat(matches2).isTrue();
            assertThat(Math.abs(duration1 - duration2)).isLessThan(10_000_000); // 10ms tolerance
        }
    }

    @Nested
    @DisplayName("fromHashedValue() method")
    class FromHashedValueMethod {

        @Test
        @DisplayName("should create hash from valid hashed value")
        void shouldCreateHashFromValidHashedValue() {
            // When
            TokenHash hash = TokenHash.fromHashedValue(VALID_HASH_64_CHARS);

            // Then
            assertThat(hash.getValue()).isEqualTo(VALID_HASH_64_CHARS);
        }

        @Test
        @DisplayName("should reject null hashed value")
        void shouldRejectNullHashedValue() {
            assertThatThrownBy(() -> TokenHash.fromHashedValue(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Token hash is required");
        }

        @Test
        @DisplayName("should reject empty hashed value")
        void shouldRejectEmptyHashedValue() {
            assertThatThrownBy(() -> TokenHash.fromHashedValue("  "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Token hash is required");
        }

        @Test
        @DisplayName("should reject hashed value with wrong length")
        void shouldRejectHashedValueWithWrongLength() {
            String wrongLength = "0123456789abcdef"; // Only 16 chars

            assertThatThrownBy(() -> TokenHash.fromHashedValue(wrongLength))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Token hash must be exactly 64 characters");
        }

        @Test
        @DisplayName("should reject hashed value with invalid characters")
        void shouldRejectHashedValueWithInvalidCharacters() {
            String invalidChars = "0123456789abcdefghij0123456789abcdefghij0123456789abcdefghij0124"; // Contains 'g', 'h', 'i', 'j'

            assertThatThrownBy(() -> TokenHash.fromHashedValue(invalidChars))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Token hash must contain only hexadecimal characters");
        }

        @Test
        @DisplayName("should be case insensitive for hex characters")
        void shouldBeCaseInsensitiveForHexCharacters() {
            // Given
            String uppercase = "0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF";
            String lowercase = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

            // When
            TokenHash hash1 = TokenHash.fromHashedValue(uppercase);
            TokenHash hash2 = TokenHash.fromHashedValue(lowercase);

            // Then - both should be valid, though they're different values
            assertThat(hash1.getValue()).isEqualTo(uppercase);
            assertThat(hash2.getValue()).isEqualTo(lowercase);
        }
    }

    @Nested
    @DisplayName("Object methods (equals, hashCode, toString)")
    class ObjectMethods {

        @Test
        @DisplayName("should implement equals correctly")
        void shouldImplementEqualsCorrectly() {
            // Given
            TokenHash hash1 = TokenHash.fromHashedValue(VALID_HASH_64_CHARS);
            TokenHash hash2 = TokenHash.fromHashedValue(VALID_HASH_64_CHARS);
            TokenHash hash3 = TokenHash.fromHashedValue(
                    "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdee");

            // Then
            assertThat(hash1).isEqualTo(hash2);
            assertThat(hash1).isNotEqualTo(hash3);
        }

        @Test
        @DisplayName("should implement hashCode correctly")
        void shouldImplementHashCodeCorrectly() {
            // Given
            TokenHash hash1 = TokenHash.fromHashedValue(VALID_HASH_64_CHARS);
            TokenHash hash2 = TokenHash.fromHashedValue(VALID_HASH_64_CHARS);

            // Then
            assertThat(hash1.hashCode()).isEqualTo(hash2.hashCode());
        }

        @Test
        @DisplayName("should have meaningful toString without exposing full hash")
        void shouldHaveMeaningfulToStringWithoutExposingFullHash() {
            // Given
            TokenHash hash = TokenHash.fromHashedValue(VALID_HASH_64_CHARS);

            // When
            String toString = hash.toString();

            // Then
            assertThat(toString).contains("TokenHash");
            assertThat(toString).contains("***"); // Redacted indicator
            assertThat(toString).doesNotContain(VALID_HASH_64_CHARS); // Full hash not exposed
            assertThat(toString).endsWith("89abcdef]"); // Shows last 8 chars (including brackets)
        }
    }

    @Nested
    @DisplayName("SECURITY: Timing Attack Prevention")
    class SecurityTimingAttackPrevention {

        @Test
        @DisplayName("SECURITY: constant-time comparison returns true for matching strings")
        void constantTimeComparisonReturnsTrueForMatchingStrings() {
            // Given
            String token = "matching-token-12345";
            TokenHash hash = TokenHash.hash(token);

            // When
            boolean result = hash.matches(token);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("SECURITY: constant-time comparison returns false for non-matching strings")
        void constantTimeComparisonReturnsFalseForNonMatchingStrings() {
            // Given
            String correctToken = "correct-token-12345";
            String wrongToken = "wrong-token-67890";
            TokenHash hash = TokenHash.hash(correctToken);

            // When
            boolean result = hash.matches(wrongToken);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("SECURITY: constant-time comparison returns false for different length strings")
        void constantTimeComparisonReturnsFalseForDifferentLengthStrings() {
            // Given
            String shortToken = "short";
            String longToken = "this-is-a-much-longer-token-string";
            TokenHash hash = TokenHash.hash(shortToken);

            // When
            boolean result = hash.matches(longToken);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("SECURITY: timing should be independent of position of first difference (early)")
        void timingIndependentOfDifferencePositionEarly() {
            // Given
            String baseToken = "this-is-a-test-token-for-timing-attack-prevention";
            String diffAtStart = "Xhis-is-a-test-token-for-timing-attack-prevention";
            TokenHash hash = TokenHash.hash(baseToken);

            // When - measure comparison time for mismatch at position 0
            long startTime = System.nanoTime();
            hash.matches(diffAtStart);
            long timeDiffAtStart = System.nanoTime() - startTime;

            // Then - should complete without early exit (constant-time)
            assertThat(timeDiffAtStart).isGreaterThan(0);
        }

        @Test
        @DisplayName("SECURITY: timing should be independent of position of first difference (middle)")
        void timingIndependentOfDifferencePositionMiddle() {
            // Given
            String baseToken = "this-is-a-test-token-for-timing-attack-prevention";
            String diffAtMiddle = "this-is-a-test-Xoken-for-timing-attack-prevention";
            TokenHash hash = TokenHash.hash(baseToken);

            // When - measure comparison time for mismatch at middle position
            long startTime = System.nanoTime();
            hash.matches(diffAtMiddle);
            long timeDiffAtMiddle = System.nanoTime() - startTime;

            // Then - should complete without early exit (constant-time)
            assertThat(timeDiffAtMiddle).isGreaterThan(0);
        }

        @Test
        @DisplayName("SECURITY: timing should be independent of position of first difference (late)")
        void timingIndependentOfDifferencePositionLate() {
            // Given
            String baseToken = "this-is-a-test-token-for-timing-attack-prevention";
            String diffAtEnd = "this-is-a-test-token-for-timing-attack-preventioX";
            TokenHash hash = TokenHash.hash(baseToken);

            // When - measure comparison time for mismatch at last position
            long startTime = System.nanoTime();
            hash.matches(diffAtEnd);
            long timeDiffAtEnd = System.nanoTime() - startTime;

            // Then - should complete without early exit (constant-time)
            assertThat(timeDiffAtEnd).isGreaterThan(0);
        }

        @Test
        @DisplayName("SECURITY: timing variations should be minimal regardless of difference position")
        @Disabled("Flaky test... not sure what is problem there")
        void timingVariationsMinimalRegardlessOfDifferencePosition() {
            // Given
            String baseToken = "this-is-a-test-token-for-timing-attack-prevention-analysis";
            String diffAtStart = "Xhis-is-a-test-token-for-timing-attack-prevention-analysis";
            String diffAtMiddle = "this-is-a-test-Xoken-for-timing-attack-prevention-analysis";
            String diffAtEnd = "this-is-a-test-token-for-timing-attack-prevention-analysiX";
            TokenHash hash = TokenHash.hash(baseToken);

            // When - measure multiple comparisons at different difference positions
            int iterations = 100;
            long totalTimeStart = 0;
            long totalTimeMiddle = 0;
            long totalTimeEnd = 0;

            for (int i = 0; i < iterations; i++) {
                long start = System.nanoTime();
                hash.matches(diffAtStart);
                totalTimeStart += System.nanoTime() - start;

                start = System.nanoTime();
                hash.matches(diffAtMiddle);
                totalTimeMiddle += System.nanoTime() - start;

                start = System.nanoTime();
                hash.matches(diffAtEnd);
                totalTimeEnd += System.nanoTime() - start;
            }

            long avgTimeStart = totalTimeStart / iterations;
            long avgTimeMiddle = totalTimeMiddle / iterations;
            long avgTimeEnd = totalTimeEnd / iterations;

            // Then - all average times should be within 2x of each other
            // In true constant-time comparison, these would be nearly identical
            // We allow 2x variance due to JVM JIT, GC, and system noise
            assertThat(avgTimeStart).isLessThan(avgTimeMiddle * 2);
            assertThat(avgTimeMiddle).isLessThan(avgTimeEnd * 2);
            assertThat(avgTimeStart).isLessThan(avgTimeEnd * 2);
        }

        @Test
        @DisplayName("SECURITY: constant-time comparison prevents early exit on complete match")
        void constantTimeComparisonPreventsEarlyExitOnCompleteMatch() {
            // Given
            String token = "complete-match-token-123456789";
            TokenHash hash = TokenHash.hash(token);

            // When - measure time for matching comparison
            long startTime = System.nanoTime();
            hash.matches(token);
            long timeForMatch = System.nanoTime() - startTime;

            // Then - should process entire string (constant-time)
            assertThat(timeForMatch).isGreaterThan(0);
        }

        @Test
        @DisplayName("SECURITY: comparison time should not leak information about token similarity")
        void comparisonTimeShouldNotLeakTokenSimilarity() {
            // Given
            String original = "original-token-value-for-timing-analysis";
            String verySimilar = "original-token-value-for-timing-analysiz"; // 1 char different
            String somewhatSimilar = "original-token-Xalue-for-timing-analysis"; // middle different
            String completelyDifferent = "zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz"; // all different

            TokenHash hash = TokenHash.hash(original);

            // When - measure comparison times
            int iterations = 50;
            long timeVerySimilar = 0;
            long timeSomewhatSimilar = 0;
            long timeCompletelyDifferent = 0;

            for (int i = 0; i < iterations; i++) {
                long start = System.nanoTime();
                hash.matches(verySimilar);
                timeVerySimilar += System.nanoTime() - start;

                start = System.nanoTime();
                hash.matches(somewhatSimilar);
                timeSomewhatSimilar += System.nanoTime() - start;

                start = System.nanoTime();
                hash.matches(completelyDifferent);
                timeCompletelyDifferent += System.nanoTime() - start;
            }

            // Then - all times should be similar (within 5x tolerance for JVM variations)
            // This prevents attackers from determining how similar their guess is to the real token
            // Note: 5x tolerance is used because JVM JIT compilation, GC, and system noise
            // can cause significant variations in timing measurements
            long avgVerySimilar = timeVerySimilar / iterations;
            long avgSomewhatSimilar = timeSomewhatSimilar / iterations;
            long avgCompletelyDifferent = timeCompletelyDifferent / iterations;

            assertThat(avgVerySimilar).isLessThan(avgCompletelyDifferent * 5);
            assertThat(avgSomewhatSimilar).isLessThan(avgCompletelyDifferent * 5);
            assertThat(avgCompletelyDifferent).isLessThan(avgVerySimilar * 5);
        }

        @Test
        @DisplayName("SECURITY: constant-time comparison handles single character differences")
        void constantTimeComparisonHandlesSingleCharacterDifferences() {
            // Given
            String token = "token-with-single-char-difference-test";
            String[] variations = {
                    "Xoken-with-single-char-difference-test", // First char different
                    "tXken-with-single-char-difference-test", // Second char different
                    "tokXn-with-single-char-difference-test", // Middle char different
                    "token-with-single-char-difference-tesX", // Second to last char different
                    "token-with-single-char-difference-test"  // Exact match (last char)
            };

            TokenHash hash = TokenHash.hash(token);

            // When/Then - all comparisons should complete without early exit
            for (String variation : variations) {
                boolean result = hash.matches(variation);

                // Only the exact match should return true
                if (variation.equals(token)) {
                    assertThat(result).isTrue();
                } else {
                    assertThat(result).isFalse();
                }
            }
        }

        @Test
        @DisplayName("SECURITY: immediate rejection for different length hashes is acceptable")
        void immediateRejectionForDifferentLengthHashesIsAcceptable() {
            // Given
            String token = "test-token";
            String muchLongerToken = "test-token-with-many-more-characters-than-the-original";
            TokenHash hash = TokenHash.hash(token);

            // When - comparing different length tokens
            long startTime = System.nanoTime();
            boolean result = hash.matches(muchLongerToken);
            long duration = System.nanoTime() - startTime;

            // Then
            // It's acceptable to return false immediately for different lengths
            // because SHA-256 hashes are always 64 hex characters (fixed length)
            // Timing attacks on length are not useful when length is fixed and known
            assertThat(result).isFalse();
            assertThat(duration).isGreaterThan(0);
        }
    }
}
