package com.klabis.common.text;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CzechCollation")
class CzechCollationTest {

    @Test
    @DisplayName("sorts according to Czech alphabet rules")
    void sortsByCzechAlphabet() {
        List<String> names = new ArrayList<>(List.of("Hrubý", "Dvořák", "Chalupa", "Čermák", "Dlouhý", "Cibulka", "Dlouhá"));

        names.sort(CzechCollation.comparator());

        assertThat(names).containsExactly("Cibulka", "Čermák", "Dlouhá", "Dlouhý", "Dvořák", "Hrubý", "Chalupa");
    }
}
