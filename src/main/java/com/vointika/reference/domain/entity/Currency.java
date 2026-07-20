package com.vointika.reference.domain.entity;

import java.util.UUID;

public class Currency {

    private final UUID id;
    private final String code;
    private final String name;
    private final String symbol;

    public Currency(UUID id, String code, String name, String symbol) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.symbol = symbol;
    }

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getSymbol() { return symbol; }
}
