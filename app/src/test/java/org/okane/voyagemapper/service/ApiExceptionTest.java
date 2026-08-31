package org.okane.voyagemapper.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ApiExceptionTest {

    @Test
    void parsesRetryAfter() {
        ApiException e =
                new ApiException(429, "Too many requests", "37");

        assertEquals(429, e.getStatusCode());
        assertEquals(37L, e.getRetryAfterSeconds());
    }

    @Test
    void missingRetryAfterDefaultsToZero() {
        ApiException e =
                new ApiException(429, "Too many requests", null);

        assertEquals(0L, e.getRetryAfterSeconds());
    }

    @Test
    void invalidRetryAfterDefaultsToZero() {
        ApiException e =
                new ApiException(429, "Too many requests", "nonsense");

        assertEquals(0L, e.getRetryAfterSeconds());
    }

    @Test
    void invalidRetryAfterDoesNotThrow() {
        assertDoesNotThrow(() ->
                new ApiException(429, "Too many requests", "not-a-number")
        );
    }
}