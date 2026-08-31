package org.okane.voyagemapper.service;

import android.util.Log;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.Call;
import okhttp3.EventListener;

public class RequestEventListener extends EventListener {
    private static final AtomicInteger activeRequests = new AtomicInteger(0);

    @Override
    public void callStart(Call call) {
        int active = activeRequests.incrementAndGet();
        Log.d("HTTP_SOK_ACTIVE", "START active=" + active + " " + call.request().url());
    }

    @Override
    public void callEnd(Call call) {
        int active = activeRequests.decrementAndGet();
        Log.d("HTTP_SOK_ACTIVE", "END active=" + active + " " + call.request().url());
    }

    @Override
    public void callFailed(Call call, IOException ioe) {
        int active = activeRequests.decrementAndGet();
        Log.d("HTTP_SOK_ACTIVE", "FAILED active=" + active + " " + call.request().url());
    }
}
