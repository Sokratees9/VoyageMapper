package org.okane.voyagemapper.service;

public class ApiException extends Exception {
    private final int statusCode;
    private final long retryAfterSeconds;

    public ApiException(int statusCode, String message, String retryAfter) {
        super(message);
        this.statusCode = statusCode;
        long parsedRetryAfter = 0L;
        if (retryAfter != null) {
            try {
                parsedRetryAfter = Integer.parseInt(retryAfter);
            } catch (NumberFormatException ignored) {}
        }
        this.retryAfterSeconds = parsedRetryAfter;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
