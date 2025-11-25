package org.okane.voyagemapper;

import okhttp3.Request;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import okhttp3.OkHttpClient;

public interface WikiService {
    @GET("w/api.php")
    Call<WikiResponse> nearby(
            @Query("action") String action,
            @Query("format") String format,
            @Query("generator") String generator,
            @Query("ggscoord") String ggscoord,
            @Query("ggsradius") int radiusMeters,
            @Query("ggslimit") int limit,
            @Query("ggsprimary") String primary,
            @Query("prop") String prop,
            @Query("coprimary") String coprimary,
            @Query("piprop") String piprop,
            @Query("pithumbsize") int thumbSize,
            @Query("exintro") int exintro,
            @Query("explaintext") int explaintxt,
            @Query("origin") String origin // add this param (see below)
    );

    @GET("w/api.php")
    Call<PageContentResponse> pageContent(
            @Query("action") String action,           // "query"
            @Query("format") String format,           // "json"
            @Query("prop") String prop,               // "revisions"
            @Query("rvprop") String rvprop,           // "content"
            @Query("rvslots") String rvslots,         // "main"
            @Query("formatversion") int formatVersion,// 2
            @Query("pageids") long pageId,
            @Query("origin") String origin            // "*"
    );

    static WikiService create() {
        String UA = "VoyageMapper/1.0 (http://noflyzone.o-kane.org; sokratees99@gmail.com)";
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request req = chain.request().newBuilder()
                            .header("User-Agent", UA)
                            .header("Api-User-Agent", UA)          // optional but recommended
                            .header("Accept", "application/json")   // nice-to-have
                            .build();
                    return chain.proceed(req);
                })
                .build();

        return new Retrofit.Builder()
                .baseUrl("https://en.wikivoyage.org/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build()
                .create(WikiService.class);
    }
}
