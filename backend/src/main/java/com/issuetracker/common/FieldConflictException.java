package com.issuetracker.common;

/**
 * A request conflicts with existing data on one field, e.g. a unique value already taken. Maps to 409.
 */
public class FieldConflictException extends RuntimeException {

    private final String field;

    public FieldConflictException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
