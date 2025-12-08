package org.okane.voyagemapper.util;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Callback;
import okio.Timeout;

public class FakeCall implements Call {
    private final Request request;
    private final Response response;
    private boolean executed = false;

    FakeCall(Request request, Response response) {
        this.request = request;
        this.response = response;
    }

    @NonNull
    @Override
    public Request request() {
        return request;
    }

    @NonNull
    @Override
    public Response execute() {
        executed = true;
        return response;
    }

    @Override
    public void enqueue(Callback callback) {
        executed = true;
        try {
            callback.onResponse(this, response);
        } catch (IOException e) {
            // If your production code ever triggers onFailure, you can add that too
            callback.onFailure(this, e);
        }
    }

    @Override
    public void cancel() { }

    @Override
    public boolean isExecuted() {
        return executed;
    }

    @Override
    public boolean isCanceled() {
        return false;
    }

    @NonNull
    @Override
    public Timeout timeout() {
        return Timeout.NONE;
    }

    @Override
    public Call clone() {
        return new FakeCall(request, response);
    }
}
