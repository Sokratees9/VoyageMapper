package org.okane.voyagemapper.service;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface WikidataService {

    // Example: GET https://www.wikidata.org/wiki/Special:EntityData/Q712311.json
    @GET("wiki/Special:EntityData/{id}.json")
    Call<ResponseBody> getEntity(@Path("id") String id);

    // Batched Wikidata lookup
    @GET("w/api.php?action=wbgetentities&props=claims&format=json")
    Call<ResponseBody> getEntities(@Query("ids") String ids);

    static WikidataService create() {
        OkHttpClient client = ApiClient.getHttpClient();

        return new Retrofit.Builder()
                .baseUrl("https://www.wikidata.org/")
                .client(client)
                .build()
                .create(WikidataService.class);
    }
}