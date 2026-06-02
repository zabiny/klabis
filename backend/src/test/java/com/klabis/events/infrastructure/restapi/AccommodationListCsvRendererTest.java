package com.klabis.events.infrastructure.restapi;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AccommodationListCsvRendererTest {

    private AccommodationListCsvRenderer renderer;

    @BeforeEach
    void setUp() {
        renderer = new AccommodationListCsvRenderer();
    }

    // --- Task 1.2 + 1.3: delimiter, header row, one row per member, combined address ---

    @Test
    void shouldRenderCsvWithSemicolonDelimiter() {
        var item = itemWithFullAddress();

        String csv = render(List.of(item));

        assertThat(csv).contains(";");
    }

    @Test
    void shouldRenderCzechHeaderRow() {
        String csv = render(List.of());

        assertThat(firstLine(csv)).isEqualTo("Jméno;Příjmení;Číslo OP;Platnost OP;Datum narození;Adresa");
    }

    @Test
    void shouldRenderOneRowPerMember() {
        var items = List.of(itemWithFullAddress(), itemWithFullAddress());

        String csv = render(items);

        assertThat(lines(csv)).hasSize(3); // header + 2 data rows
    }

    @Test
    void shouldMapFirstNameAndLastName() {
        var item = new AccommodationListItemDto(
                "Jana", "Novotná", "AB123456", LocalDate.of(2028, 12, 31),
                LocalDate.of(1990, 5, 15),
                "Hlavní 1", "Praha", "10000", "CZ"
        );

        String csv = render(List.of(item));

        String dataRow = lines(csv).get(1);
        assertThat(dataRow).startsWith("Jana;Novotná;");
    }

    @Test
    void shouldCombineAddressIntoSingleColumn() {
        var item = new AccommodationListItemDto(
                "Jana", "Novotná", "AB123456", LocalDate.of(2028, 12, 31),
                LocalDate.of(1990, 5, 15),
                "Hlavní 1", "Praha", "10000", "CZ"
        );

        String csv = render(List.of(item));

        String dataRow = lines(csv).get(1);
        // last column (address) contains comma-separated parts — delimiter is ; so no quoting needed
        assertThat(dataRow).endsWith(";Hlavní 1, 10000 Praha, CZ");
    }

    @Test
    void shouldOmitAddressPartsWhenNull() {
        var item = new AccommodationListItemDto(
                "Jana", "Novotná", "AB123456", LocalDate.of(2028, 12, 31),
                LocalDate.of(1990, 5, 15),
                null, "Praha", "10000", null
        );

        String csv = render(List.of(item));

        String dataRow = lines(csv).get(1);
        assertThat(dataRow).endsWith(";10000 Praha");
    }

    // --- Task 1.4 + 1.5: null values produce empty cells ---

    @Test
    void shouldRenderEmptyCellForMissingIdentityCardNumber() {
        var item = new AccommodationListItemDto(
                "Jana", "Novotná", null, null, null,
                "Hlavní 1", "Praha", "10000", "CZ"
        );

        String csv = render(List.of(item));

        String dataRow = lines(csv).get(1);
        // header: Jméno;Příjmení;Číslo OP;Platnost OP;Datum narození;Adresa
        // data:   Jana;Novotná;;;;<address>
        String[] cells = splitCsvRow(dataRow);
        assertThat(cells[2]).isEmpty(); // identityCardNumber
        assertThat(cells[3]).isEmpty(); // identityCardValidityDate
        assertThat(cells[4]).isEmpty(); // dateOfBirth
    }

    @Test
    void shouldNotRenderNeuvedenoForMissingValues() {
        var item = new AccommodationListItemDto(
                "Jana", "Novotná", null, null, null,
                "Hlavní 1", "Praha", "10000", "CZ"
        );

        String csv = render(List.of(item));

        assertThat(csv).doesNotContain("neuvedeno");
    }

    // --- Task 1.6: UTF-8 BOM prefix ---

    @Test
    void shouldStartWithUtf8Bom() {
        byte[] bytes = renderer.renderToBytes(List.of());

        assertThat(bytes[0]).isEqualTo((byte) 0xEF);
        assertThat(bytes[1]).isEqualTo((byte) 0xBB);
        assertThat(bytes[2]).isEqualTo((byte) 0xBF);
    }

    @Test
    void shouldStartWithBomCharacterInString() {
        String csv = render(List.of());

        assertThat(csv).startsWith("﻿");
    }

    // --- helpers ---

    private String render(List<AccommodationListItemDto> items) {
        return new String(renderer.renderToBytes(items), java.nio.charset.StandardCharsets.UTF_8);
    }

    private String firstLine(String csv) {
        // skip BOM if present
        String content = csv.startsWith("﻿") ? csv.substring(1) : csv;
        return content.lines().findFirst().orElse("");
    }

    private List<String> lines(String csv) {
        String content = csv.startsWith("﻿") ? csv.substring(1) : csv;
        return content.lines()
                .filter(line -> !line.isBlank())
                .toList();
    }

    private String[] splitCsvRow(String row) {
        // naive split on ; for rows without quoted semicolons — sufficient for test assertions
        return row.split(";", -1);
    }

    private AccommodationListItemDto itemWithFullAddress() {
        return new AccommodationListItemDto(
                "Jan", "Novák", "AB123456", LocalDate.of(2028, 12, 31),
                LocalDate.of(1985, 3, 20),
                "Náměstí 5", "Brno", "60200", "CZ"
        );
    }
}
