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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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

    private final Map<String, List<CoordCallback>> pending = new ConcurrentHashMap<>();
    private final ScheduledExecutorService batchExecutor = Executors.newSingleThreadScheduledExecutor();
    private final AtomicBoolean batchScheduled = new AtomicBoolean(false);
    private static final long BATCH_DELAY_MS = 50L;
    private static final int MAX_BATCH_SIZE = 25;

    private final Map<String, LatLng> cacheWikidataLatLng = new ConcurrentHashMap<>();
    private final WikidataService service;

    public interface CoordCallback {
        void onResult(@Nullable LatLng coords);
    }

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

        LatLng cached = cacheWikidataLatLng.get(wikidataId);
        if (cached != null) {
            callback.onResult(cached == NO_COORDS ? null : cached);
            return;
        }

        /*
         * Several callers may request the same Wikidata ID before
         * the batch is sent. Keep all their callbacks together.
         */
        pending.compute(wikidataId, (id, callbacks) -> {
                if (callbacks == null) {
                    callbacks = Collections.synchronizedList(new ArrayList<>());
                }
                callbacks.add(callback);
                return callbacks;
        });
        LOGGER.d(TAG, "Queued " + wikidataId + "; pending IDs=" + pending.size());
        scheduleBatch();
    }

    private void scheduleBatch() {
        if (!batchScheduled.compareAndSet(false, true)) {
            return;
        }

        batchExecutor.schedule(
                this::sendBatch,
                BATCH_DELAY_MS,
                TimeUnit.MILLISECONDS
        );
    }

    private void sendBatch() {
        batchScheduled.set(false);
        List<String> allIds = new ArrayList<>(pending.keySet());
        if (allIds.isEmpty()) {
            return;
        }

        final List<String> ids;
        if (allIds.size() > MAX_BATCH_SIZE) {
            ids = new ArrayList<>(allIds.subList(0, MAX_BATCH_SIZE));
        } else {
            ids = allIds;
        }

        String joinedIds = String.join("|", ids);
        LOGGER.d(TAG, "Sending Wikidata batch of " + ids.size() + " IDs: " + joinedIds);
        service.getEntities(joinedIds).enqueue(new Callback<>() {
            @Override
            public void onResponse(
                    @NonNull Call<ResponseBody> call,
                    @NonNull Response<ResponseBody> response) {

                ResponseBody body = response.body();
                if (!response.isSuccessful() || body == null) {
                    completeBatchWithFailure(ids);
                    scheduleAnotherBatchIfNeeded();
                    return;
                }

                try (ResponseBody responseBody = body) {
                    String json = responseBody.string();
                    for (String id : ids) {
                        LatLng coords = parseP625(json, id);
                        cacheWikidataLatLng.put(id, Objects.requireNonNullElse(coords, NO_COORDS));
                        complete(id, coords);
                    }
                } catch (Exception e) {
                    LOGGER.w(TAG, "Failed to parse Wikidata batch", e);
                    completeBatchWithFailure(ids);
                }
                scheduleAnotherBatchIfNeeded();
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                LOGGER.w(TAG, "Failed to fetch Wikidata batch", t);
                completeBatchWithFailure(ids);
                scheduleAnotherBatchIfNeeded();
            }
        });
    }

    private void complete(String wikidataId, @Nullable LatLng coords) {
        List<CoordCallback> callbacks = pending.remove(wikidataId);
        if (callbacks == null) {
            return;
        }
        for (CoordCallback callback : callbacks) {
            callback.onResult(coords);
        }
    }

    private void completeBatchWithFailure(List<String> ids) {
        for (String id : ids) {
            cacheWikidataLatLng.put(id, NO_COORDS);
            complete(id, null);
        }
    }

    private void scheduleAnotherBatchIfNeeded() {
        if (!pending.isEmpty()) {
            scheduleBatch();
        }
    }

    @Nullable
    LatLng parseP625(String json, String wikidataId) throws Exception {
        JSONObject root = new JSONObject(json);
        JSONObject entities = root.optJSONObject("entities");
        if (entities == null) {
            return null;
        }

        JSONObject entity = entities.optJSONObject(wikidataId);
        if (entity == null) {
            return null;
        }

        JSONObject claims = entity.optJSONObject("claims");
        if (claims == null) {
            return null;
        }

        JSONArray p625 = claims.optJSONArray("P625");
        if (p625 == null || p625.length() == 0) {
            LOGGER.d(TAG, "No P625 for " + wikidataId);
            return null;
        }

        JSONObject firstClaim = p625.getJSONObject(0);
        JSONObject mainsnak = firstClaim.optJSONObject("mainsnak");
        if (mainsnak == null) {
            return null;
        }

        JSONObject datavalue = mainsnak.optJSONObject("datavalue");
        if (datavalue == null) {
            return null;
        }

        JSONObject value = datavalue.optJSONObject("value");
        if (value == null) {
            return null;
        }

        double lat = value.getDouble("latitude");
        double lon = value.getDouble("longitude");
        return new LatLng(lat, lon);
    }
}
