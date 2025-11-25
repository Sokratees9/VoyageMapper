package org.okane.voyagemapper;


import retrofit2.Callback;

public class WikiRepository {
    private final WikiService api = WikiService.create();

    public void loadNearby(double lat, double lon, int radiusMeters, int limit, Callback<WikiResponse> cb) {
        String coord = lat + "|" + lon;
        api.nearby(
                "query", "json",
                "geosearch",
                coord,
                radiusMeters, limit, "all",
                "coordinates|pageimages|extracts|pageprops",
                "all", "thumbnail", 400,
                1, 1, "*"
        ).enqueue(cb);
    }

    // Convenience for your 20 km default
    public void loadNearby20km(double lat, double lon, Callback<WikiResponse> cb) {
        loadNearby(lat, lon, 20_000, 50, cb);
    }

    public void fetchPageWikitext(long pageId, retrofit2.Callback<PageContentResponse> cb) {
        api.pageContent(
                "query", "json",
                "revisions",
                "content",
                "main",
                2,
                pageId,
                "*"
        ).enqueue(cb);
    }
}
