package org.okane.voyagemapper.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

class PrefetchCooldownTest {

    @Test
    void initiallyNotBlocked() {
        AtomicLong clock = new AtomicLong(1_000L);
        PrefetchCooldown cooldown = new PrefetchCooldown(clock::get);
        assertFalse(cooldown.isBlocked());
        assertEquals(0L, cooldown.remainingMillis());
    }

    @Test
    void blocksForRetryAfterPeriod() {
        AtomicLong clock = new AtomicLong(1_000L);
        PrefetchCooldown cooldown = new PrefetchCooldown(clock::get);
        cooldown.blockForSeconds(37);
        assertTrue(cooldown.isBlocked());
        assertEquals(37_000L, cooldown.remainingMillis());
    }

    @Test
    void remainsBlockedBeforeExpiry() {
        AtomicLong clock = new AtomicLong(1_000L);
        PrefetchCooldown cooldown = new PrefetchCooldown(clock::get);
        cooldown.blockForSeconds(37);
        clock.addAndGet(36_999L);
        assertTrue(cooldown.isBlocked());
        assertEquals(1L, cooldown.remainingMillis());
    }

    @Test
    void becomesUnblockedAtExpiry() {
        AtomicLong clock = new AtomicLong(1_000L);
        PrefetchCooldown cooldown = new PrefetchCooldown(clock::get);
        cooldown.blockForSeconds(37);
        clock.addAndGet(37_000L);
        assertFalse(cooldown.isBlocked());
        assertEquals(0L, cooldown.remainingMillis());
    }

    @Test
    void longerCooldownExtendsExistingBlock() {
        AtomicLong clock = new AtomicLong(1_000L);
        PrefetchCooldown cooldown = new PrefetchCooldown(clock::get);
        cooldown.blockForSeconds(20);
        clock.addAndGet(5_000L);
        cooldown.blockForSeconds(30);
        assertEquals(30_000L, cooldown.remainingMillis());
    }

    @Test
    void shorterCooldownDoesNotShortenExistingBlock() {
        AtomicLong clock = new AtomicLong(1_000L);
        PrefetchCooldown cooldown = new PrefetchCooldown(clock::get);
        cooldown.blockForSeconds(60);
        clock.addAndGet(5_000L);
        cooldown.blockForSeconds(10);
        assertEquals(55_000L, cooldown.remainingMillis());
    }

    @Test
    void zeroRetryAfterDoesNotBlock() {
        AtomicLong clock = new AtomicLong(1_000L);
        PrefetchCooldown cooldown = new PrefetchCooldown(clock::get);
        cooldown.blockForSeconds(0);
        assertFalse(cooldown.isBlocked());
    }

    @Test
    void negativeRetryAfterDoesNotBlock() {
        AtomicLong clock = new AtomicLong(1_000L);
        PrefetchCooldown cooldown = new PrefetchCooldown(clock::get);
        cooldown.blockForSeconds(-10);
        assertFalse(cooldown.isBlocked());
    }
}