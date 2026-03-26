package org.okane.voyagemapper.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import retrofit2.Callback;
import retrofit2.Response;

public class FakeWikiRepository extends WikiRepository {
    private final Map<Long, PageContentResponse> responses = new HashMap<>();
    private final Map<Long, Throwable> errors = new HashMap<>();

    public void setResponse(long pageId, PageContentResponse response) {
        responses.put(pageId, response);
    }

    public void setError(long pageId, Throwable error) {
        errors.put(pageId, error);
    }

    @Override
    public void fetchPageWikitext(long pageId, Callback<PageContentResponse> cb) {
        if (errors.containsKey(pageId)) {
            cb.onFailure(null, errors.get(pageId));
            return;
        }

        PageContentResponse response = responses.get(pageId);
        cb.onResponse(null, Response.success(Objects.requireNonNullElseGet(response, PageContentResponse::new)));
    }
}