package org.okane.voyagemapper.service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PrefetchTracker {

    private final Set<Long> inProgress = ConcurrentHashMap.newKeySet();

    public boolean tryStart(long pageId) {
        return inProgress.add(pageId);
    }

    public void finish(long pageId) {
        inProgress.remove(pageId);
    }

    public boolean isInProgress(long pageId) {
        return inProgress.contains(pageId);
    }

    public int size() {
        return inProgress.size();
    }
}