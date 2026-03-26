package org.okane.voyagemapper.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class MediaWikiUtilsTest {

    @ParameterizedTest
    @CsvSource({
            // ---- Existing basics ----
            "'{{km|12}}', '12 km'",
            "'{{m|300}}', '300 m'",
            "'test string with no media wiki units', 'test string with no media wiki units'",
            "'a', 'a'",
            "'{{km|12}} {{m|300}}', '12 km 300 m'",

            // ---- Kilometer aliases + whitespace + case ----
            "'{{kilometer|12}}', '12 km'",
            "'{{kilometre|12}}', '12 km'",
            "'{{ KM | 12 }}', '12 km'",
            "'{{  kilometer  |  10-15  }}', '10-15 km'",
            // extra params ignored
            "'{{km|12|adj}}', '12 km'",
            "'{{kilometer|12|on}}', '12 km'",

            // ---- Meter aliases + whitespace + case ----
            "'{{meter|300}}', '300 m'",
            "'{{metre|300}}', '300 m'",
            "'{{ M | 300 }}', '300 m'",
            "'{{m|300|adj}}', '300 m'",

            // ---- Mile aliases ----
            "'{{mi|2}}', '2 mi'",
            "'{{mile|2}}', '2 mi'",
            "'{{miles|2}}', '2 mi'",
            "'{{ Mi | 2 }}', '2 mi'",
            "'{{mile|2|adj}}', '2 mi'",

            // ---- Feet / foot / ft ----
            "'{{ft|6}}', '6 ft'",
            "'{{foot|6}}', '6 ft'",
            "'{{feet|6}}', '6 ft'",
            "'{{ FT | 6 }}', '6 ft'",
            "'{{foot|6|adj}}', '6 ft'",

            // ---- Yard / yd ----
            "'{{yd|10}}', '10 yd'",
            "'{{yard|10}}', '10 yd'",
            "'{{yards|10}}', '10 yd'",
            "'{{ YD | 10 }}', '10 yd'",
            "'{{yard|10|adj}}', '10 yd'",

            // ---- Kilogram / kg ----
            "'{{kg|5}}', '5 kg'",
            "'{{kilogram|5}}', '5 kg'",
            "'{{kilograms|5}}', '5 kg'",
            "'{{ KG | 5 }}', '5 kg'",
            "'{{kg|5|adj}}', '5 kg'",

            // ---- Pound / lb ----
            "'{{lb|10}}', '10 lb'",
            "'{{pound|10}}', '10 lb'",
            "'{{pounds|10}}', '10 lb'",
            "'{{ LB | 10 }}', '10 lb'",
            "'{{pound|10|adj}}', '10 lb'",

            // ---- Hectare / ha ----
            "'{{ha|2}}', '2 ha'",
            "'{{hectare|2}}', '2 ha'",
            "'{{hectares|2}}', '2 ha'",
            "'{{ HA | 2 }}', '2 ha'",
            "'{{hectare|2|adj}}', '2 ha'",

            // ---- Square feet ----
            "'{{squarefeet|3000}}', '3000 ft²'",
            "'{{squarefoot|3000}}', '3000 ft²'",
            "'{{sqft|3000}}', '3000 ft²'",
            "'{{ft2|3000}}', '3000 ft²'",
            "'{{ SQFT | 3000 }}', '3000 ft²'",
            "'{{squarefeet|3000|adj}}', '3000 ft²'",

            // ---- Square meters ----
            "'{{squaremeter|50}}', '50 m²'",
            "'{{squaremetre|50}}', '50 m²'",
            "'{{sqm|50}}', '50 m²'",
            "'{{m2|50}}', '50 m²'",
            "'{{ SQM | 50 }}', '50 m²'",
            "'{{squaremeter|50|adj}}', '50 m²'",

            // ---- Square kilometers ----
            "'{{squarekilometer|10}}', '10 km²'",
            "'{{squarekilometre|10}}', '10 km²'",
            "'{{km2|10}}', '10 km²'",
            "'{{ KM2 | 10 }}', '10 km²'",
            "'{{squarekilometer|10|adj}}', '10 km²'",

            // ---- Square miles ----
            "'{{squaremile|6}}', '6 mi²'",
            "'{{mi2|6}}', '6 mi²'",
            "'{{ MI2 | 6 }}', '6 mi²'",
            "'{{squaremile|6|adj}}', '6 mi²'",

            // ---- Mixed units in one string ----
            "'Walk {{km|12}} then {{m|300}} and another {{mi|2}}.', 'Walk 12 km then 300 m and another 2 mi.'",

            // ---- Non-matching templates should stay unchanged ----
            "'{{unknown|12}}', '{{unknown|12}}'",
            "'{{km}}', '{{km}}'",                 // no first param => should not match your pattern
            "'{{km|}}', '{{km|}}'",               // empty first param => doesn't match [^|}]+
            "'{{km|12', '{{km|12'",               // malformed
            "'km|12}}', 'km|12}}'",               // malformed
    })
    void testExpandSimpleUnits(String input, String expected) {
        assertEquals(expected, MediaWikiUtils.expandSimpleUnits(input));
    }

    @Test
    void testExpandSimpleUnitsWithTabsAndNewLine() {
        assertEquals("12 km", MediaWikiUtils.expandSimpleUnits("{{km|\n 12 \n}}"));
        assertEquals("12 km", MediaWikiUtils.expandSimpleUnits("{{km|\n 12 \t}}"));
        assertEquals("12 km", MediaWikiUtils.expandSimpleUnits("{{km|\t 12 \t}}"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "  ", "\t", "\n"})
    void testNullOrEmptyExpandSimpleUnits(String input) {
        assertEquals(input, MediaWikiUtils.expandSimpleUnits(input));
    }

    @ParameterizedTest
    @CsvSource({
            "Londonderry, Derry",
            "londonderry, Derry",
            "Londonderry-based, Derry-based",
            "Londonderry's walls, Derry's walls",
            "lOnDoNdErRy, Derry",
            "Something else, Something else",
            ", "
    })
    void testFixDerry(String input, String expected) {
        assertEquals(expected, MediaWikiUtils.fixDerry(input, null));
        assertNull(MediaWikiUtils.fixDerry(null, null));
    }

    @ParameterizedTest
    @CsvSource({
            "Londonderry, Londonderry",
            "Londonderry-based, Londonderry-based",
            "Londonderry's walls, Londonderry's walls",
            "Something else, Something else",
            ", "
    })
    void testDoNotFixLondonDerryNewHampshire(String input, String expected) {
        assertEquals(expected, MediaWikiUtils.fixDerry(input, "Londonderry (New Hampshire)"));
    }
}
