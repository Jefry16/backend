package com.vointika.reference.domain.entity;

import java.util.UUID;

public class Country {

    private final UUID id;
    private final String code;
    private final String name;

    public Country(UUID id, String code, String name) {
        this.id = id;
        this.code = code;
        this.name = name;
    }

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
}
