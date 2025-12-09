package org.okane.voyagemapper.service;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface WikidataService {

    // Example: GET https://www.wikidata.org/wiki/Special:EntityData/Q712311.json
    @GET("wiki/Special:EntityData/{id}.json")
    Call<ResponseBody> getEntity(@Path("id") String id);

    static WikidataService create() {
        OkHttpClient client = ApiClient.getHttpClient();

        return new Retrofit.Builder()
                .baseUrl("https://www.wikidata.org/")
                .client(client)
                .build()
                .create(WikidataService.class);
    }
}