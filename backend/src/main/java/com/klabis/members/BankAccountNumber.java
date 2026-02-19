package com.klabis.members;

import org.apache.commons.validator.routines.IBANValidator;
import org.jmolecules.ddd.annotation.ValueObject;

import java.util.regex.Pattern;

@ValueObject
public record BankAccountNumber(String value, AccountFormat format) {

    private static final Pattern DOMESTIC_FORMAT_PATTERN = Pattern.compile("^\\d+/\\d+$");
    private static final IBANValidator IBAN_VALIDATOR = IBANValidator.getInstance();

    public enum AccountFormat {
        IBAN,
        DOMESTIC
    }

    public BankAccountNumber {
        if (value == null || value.trim().isBlank()) {
            throw new IllegalArgumentException("Bank account number is required");
        }

        String normalized = normalize(value.trim());
        AccountFormat detectedFormat = detectFormat(normalized);
        validateFormat(normalized, detectedFormat);

        value = normalized;
        format = detectedFormat;
    }

    public static BankAccountNumber of(String value) {
        return new BankAccountNumber(value, null);
    }

    private static String normalize(String value) {
        return value.replaceAll("\\s+", "").toUpperCase();
    }

    private static AccountFormat detectFormat(String normalized) {
        if (DOMESTIC_FORMAT_PATTERN.matcher(normalized).matches()) {
            return AccountFormat.DOMESTIC;
        }

        if (normalized.length() >= 2 && Character.isLetter(normalized.charAt(0))) {
            return AccountFormat.IBAN;
        }

        throw new IllegalArgumentException("Cannot detect account format for: " + normalized);
    }

    private static void validateFormat(String value, AccountFormat format) {
        if (format == AccountFormat.IBAN) {
            if (!IBAN_VALIDATOR.isValid(value)) {
                throw new IllegalArgumentException("Invalid IBAN: " + value);
            }
        } else if (format == AccountFormat.DOMESTIC) {
            String[] parts = value.split("/");
            String accountNumber = parts[0];
            String bankCode = parts[1];

            if (bankCode.length() != 4) {
                throw new IllegalArgumentException("Invalid domestic format: " + value);
            }
            if (accountNumber.length() > 10) {
                throw new IllegalArgumentException("Invalid domestic format: " + value);
            }
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
