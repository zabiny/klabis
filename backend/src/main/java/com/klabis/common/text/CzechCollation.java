package com.klabis.common.text;

import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;

public final class CzechCollation {

    // JDK RuleBasedCollator.compare is synchronized, so one shared instance is safe.
    private static final Collator CZECH = Collator.getInstance(Locale.forLanguageTag("cs-CZ"));

    private CzechCollation() {
    }

    public static Comparator<String> comparator() {
        return CZECH::compare;
    }
}
