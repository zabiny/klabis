package com.klabis.common.jdbc;

import java.text.Normalizer;

public final class UnaccentFunction {

    private UnaccentFunction() {
    }

    public static String unaccent(String input) {
        if (input == null) {
            return null;
        }
        String decomposed = Normalizer.normalize(input, Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }
}
