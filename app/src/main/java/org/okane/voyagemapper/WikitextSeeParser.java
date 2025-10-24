package org.okane.voyagemapper;

import java.util.*;
import java.util.regex.*;

public class WikitextSeeParser {

    // Finds blocks like {{see| name=... | lat=... | long=... }}
    // This assumes listings are not deeply nested. Works well for most pages.
    private static final Pattern SEE_BLOCK =
            Pattern.compile("\\{\\{\\s*(?i:see|do)\\s*\\|([^{}]*)\\}\\}", Pattern.DOTALL);

    public static List<SeeListing> parse(String wikitext) {
        List<SeeListing> out = new ArrayList<>();
        if (wikitext == null || wikitext.isEmpty()) return out;

        Matcher m = SEE_BLOCK.matcher(wikitext);
        while (m.find()) {
            String body = m.group(1);                 // the part after {{see|
            Map<String, String> params = parseParams(body);
            Double lat = parseDouble(firstNonEmpty(params, "lat", "latitude"));
            Double lon = parseDouble(firstNonEmpty(params, "long", "lon", "longitude"));
            if (lat == null || lon == null) {
                continue; // require coordinates
            }

            String name = firstNonEmpty(params, "name", "alt"); // name or alt text
            if (name == null || name.isEmpty()) name = "Sight";

            String phone = firstNonEmpty(params, "phone", "tel");
            String url = firstNonEmpty(params, "url", "website", "wikidata");
            String content = firstNonEmpty(params, "content");

            out.add(new SeeListing(name, lat, lon, phone, url, content));
        }
        return out;
    }

    private static Map<String, String> parseParams(String body) {
        Map<String, String> map = new LinkedHashMap<>();
        // split on | that are not inside brackets (simple heuristic)
        String[] parts = body.split("\\|");
        for (String p : parts) {
            int eq = p.indexOf('=');
            if (eq <= 0) continue;
            String key = p.substring(0, eq).trim().toLowerCase(Locale.ROOT);
            String val = p.substring(eq + 1).trim();
            // strip surrounding markup braces/quotes occasionally present
            val = val.replaceAll("^\\s*\"|\"\\s*$", "").trim();
            map.put(key, val);
        }
        return map;
    }

    @SafeVarargs
    private static String firstNonEmpty(Map<String, String> map, String... keys) {
        for (String k : keys) {
            String v = map.get(k);
            if (v != null && !v.isEmpty()) return v;
        }
        return null;
    }

    private static Double parseDouble(String s) {
        if (s == null) return null;
        try { return Double.parseDouble(s); } catch (Exception e) { return null; }
    }
}