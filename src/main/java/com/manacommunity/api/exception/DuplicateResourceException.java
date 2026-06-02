package com.manacommunity.api.exception;

import org.springframework.http.HttpStatus;

/** Thrown when a unique constraint is violated (duplicate email, phone, etc.). */
public class DuplicateResourceException extends ManaCommunityException {

    public DuplicateResourceException(String resource, String field, String value) {
        super(resource + " already exists with " + field + ": " + value,
                HttpStatus.CONFLICT, "DUPLICATE_RESOURCE");
    }
}
