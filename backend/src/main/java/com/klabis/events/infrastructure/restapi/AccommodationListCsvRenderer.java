package com.klabis.events.infrastructure.restapi;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@org.springframework.stereotype.Component
class AccommodationListCsvRenderer {

    private static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    private static final CSVFormat CSV_FORMAT = CSVFormat.DEFAULT.builder()
            .setDelimiter(';')
            .setHeader("Jméno", "Příjmení", "Číslo OP", "Platnost OP", "Datum narození", "Adresa")
            .build();

    byte[] renderToBytes(List<AccommodationListItemDto> items) {
        var out = new ByteArrayOutputStream();
        out.writeBytes(UTF8_BOM);
        try (var writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
             var printer = new CSVPrinter(writer, CSV_FORMAT)) {
            for (AccommodationListItemDto item : items) {
                printer.printRecord(
                        item.firstName(),
                        item.lastName(),
                        item.identityCardNumber(),
                        Objects.toString(item.identityCardValidityDate(), null),
                        Objects.toString(item.dateOfBirth(), null),
                        formatAddress(item)
                );
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toByteArray();
    }

    private static String formatAddress(AccommodationListItemDto item) {
        List<String> parts = new ArrayList<>();
        if (item.addressStreet() != null) {
            parts.add(item.addressStreet());
        }
        String cityLine = buildCityLine(item.addressPostalCode(), item.addressCity());
        if (cityLine != null) {
            parts.add(cityLine);
        }
        if (item.addressCountry() != null) {
            parts.add(item.addressCountry());
        }
        return parts.isEmpty() ? null : String.join(", ", parts);
    }

    private static String buildCityLine(String postalCode, String city) {
        if (postalCode != null && city != null) {
            return postalCode + " " + city;
        }
        if (postalCode != null) {
            return postalCode;
        }
        return city;
    }
}
