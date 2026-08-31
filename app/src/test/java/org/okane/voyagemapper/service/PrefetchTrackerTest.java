package org.okane.voyagemapper.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

class PrefetchTrackerTest {

    @Test
    void firstRequestForPageIsAccepted() {
        PrefetchTracker tracker = new PrefetchTracker();
        assertTrue(tracker.tryStart(123L));
        assertTrue(tracker.isInProgress(123L));
    }

    @Test
    void duplicateRequestForSamePageIsRejected() {
        PrefetchTracker tracker = new PrefetchTracker();
        assertTrue(tracker.tryStart(123L));
        assertFalse(tracker.tryStart(123L));
        assertEquals(1, tracker.size());
    }

    @Test
    void differentPagesCanBeInProgressTogether() {
        PrefetchTracker tracker = new PrefetchTracker();
        assertTrue(tracker.tryStart(123L));
        assertTrue(tracker.tryStart(456L));
        assertEquals(2, tracker.size());
    }

    @Test
    void pageCanBeRequestedAgainAfterFinishing() {
        PrefetchTracker tracker = new PrefetchTracker();
        assertTrue(tracker.tryStart(123L));
        tracker.finish(123L);
        assertFalse(tracker.isInProgress(123L));
        assertTrue(tracker.tryStart(123L));
    }

    @Test
    void finishingUnknownPageDoesNothing() {
        PrefetchTracker tracker = new PrefetchTracker();
        assertDoesNotThrow(() -> tracker.finish(999L));
        assertEquals(0, tracker.size());
    }

    @Test
    void concurrentRequestsAllowOnlyOneStart() throws Exception {
        PrefetchTracker tracker = new PrefetchTracker();
        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);

        List<Future<Boolean>> results = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            results.add(
                    executor.submit(() -> {
                        ready.countDown();
                        start.await();
                        return tracker.tryStart(123L);
                    })
            );
        }

        ready.await();
        start.countDown();
        int accepted = 0;

        for (Future<Boolean> result : results) {
            if (result.get()) {
                accepted++;
            }
        }

        executor.shutdown();
        assertEquals(1, accepted);
        assertEquals(1, tracker.size());
    }
}