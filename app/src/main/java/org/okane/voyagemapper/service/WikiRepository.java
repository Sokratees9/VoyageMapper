package org.okane.voyagemapper.service;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import retrofit2.Call;

public class WikiRepository {
    private final WikiService api = WikiService.create();

    private static final int NEARBY_LIMIT = 100;

    public void loadNearby(double lat, double lon, NearbyCallback cb) {
        String coordinates = lat + "|" + lon;
        Map<Long, WikiResponse.Page> accumulator = new LinkedHashMap<>();
        fetchNearbyPage(coordinates, accumulator, null, null, null, cb);
    }

    public void fetchPageWikitext(long pageId, retrofit2.Callback<PageContentResponse> cb) {
        api.pageContent(pageId).enqueue(cb);
    }

    private void fetchNearbyPage(
            String coord,
            Map<Long, WikiResponse.Page> accumulator,
            @Nullable String cont,
            @Nullable Integer excont,
            @Nullable Long picont,
            NearbyCallback callback) {

        Call<WikiResponse> call = api.nearby(
                coord,
                400,
                "max",                           // exlimit
                cont,                                   // continue
                excont,                                 // excontinue
                picont                                 // picontinue
        );

        call.enqueue(new retrofit2.Callback<>() {
            @Override
            public void onResponse(@NonNull Call<WikiResponse> c,
                    @NonNull retrofit2.Response<WikiResponse> res) {
                if (!res.isSuccessful() || res.body() == null) {
                    callback.onError(new RuntimeException("Bad response"));
                    return;
                }

                WikiResponse body = res.body();
                // Merge page extracts into accumulator
                if (body.query != null && body.query.pages != null && !body.query.pages.isEmpty()) {
                    for (WikiResponse.Page p : body.query.pages.values()) {
                        WikiResponse.Page cached = accumulator.get(p.pageid);

                        if (cached == null) {
                            // First time we see this page: store it as-is
                            accumulator.put(p.pageid, p);
                        } else {
                            // Only patch in extract if we're gaining information
                            boolean newHasExtract = p.extract != null && !p.extract.isBlank();
                            boolean oldHasExtract = cached.extract != null && !cached.extract.isBlank();

                            if (newHasExtract && !oldHasExtract) {
                                cached.extract = p.extract;
                            }
                        }
                    }
                } else {
                    callback.onSuccess(new ArrayList<>());
                    return;
                }

                // Stop if we've reached our overall limit
                if (accumulator.size() >= NEARBY_LIMIT) {
                    callback.onSuccess(new ArrayList<>(accumulator.values()));
                    return;
                }

                // Check continuation
                WikiResponse.ContinueInfo ci = body.cont;
                if (ci != null && (ci.excontinue != null || ci.picontinue != null)) {
                    // More extracts/pageimages to fetch
                    fetchNearbyPage(
                            coord,
                            accumulator,
                            ci.cont,          // same "continue" string
                            ci.excontinue,    // excontinue
                            ci.picontinue,    // picontinue
                            callback
                    );
                } else {
                    // No more continuation → we're done
                    callback.onSuccess(new ArrayList<>(accumulator.values()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<WikiResponse> c, @NonNull Throwable t) {
                callback.onError(t);
            }
        });
    }
}
