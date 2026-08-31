package org.okane.voyagemapper.service;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

public class PrefetchCooldown {

    private final AtomicLong blockedUntil = new AtomicLong(0L);
    private final LongSupplier clock;

    public PrefetchCooldown() {
        this(System::currentTimeMillis);
    }

    PrefetchCooldown(LongSupplier clock) {
        this.clock = clock;
    }

    public void blockForSeconds(long seconds) {
        if (seconds <= 0) {
            return;
        }
        long until = clock.getAsLong() + TimeUnit.SECONDS.toMillis(seconds);
        blockedUntil.updateAndGet(existing -> Math.max(existing, until));
    }

    public boolean isBlocked() {
        return clock.getAsLong() < blockedUntil.get();
    }

    public long remainingMillis() {
        return Math.max(0L, blockedUntil.get() - clock.getAsLong());
    }
}