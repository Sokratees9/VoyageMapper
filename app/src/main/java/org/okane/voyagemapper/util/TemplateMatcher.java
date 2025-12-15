package org.okane.voyagemapper.util;

import androidx.annotation.NonNull;

import org.okane.voyagemapper.model.SeeListing;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TemplateMatcher {
    /**
     * @param name         see, do, marker, listing
     * @param body         the part after the first |
     * @param start        index of first '{'
     * @param end          index just AFTER final "}}"
     * @param sameLineTail text after }} on the same line
     */
    public record TemplateMatch(String name, String body, int start, int end, String sameLineTail) {
    }

    public static List<TemplateMatch> extractTemplates(String text) {
        List<TemplateMatch> out = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return out;
        }

        char[] cs = text.toCharArray();
        int n = cs.length;

        for (int i = 0; i < n - 1; i++) {
            // Look for "{{"
            if (cs[i] == '{' && cs[i + 1] == '{') {
                int start = i;
                int depth = 1;
                int j = i + 2;

                while (j < n - 1 && depth > 0) {
                    if (cs[j] == '{' && cs[j + 1] == '{') {
                        depth++;
                        j += 2;
                    } else if (cs[j] == '}' && cs[j + 1] == '}') {
                        depth--;
                        j += 2;
                    } else {
                        j++;
                    }
                }

                if (depth != 0) {
                    // Unbalanced braces; bail out
                    break;
                }

                int end = j; // position just AFTER the final "}}"
                String inner = text.substring(start + 2, end - 2); // inside {{ ... }}

                // Split name | body
                String name;
                String body;
                int pipe = inner.indexOf('|');
                if (pipe == -1) {
                    name = inner.trim();
                    body = "";
                } else {
                    name = inner.substring(0, pipe).trim();
                    body = inner.substring(pipe + 1); // may be multi-line
                }

                // Tail: text after }} up to end of line (no newline)
                int tailStart = end;
                int tailEnd = tailStart;
                while (tailEnd < n && cs[tailEnd] != '\n' && cs[tailEnd] != '\r') {
                    tailEnd++;
                }
                String tail = text.substring(tailStart, tailEnd).trim();

                out.add(new TemplateMatch(name, body, start, end, tail));

                i = end - 1; // jump ahead
            }
        }

        return out;
    }

    public static List<SeeListing> parse(String wikitext) {
        List<SeeListing> out = new ArrayList<>();
        if (wikitext == null || wikitext.isEmpty()) {
            return out;
        }

        List<TemplateMatch> templates = extractTemplates(wikitext);

        for (TemplateMatch tm : templates) {
            String tplName = tm.name.toLowerCase(Locale.ROOT);

            // Only see, do, or marker/listing type=see/do
            Map<String, String> params = parseParams(tm.body);
            if (!isSeeOrDo(tplName, params)) {
                continue;
            }

            Double lat = parseDouble(firstNonEmpty(params, "lat", "latitude"));
            Double lon = parseDouble(firstNonEmpty(params, "long", "lon", "longitude"));

            String name = firstNonEmpty(params, "name", "alt");
            if (name == null || name.isEmpty()) {
                name = "Sight";
            }

            String phone = firstNonEmpty(params, "phone", "tel");
            String url = firstNonEmpty(params, "url", "website");
            String address = firstNonEmpty(params, "address");
            String hours = firstNonEmpty(params, "hours");
            String price = firstNonEmpty(params, "price");
            String wiki = firstNonEmpty(params, "wikipedia");
            String image = firstNonEmpty(params, "image");
            String wikidata = firstNonEmpty(params, "wikidata");

            String content = firstNonEmpty(params, "content");
            if (content == null || content.trim().isEmpty()) {
                content = cleanWikiText(tm.sameLineTail); // tail after }}
            } else {
                content = cleanWikiText(content);
            }

            out.add(new SeeListing(
                    name, lat, lon, phone, url, content, address, hours, price, wiki, wikidata, image
            ));
        }

        return out;
    }

    private static boolean isSeeOrDo(String tplName, Map<String,String> params) {
        String t = tplName.toLowerCase(Locale.ROOT);
        if (t.equals("see") || t.equals("do")) {
            return true;
        }

        if (t.equals("marker") || t.equals("listing")) {
            String type = firstNonEmpty(params, "type");
            if (type != null) {
                String tt = type.trim().toLowerCase(Locale.ROOT);
                return tt.equals("see") || tt.equals("do");
            }
        }
        return false;
    }

    @SafeVarargs
    private static String firstNonEmpty(Map<String, String> map, String... keys) {
        for (String k : keys) {
            String v = map.get(k);
            if (v != null && !v.isEmpty()) {
                return v;
            }
        }
        return null;
    }

    private static String cleanWikiText(String s) {
        if (s == null) {
            return null;
        }
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

    private static Double parseDouble(String s) {
        if (s == null) {
            return null;
        }
        try {
            return Double.parseDouble(s);
        } catch (Exception e) {
            return null;
        }
    }

    static Map<String, String> parseParams(String body) {
        Map<String, String> map = new LinkedHashMap<>();
        if (body == null) {
            return map;
        }

        String s = body.replace("\r", "").trim();
        if (s.isEmpty()) {
            return map;
        }

        // Strip leading '|' (common in multiline templates)
        while (!s.isEmpty() && s.charAt(0) == '|') {
            s = s.substring(1).trim();
        }

        List<String> parts = getStrings(s);

        // Now interpret each part as either "key=value" or positional
        for (String raw : parts) {
            String part = raw.trim();
            if (part.isEmpty()) continue;

            int eq = part.indexOf('=');
            if (eq >= 0) {
                String key = part.substring(0, eq).trim().toLowerCase(Locale.ROOT);
                String val = part.substring(eq + 1).trim();
                if (!key.isEmpty()) {
                    map.put(key, val);
                }
            } else {
                // Positional param (rare in Wikivoyage see/marker usage) –
                // use as name if name not already present.
                if (!map.containsKey("name")) {
                    String name = part.replaceAll("''+", "").trim();
                    if (!name.isEmpty()) {
                        map.put("name", name);
                    }
                }
            }
        }
        return map;
    }

    @NonNull
    private static List<String> getStrings(String s) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        int n = s.length();
        int linkDepth = 0;     // inside [[ ... ]]
        int templateDepth = 0; // inside {{ ... }}

        for (int i = 0; i < n; i++) {
            char c = s.charAt(i);

            // Track {{ ... }} so we don't split on '|' inside templates
            if (c == '{' && i + 1 < n && s.charAt(i + 1) == '{') {
                templateDepth++;
                current.append(c);
                i++; // skip second '{'
                current.append(s.charAt(i));
                continue;
            } else if (c == '}' && i + 1 < n && s.charAt(i + 1) == '}') {
                if (templateDepth > 0) templateDepth--;
                current.append(c);
                i++; // skip second '}'
                current.append(s.charAt(i));
                continue;
            }

            // Track [[ ... ]] so we don't split on '|' inside wiki links
            if (c == '[' && i + 1 < n && s.charAt(i + 1) == '[') {
                linkDepth++;
                current.append(c);
                i++; // skip second '['
                current.append(s.charAt(i));
                continue;
            } else if (c == ']' && i + 1 < n && s.charAt(i + 1) == ']') {
                if (linkDepth > 0) linkDepth--;
                current.append(c);
                i++; // skip second ']'
                current.append(s.charAt(i));
                continue;
            }

            // Top-level '|' → new parameter
            if (c == '|' && linkDepth == 0 && templateDepth == 0) {
                parts.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }

        parts.add(current.toString());
        return parts;
    }
}
