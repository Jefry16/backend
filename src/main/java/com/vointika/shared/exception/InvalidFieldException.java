package com.vointika.shared.exception;

public class InvalidFieldException extends DomainException {
    public InvalidFieldException(String message) {
        super(message);
    }

    public InvalidFieldException(String message, String errorCode) {
        super(message, errorCode);
    }
}
