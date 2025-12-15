package org.okane.voyagemapper.service;

import java.util.List;

public interface NearbyCallback {
    void onSuccess(List<WikiResponse.Page> pages);
    void onError(Throwable t);
}
