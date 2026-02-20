package com.klabis.members.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
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

    // TODO: this needs to be fixed: it must be possible to create value object with date in past. It must not be possible to "EDIT" such date to past date (validation must be on different place)
    @Test
    @DisplayName("Should reject document with past validity date")
    void shouldRejectDocumentWithPastValidityDate() {
        // Given
        String cardNumber = "12345678";
        LocalDate pastDate = LocalDate.now().minusDays(1);

        // When & Then
        assertThatThrownBy(() -> ExpiringDocument.of(
                DocumentType.IDENTITY_CARD,
                cardNumber,
                pastDate
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Identity card validity date")
                .hasMessageContaining("past");
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

    @Test
    @DisplayName("Should be equal when all fields match")
    void shouldBeEqualWhenAllFieldsMatch() {
        // Given
        String cardNumber = "12345678";
        LocalDate validityDate = LocalDate.now().plusYears(2);

        ExpiringDocument<DocumentType> document1 = ExpiringDocument.of(
                DocumentType.IDENTITY_CARD,
                cardNumber,
                validityDate
        );
        ExpiringDocument<DocumentType> document2 = ExpiringDocument.of(
                DocumentType.IDENTITY_CARD,
                cardNumber,
                validityDate
        );

        // Then
        assertThat(document1).isEqualTo(document2);
        assertThat(document1.hashCode()).isEqualTo(document2.hashCode());
    }

    @Test
    @DisplayName("Should not be equal when document type differs")
    void shouldNotBeEqualWhenDocumentTypeDiffers() {
        // Given
        String number = "12345678";
        LocalDate validityDate = LocalDate.now().plusYears(2);

        ExpiringDocument<DocumentType> identityCard = ExpiringDocument.of(
                DocumentType.IDENTITY_CARD,
                number,
                validityDate
        );
        ExpiringDocument<DocumentType> trainerLicense = ExpiringDocument.of(
                DocumentType.TRAINER_LICENSE,
                number,
                validityDate
        );

        // Then
        assertThat(identityCard).isNotEqualTo(trainerLicense);
    }

    @Test
    @DisplayName("Should have correct toString representation")
    void shouldHaveCorrectToStringRepresentation() {
        // Given
        String cardNumber = "12345678";
        LocalDate validityDate = LocalDate.of(2026, 12, 31);

        ExpiringDocument<DocumentType> document = ExpiringDocument.of(
                DocumentType.IDENTITY_CARD,
                cardNumber,
                validityDate
        );

        // When
        String toString = document.toString();

        // Then
        assertThat(toString).contains("IDENTITY_CARD");
        assertThat(toString).contains(cardNumber);
        assertThat(toString).contains("2026-12-31");
    }
}
