package org.okane.voyagemapper.util;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONObject;
import org.okane.voyagemapper.service.WikidataService;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WikidataCoordsFetcher {

    private static final String TAG = "WikidataCoordsFetcher";

    public interface CoordCallback {
        void onResult(@Nullable LatLng coords);
    }

    private final WikidataService service;

    // Simple thread-safe in-memory cache: Q-id -> LatLng (or null meaning “no coords”)
    private final Map<String, LatLng> cache = new ConcurrentHashMap<>();

    public WikidataCoordsFetcher() {
        this.service = WikidataService.create();
    }

    public void fetchCoords(String wikidataId, CoordCallback callback) {
        if (wikidataId == null || wikidataId.isEmpty()) {
            callback.onResult(null);
            return;
        }

        // Cache hit?
        if (cache.containsKey(wikidataId)) {
            callback.onResult(cache.get(wikidataId));
            return;
        }

        service.getEntity(wikidataId).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call,
                    @NonNull Response<ResponseBody> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    cache.put(wikidataId, null);  // remember failure
                    callback.onResult(null);
                    return;
                }

                try {
                    String json = response.body().string();
                    LatLng coords = parseP625(json, wikidataId);
                    cache.put(wikidataId, coords);  // may be null if no P625
                    callback.onResult(coords);
                } catch (Exception e) {
                    Log.w(TAG, "Failed to parse Wikidata for " + wikidataId, e);
                    cache.put(wikidataId, null);
                    callback.onResult(null);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                Log.w(TAG, "Failed to fetch " + wikidataId, t);
                cache.put(wikidataId, null);
                callback.onResult(null);
            }
        });
    }

    @Nullable
    private LatLng parseP625(String json, String wikidataId) throws Exception {
        JSONObject root = new JSONObject(json);
        JSONObject entities = root.getJSONObject("entities");
        JSONObject entity = entities.getJSONObject(wikidataId);
        JSONObject claims = entity.getJSONObject("claims");
        JSONArray p625 = claims.optJSONArray("P625");
        if (p625 == null || p625.length() == 0) {
            return null;
        }

        JSONObject firstClaim = p625.getJSONObject(0);
        JSONObject mainsnak = firstClaim.getJSONObject("mainsnak");
        JSONObject datavalue = mainsnak.getJSONObject("datavalue");
        JSONObject value = datavalue.getJSONObject("value");

        double lat = value.getDouble("latitude");
        double lon = value.getDouble("longitude");

        return new LatLng(lat, lon);
    }
}
