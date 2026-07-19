package com.vointika.identity.domain.valueobject;


import com.vointika.shared.exception.InvalidFieldException;

public record UserName(String value) {

    public UserName {
        if (value == null || value.isBlank()) {
            throw new InvalidFieldException("Name cannot be blank");
        }
        value = value.trim();
        if (value.length() < 2 || value.length() > 100) {
            throw new InvalidFieldException("Name must be between 2 and 100 characters");
        }
    }
}