package org.okane.voyagemapper;

import java.util.List;
import java.util.Map;

public class WikiResponse {
    public Query query;
    public static class Query { public Map<String, Page> pages; }
    public static class Page {
        public long pageid;
        public String title;
        public String extract;
        public Thumbnail thumbnail;
        public List<Coordinate> coordinates;
    }
    public static class Thumbnail { public String source; public int width; public int height; }
    public static class Coordinate { public double lat; public double lon; }
}
