package com.danchi.common;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class Slugs {

    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");
    private static final Pattern EDGE_HYPHENS = Pattern.compile("(^-|-$)");

    private Slugs() {}

    public static String of(String value, String fallback) {
        String slug = of(value);
        return slug.isEmpty() ? fallback : slug;
    }

    public static String of(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        return EDGE_HYPHENS.matcher(NON_ALPHANUMERIC.matcher(normalized).replaceAll("-"))
                .replaceAll("");
    }
}
