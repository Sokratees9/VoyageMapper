package org.okane.voyagemapper.util;

import androidx.annotation.Nullable;

import java.util.regex.Pattern;

public final class MediaWikiUtils {
    private MediaWikiUtils() {}

    @Nullable
    public static String fixDerry(@Nullable String text, @Nullable String pageTitle) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        // Skip normalization for this specific article
        if (pageTitle != null && pageTitle.equals("Londonderry (New Hampshire)")) {
            return text;
        }

        return text.replaceAll("(?i)Londonderry", "Derry");
    }

    /**
     * Best-effort: expands common Wikivoyage measurement templates into a
     * simple metric/imperial label,
     * WITHOUT doing any unit conversion (e.g. "{{km|12}}" -> "12 km").
     * Notes:
     * - Keeps only the first parameter.
     * - Ignores extra params like "|adj" or "|on".
     * - Works with whitespace variations and case.
     */
    public static String expandSimpleUnits(String text) {
        if (text == null || text.isEmpty()) return text;

        // Distance
        text = replaceTemplateFirstParam(text,
                new String[]{"km", "kilometer", "kilometre"},
                " km");
        text = replaceTemplateFirstParam(text,
                new String[]{"m", "meter", "metre"},
                " m");
        text = replaceTemplateFirstParam(text,
                new String[]{"mi", "mile", "miles"},
                " mi");
        text = replaceTemplateFirstParam(text,
                new String[]{"ft", "foot", "feet"},
                " ft");
        text = replaceTemplateFirstParam(text,
                new String[]{"yd", "yard", "yards"},
                " yd");

        // Mass
        text = replaceTemplateFirstParam(text,
                new String[]{"kg", "kilogram", "kilograms"},
                " kg");
        text = replaceTemplateFirstParam(text,
                new String[]{"lb", "pound", "pounds"},
                " lb");

        // Area
        text = replaceTemplateFirstParam(text,
                new String[]{"ha", "hectare", "hectares"},
                " ha");

        // Square feet
        text = replaceTemplateFirstParam(text,
                new String[]{"squarefeet", "squarefoot", "sqft", "ft2"},
                " ft²");

        // Square meters
        text = replaceTemplateFirstParam(text,
                new String[]{"squaremeter", "squaremetre", "sqm", "m2"},
                " m²");

        // Square kilometers
        text = replaceTemplateFirstParam(text,
                new String[]{"squarekilometer", "squarekilometre", "km2"},
                " km²");

        // Square miles
        text = replaceTemplateFirstParam(text,
                new String[]{"squaremile", "mi2"},
                " mi²");

        return text;
    }

    /**
     * Replaces {{<name>|<value>|...}} with "<value><suffix>" for any of the
     * provided template names.
     */
    private static String replaceTemplateFirstParam(String input, String[] templateNames, String suffix) {
        // Build a case-insensitive alternation group: (?i:(km|kilometer|kilometre))
        StringBuilder names = new StringBuilder();
        for (int i = 0; i < templateNames.length; i++) {
            if (i > 0) names.append("|");
            names.append(Pattern.quote(templateNames[i]));
        }

        // Match:
        //   {{  <name>  |  <firstParam>  ( | anything )?  }}
        //
        // First param: capture everything up to '|' or '}' (so ranges like 10-15 and commas work)
        String regex =
                "\\{\\{\\s*(?i:(" + names + "))\\s*\\|\\s*([^|}]+?)\\s*(?:\\|[^}]*)?\\}\\}";

        // Replace whole template with "firstParam + suffix"
        return input.replaceAll(regex, "$2" + suffix);
    }
}
