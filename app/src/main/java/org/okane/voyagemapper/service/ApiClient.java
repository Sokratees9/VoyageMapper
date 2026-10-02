package org.okane.voyagemapper.service;

import android.util.Log;

import org.okane.voyagemapper.BuildConfig;
import org.okane.voyagemapper.R;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class ApiClient {
    private static final AtomicInteger REQUEST_COUNT = new AtomicInteger(0);

    private static final String USER_AGENT =
            "VoyageMap/" + BuildConfig.VERSION_NAME + " (" + R.string.email + ")";

    private static final OkHttpClient client = buildClient();

    private ApiClient() {
        // no instances
    }

    public static OkHttpClient getHttpClient() {
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
                .eventListener(new RequestEventListener())
                .addInterceptor(chain -> {
                    int n = REQUEST_COUNT.incrementAndGet();
                    Request request = chain.request();
//                    Log.d("HTTP_SOK_COUNT", "#" + n + " " + request.method() + " " + request.url());
                    Response response = chain.proceed(request);
//                    Log.d("HTTP_SOK_COUNT", "#" + n + " -> " + response.code());
                    if (response.code() == 429) {
                        Log.w("HTTP_429",
                                "#" + n + " Retry-After=" + response.header("Retry-After"));
                    }
                    return response;
                })
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