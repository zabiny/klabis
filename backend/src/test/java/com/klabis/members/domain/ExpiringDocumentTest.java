package com.klabis.members.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ExpiringDocument Value Object Tests")
class ExpiringDocumentTest {

    @Test
    @DisplayName("Should create identity card document with valid data")
    void shouldCreateIdentityCardDocumentWithValidData() {
        // Given
        String cardNumber = "12345678";
        LocalDate validityDate = LocalDate.now().plusYears(5);

        // When
        ExpiringDocument<DocumentType> document = ExpiringDocument.of(
                DocumentType.IDENTITY_CARD,
                cardNumber,
                validityDate
        );

        // Then
        assertThat(document.documentType()).isEqualTo(DocumentType.IDENTITY_CARD);
        assertThat(document.number()).isEqualTo(cardNumber);
        assertThat(document.validityDate()).isEqualTo(validityDate);
    }

    @Test
    @DisplayName("Should create trainer license document with valid data")
    void shouldCreateTrainerLicenseDocumentWithValidData() {
        // Given
        String licenseNumber = "TL-2024-001";
        LocalDate validityDate = LocalDate.now().plusMonths(6);

        // When
        ExpiringDocument<DocumentType> document = ExpiringDocument.of(
                DocumentType.TRAINER_LICENSE,
                licenseNumber,
                validityDate
        );

        // Then
        assertThat(document.documentType()).isEqualTo(DocumentType.TRAINER_LICENSE);
        assertThat(document.number()).isEqualTo(licenseNumber);
        assertThat(document.validityDate()).isEqualTo(validityDate);
    }

    @Test
    @DisplayName("Should reject document with blank number")
    void shouldRejectDocumentWithBlankNumber() {
        // Given
        LocalDate validityDate = LocalDate.now().plusYears(1);

        // When & Then
        assertThatThrownBy(() -> ExpiringDocument.of(
                DocumentType.IDENTITY_CARD,
                "   ",
                validityDate
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Identity card number");
    }

    @Test
    @DisplayName("Should reject document with null number")
    void shouldRejectDocumentWithNullNumber() {
        // Given
        LocalDate validityDate = LocalDate.now().plusYears(1);

        // When & Then
        assertThatThrownBy(() -> ExpiringDocument.of(
                DocumentType.TRAINER_LICENSE,
                null,
                validityDate
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Trainer license number");
    }

    @Test
    @DisplayName("Should reject document with number exceeding max length")
    void shouldRejectDocumentWithNumberExceedingMaxLength() {
        // Given
        String tooLongNumber = "A".repeat(51); // MAX_DOCUMENT_NUMBER_LENGTH is 50
        LocalDate validityDate = LocalDate.now().plusYears(1);

        // When & Then
        assertThatThrownBy(() -> ExpiringDocument.of(
                DocumentType.IDENTITY_CARD,
                tooLongNumber,
                validityDate
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Identity card number");
    }

    @Test
    @DisplayName("Should reject document with null validity date")
    void shouldRejectDocumentWithNullValidityDate() {
        // Given
        String cardNumber = "12345678";

        // When & Then
        assertThatThrownBy(() -> ExpiringDocument.of(
                DocumentType.IDENTITY_CARD,
                cardNumber,
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Identity card validity date");
    }

    @Test
    @DisplayName("Should accept document with past validity date (used when reconstructing from persistence)")
    void shouldAcceptDocumentWithPastValidityDate() {
        // Given
        String cardNumber = "12345678";
        LocalDate pastDate = LocalDate.now().minusDays(1);

        // When & Then - past dates are allowed (e.g. when loading expired documents from DB)
        assertThatNoException().isThrownBy(() -> ExpiringDocument.of(
                DocumentType.IDENTITY_CARD,
                cardNumber,
                pastDate
        ));
    }

    @Test
    @DisplayName("Should accept document with today as validity date")
    void shouldAcceptDocumentWithTodayAsValidityDate() {
        // Given
        String cardNumber = "12345678";
        LocalDate today = LocalDate.now();

        // When
        ExpiringDocument<DocumentType> document = ExpiringDocument.of(
                DocumentType.IDENTITY_CARD,
                cardNumber,
                today
        );

        // Then
        assertThat(document.validityDate()).isEqualTo(today);
    }

    @Test
    @DisplayName("Should create identity card through constructor")
    void shouldCreateIdentityCardThroughConstructor() {
        // Given
        String cardNumber = "AB1234567";
        LocalDate validityDate = LocalDate.now().plusYears(3);

        // When
        ExpiringDocument<DocumentType> document = new ExpiringDocument<>(
                DocumentType.IDENTITY_CARD,
                cardNumber,
                validityDate
        );

        // Then
        assertThat(document.documentType()).isEqualTo(DocumentType.IDENTITY_CARD);
        assertThat(document.number()).isEqualTo(cardNumber);
        assertThat(document.validityDate()).isEqualTo(validityDate);
    }
}
