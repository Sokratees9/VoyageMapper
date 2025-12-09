package org.okane.voyagemapper.service;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import okhttp3.OkHttpClient;

public interface WikiService {
    // Nearby / geosearch
    @GET("w/api.php?action=query" +
            "&format=json" +
            "&generator=geosearch" +
            "&ggsprimary=all" +
            "&prop=coordinates|pageimages|extracts|pageprops" +
            "&coprimary=all" +
            "&piprop=thumbnail" +
            "&exintro=1" +
            "&explaintext=1" +
            "&origin=*")
    Call<WikiResponse> nearby(
            @Query("ggscoord") String ggscoord,      // "lat|lon"
            @Query("ggsradius") int radiusMeters,    // 20000
            @Query("ggslimit") int limit,            // 100
            @Query("colimit") int colimit,           // 100
            @Query("pithumbsize") int thumbSize      // 400
    );

    // Page content / wikitext
    @GET("w/api.php?action=query" +
            "&format=json" +
            "&prop=revisions" +
            "&rvprop=content" +
            "&rvslots=main" +
            "&formatversion=2" +
            "&origin=*")
    Call<PageContentResponse> pageContent(
            @Query("pageids") long pageId
    );

    static WikiService create() {
        OkHttpClient client = ApiClient.getHttpClient();

        return new Retrofit.Builder()
                .baseUrl("https://en.wikivoyage.org/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build()
                .create(WikiService.class);
    }
}
