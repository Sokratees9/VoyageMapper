package org.okane.voyagemapper;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONObject;

import java.io.IOException;
import java.util.function.Consumer;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class WikidataHelper {
    private static final String WIKIDATA_API =
            "https://www.wikidata.org/w/api.php?action=wbgetentities&props=claims&format=json&ids=";

    private final OkHttpClient client = new OkHttpClient();

    /** Fetches P625 (lat/lon) from Wikidata and fills the item if found */
    public void fetchCoordinates(String wikidataId, Consumer<LatLng> callback) {
        if (wikidataId == null || wikidataId.isEmpty()) {
            callback.accept(null);
            return;
        }

        String url = WIKIDATA_API + wikidataId;
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "VoyageMap/1.0 (sokratees99@gmail.com)")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.accept(null);
            }

            @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.accept(null);
                    return;
                }

                try {
                    JSONObject root = new JSONObject(response.body().string());
                    JSONObject entities = root.optJSONObject("entities");
                    if (entities == null) { callback.accept(null); return; }

                    JSONObject entity = entities.optJSONObject(wikidataId);
                    JSONObject claims = entity != null ? entity.optJSONObject("claims") : null;
                    if (claims == null || !claims.has("P625")) { callback.accept(null); return; }

                    JSONObject p625 = claims.getJSONArray("P625")
                            .getJSONObject(0)
                            .getJSONObject("mainsnak")
                            .getJSONObject("datavalue")
                            .getJSONObject("value");

                    double lat = p625.getDouble("latitude");
                    double lon = p625.getDouble("longitude");
                    callback.accept(new LatLng(lat, lon));
                } catch (Exception ex) {
                    callback.accept(null);
                }
            }
        });
    }
}
