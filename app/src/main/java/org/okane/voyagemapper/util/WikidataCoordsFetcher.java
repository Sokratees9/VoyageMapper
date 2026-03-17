package org.okane.voyagemapper.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONObject;
import org.okane.voyagemapper.log.AndroidLogger;
import org.okane.voyagemapper.log.AppLogger;
import org.okane.voyagemapper.log.NoOpLogger;
import org.okane.voyagemapper.service.WikidataService;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WikidataCoordsFetcher {

    private static final String TAG = "WikidataCoordsFetcher";

    static AppLogger LOGGER = new AndroidLogger();

    static void setLoggerForTests(AppLogger logger) {
        LOGGER = (logger != null) ? logger : new NoOpLogger();
    }

    private static final LatLng NO_COORDS = new LatLng(999, 999); // impossible value

    public interface CoordCallback {
        void onResult(@Nullable LatLng coords);
    }

    private final Map<String, LatLng> cacheWikidataLatLng = new ConcurrentHashMap<>();
    private final WikidataService service;

    public WikidataCoordsFetcher() {
        this(WikidataService.create());
    }

    WikidataCoordsFetcher(WikidataService service) {  // Visible for testing
        this.service = service;
    }
    public void fetchCoords(String wikidataId, CoordCallback callback) {
        if (wikidataId == null || wikidataId.isEmpty()) {
            callback.onResult(null);
            return;
        }

        // Cache hit?
        LatLng cached = cacheWikidataLatLng.get(wikidataId);
        if (cached != null) {
            callback.onResult(cached == NO_COORDS ? null : cached);
            return;
        }

        service.getEntity(wikidataId).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ResponseBody> call,
                    @NonNull Response<ResponseBody> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    cacheWikidataLatLng.put(wikidataId, NO_COORDS);  // remember failure
                    callback.onResult(null);
                    return;
                }

                try {
                    String json = response.body().string();
                    LatLng coords = parseP625(json, wikidataId);

                    if (coords == null) {
                        cacheWikidataLatLng.put(wikidataId, NO_COORDS);
                        callback.onResult(null);
                    } else {
                        cacheWikidataLatLng.put(wikidataId, coords);
                        callback.onResult(coords);
                    }
                } catch (Exception e) {
                    LOGGER.w(TAG, "Failed to parse Wikidata for " + wikidataId, e);
                    cacheWikidataLatLng.put(wikidataId, NO_COORDS);
                    callback.onResult(null);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                LOGGER.w(TAG, "Failed to fetch " + wikidataId, t);
                cacheWikidataLatLng.put(wikidataId, NO_COORDS);
                callback.onResult(null);
            }
        });
    }

    @Nullable
    LatLng parseP625(String json, String wikidataId) throws Exception {
        JSONObject root = new JSONObject(json);
        JSONObject entities = root.getJSONObject("entities");
        JSONObject entity = entities.getJSONObject(wikidataId);
        JSONObject claims = entity.getJSONObject("claims");
        JSONArray p625 = claims.optJSONArray("P625");
        if (p625 == null || p625.length() == 0) {
            LOGGER.d(TAG, "No P625 for " + wikidataId);
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
