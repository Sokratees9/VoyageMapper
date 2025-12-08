package org.okane.voyagemapper.util;

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
//    private static final String WIKIDATA_API =
//            "https://www.wikidata.org/w/api.php?action=wbgetentities&props=claims&format=json&ids=";

    // Allow dependency injection for tests
//    private static OkHttpClient client = new OkHttpClient();

//    public static void setHttpClient(OkHttpClient client) {
//        WikidataHelper.client = client;
//    }

//    public static LatLng parseP625(String wikidataId, String json) {
//        try {
//            System.out.println("JSON = [" + json + "]");
//            JSONObject root = new JSONObject(json);
//            JSONObject entities = root.optJSONObject("entities");
//            if (entities == null) {
//                return null;
//            }
//
//            JSONObject entity = entities.optJSONObject(wikidataId);
//            if (entity == null) {
//                return null;
//            }
//
//            JSONObject claims = entity.optJSONObject("claims");
//            if (claims == null || !claims.has("P625")) return null;
//
//            JSONObject p625 = claims.getJSONArray("P625")
//                    .getJSONObject(0)
//                    .getJSONObject("mainsnak")
//                    .getJSONObject("datavalue")
//                    .getJSONObject("value");
//
//            double lat = p625.getDouble("latitude");
//            double lon = p625.getDouble("longitude");
//            return new LatLng(lat, lon);
//        } catch (Exception ex) {
//            return null;
//        }
//    }

//    public static void fetchCoordinates(String wikidataId, Consumer<LatLng> callback) {
//        if (wikidataId == null || wikidataId.isEmpty()) {
//            callback.accept(null);
//            return;
//        }
//
//        String url = WIKIDATA_API + wikidataId;
//        Request request = new Request.Builder()
//                .url(url)
//                .header("User-Agent", "VoyageMap/1.0 (sokratees99@gmail.com)")
//                .build();
//
//        client.newCall(request).enqueue(new Callback() {
//            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {
//                callback.accept(null);
//            }
//
//            @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
//                if (!response.isSuccessful()) {
//                    callback.accept(null);
//                    return;
//                }
//                String json = response.body().string();
//                callback.accept(parseP625(wikidataId, json));
//            }
//        });
//    }
}
