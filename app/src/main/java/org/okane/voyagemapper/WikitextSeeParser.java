package org.okane.voyagemapper;

import java.util.*;
import java.util.regex.*;

public class WikitextSeeParser {

    // Bullet line: multi-line body + same-line tail (no newline allowed between '}}' and tail)
    // G1 = template name, G2 = body, G3 = same-line tail
    private static final Pattern BULLET_LISTING_MULTI =
            Pattern.compile("(?m)^\\s*[*#-]\\s*\\{\\{\\s*(?i:(see|do|marker))\\s*\\|([\\s\\S]*?)\\}\\}[ \\t]*(.*)$");


    // Fallback anywhere: multi-line body; G1 = template name, G2 = body (no tail)
    private static final Pattern ANY_SEE =
            Pattern.compile("\\{\\{\\s*(?i:(see|do|marker))\\s*\\|([\\s\\S]*?)\\}\\}", Pattern.DOTALL);

    public static List<SeeListing> parse(String wikitext) {
        List<SeeListing> out = new ArrayList<>();
        if (wikitext == null || wikitext.isEmpty()) return out;

        List<int[]> consumed = new ArrayList<>();

        // Pass 1: bullets with tail
        Matcher m = BULLET_LISTING_MULTI.matcher(wikitext);
        while (m.find()) {
            String tpl  = m.group(1);          // see|do|marker (captured)
            String body = m.group(2);          // params
            String tail = m.group(3).trim();   // prose after }}

            Map<String, String> params = parseParams(body);
            if (!isSeeOrDo(tpl, params)) continue;

            addListingFrom(params, tail, out);
            consumed.add(new int[]{m.start(), m.end()});
        }

        // Pass 2: anywhere (no tail)
        Matcher m2 = ANY_SEE.matcher(wikitext);
        while (m2.find()) {
            int s = m2.start(), e = m2.end();
            boolean overlap = false;
            for (int[] span : consumed) {
                if (!(e <= span[0] || s >= span[1])) { overlap = true; break; }
            }
            if (overlap) continue;

            String tpl = m2.group(1);
            String body = m2.group(2);
            Map<String, String> params = parseParams(body);
            if (!isSeeOrDo(tpl, params)) continue;

            addListingFrom(params, /*tail*/ "", out);
        }

        return out;
    }

    private static void addListingFrom(Map<String,String> params, String tail, List<SeeListing> out) {
        Double lat = parseDouble(firstNonEmpty(params, "lat", "latitude"));
        Double lon = parseDouble(firstNonEmpty(params, "long", "lon", "longitude"));
        if (lat == null || lon == null) return;

        String name    = firstNonEmpty(params, "name", "alt"); if (name == null || name.isEmpty()) name = "Sight";
        String phone   = firstNonEmpty(params, "phone", "tel");
        String url     = firstNonEmpty(params, "url", "website", "wikidata");
        String address = firstNonEmpty(params, "address");
        String hours   = firstNonEmpty(params, "hours");
        String price   = firstNonEmpty(params, "price");
        String wiki    = firstNonEmpty(params, "wikipedia");
        String image   = firstNonEmpty(params, "image");

        String content = firstNonEmpty(params, "content");
        content = (content == null || content.trim().isEmpty())
                ? cleanWikiText(tail)
                : cleanWikiText(content);

        out.add(new SeeListing(name, lat, lon, phone, url, content, address, hours, price, wiki, image));
    }


    private static boolean isSeeOrDo(String tplName, Map<String,String> params) {
        String t = tplName.toLowerCase(Locale.ROOT);
        if (t.equals("see") || t.equals("do")) return true;

        if (t.equals("marker")) {
            String type = firstNonEmpty(params, "type");
            if (type != null) {
                String tt = type.trim().toLowerCase(Locale.ROOT);
                return tt.equals("see") || tt.equals("do");
            }
            return false; // marker with no type → skip
        }
        return false;
    }

    private static String cleanWikiText(String s) {
        if (s == null) return null;
        String t = s;

        // [[Title|label]] → label ; [[Title]] → Title
        t = t.replaceAll("\\[\\[([^\\]|]+)\\|([^\\]]+)\\]\\]", "$2");
        t = t.replaceAll("\\[\\[([^\\]]+)\\]\\]", "$1");

        // External links: [http://... Label] → Label
        t = t.replaceAll("\\[(?:https?://[^\\s\\]]+)\\s+([^\\]]+)\\]", "$1");

        // Remove simple italics/bold markup
        t = t.replaceAll("''+", "");

        // Collapse whitespace
        t = t.replaceAll("\\s+", " ").trim();
        return t;
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