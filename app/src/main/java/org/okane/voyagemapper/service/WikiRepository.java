package org.okane.voyagemapper.service;


import retrofit2.Callback;

public class WikiRepository {
    private final WikiService api = WikiService.create();

    public void loadNearby(double lat, double lon, int radiusMeters, int limit, Callback<WikiResponse> cb) {
        String coordinates = lat + "|" + lon;
        api.nearby(coordinates, radiusMeters, limit, 100, 400).enqueue(cb);
    }

    // Convenience for your 20 km default
    public void loadNearby20km(double lat, double lon, Callback<WikiResponse> cb) {
        loadNearby(lat, lon, 20_000, 100, cb);
    }

    public void fetchPageWikitext(long pageId, retrofit2.Callback<PageContentResponse> cb) {
        api.pageContent(pageId).enqueue(cb);
    }
}
