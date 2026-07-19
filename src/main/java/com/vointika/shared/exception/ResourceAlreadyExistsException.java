package com.vointika.shared.exception;

public class ResourceAlreadyExistsException extends DomainException {
    public ResourceAlreadyExistsException(String message) {
        super(message);
    }
}
