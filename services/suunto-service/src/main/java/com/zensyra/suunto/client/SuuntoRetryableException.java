package com.zensyra.suunto.client;

public class SuuntoRetryableException extends RuntimeException {

    public SuuntoRetryableException(int status) {
        super("Retryable Suunto API response: HTTP " + status);
    }
}