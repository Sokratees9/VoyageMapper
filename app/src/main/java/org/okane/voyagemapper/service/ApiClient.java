package org.okane.voyagemapper.service;

import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.util.concurrent.TimeUnit;

public final class ApiClient {

    private static final String USER_AGENT =
            "VoyageMap/1.0 (sokratees99@gmail.com)";

    private static OkHttpClient client;

    private ApiClient() {
        // no instances
    }

    public static OkHttpClient getHttpClient() {
        if (client == null) {
            synchronized (ApiClient.class) {
                if (client == null) {
                    client = buildClient();
                }
            }
        }
        return client;
    }

    private static OkHttpClient buildClient() {
        return new OkHttpClient.Builder()
                // Time to establish TCP connection
                .connectTimeout(15, TimeUnit.SECONDS)
                // Time waiting for server to send data
                .readTimeout(20, TimeUnit.SECONDS)
                // Time allowed for writing request body (small here, but set anyway)
                .writeTimeout(20, TimeUnit.SECONDS)
                // Total call budget (optional, but nice guardrail)
                .callTimeout(25, TimeUnit.SECONDS)
                // OkHttp already retries some connection failures
                .retryOnConnectionFailure(true)
                .addInterceptor(chain -> {
                    Request original = chain.request();
                    Request req = original.newBuilder()
                            .header("User-Agent", USER_AGENT)
                            .header("Accept", "application/json")
                            .build();
                    return chain.proceed(req);
                })
                .build();
    }
}