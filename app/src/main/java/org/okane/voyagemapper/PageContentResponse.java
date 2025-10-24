package org.okane.voyagemapper;

import java.util.List;

public class PageContentResponse {
    public Query query;

    public static class Query { public List<Page> pages; }

    public static class Page {
        public long pageid;
        public List<Revision> revisions;
    }

    public static class Revision {
        public Slots slots;
    }

    public static class Slots {
        public Content main;
    }

    public static class Content {
        public String content;   // wikitext here
    }
}
