package org.okane.voyagemapper.service;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

public class WikiResponse {
    public Query query;

    @SerializedName("continue")
    public ContinueInfo cont;
    public static class Query { public Map<String, Page> pages; }
    public static class Page {
        public long pageid;
        public String title;
        public String extract;
        public Thumbnail thumbnail;
        public List<Coordinate> coordinates;

        // Add this for Wikidata linkage
        public PageProps pageprops;

        public static class PageProps {
            @SerializedName("wikibase_item")
            public String wikibaseItem;
        }
    }
    public static class Thumbnail {
        public String source;
        public int width;
        public int height;
    }
    public static class Coordinate {
        public double lat;
        public double lon;
    }

    // NEW
    public static class ContinueInfo {
        @SerializedName("continue")
        public String cont;       // the "||coordinates|pageprops" bit

        public Integer excontinue;
        public Long picontinue;
    }
}
