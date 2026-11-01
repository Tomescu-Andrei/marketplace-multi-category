package com.marketplace.exception;

public class InvalidListingException extends RuntimeException {
    private final String field;

    public InvalidListingException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
