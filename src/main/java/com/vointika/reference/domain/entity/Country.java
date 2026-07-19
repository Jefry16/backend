package com.vointika.reference.domain.entity;

import java.util.UUID;

public class Country {

    private final UUID id;
    private final String code;
    private final String name;
    /** Storage key for the flag image (bucket-relative); null until set. Resolved to a URL at read time. */
    private final String flagKey;

    public Country(UUID id, String code, String name, String flagKey) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.flagKey = flagKey;
    }

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getFlagKey() { return flagKey; }
}
