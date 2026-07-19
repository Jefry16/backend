package com.vointika.reference.domain.entity;

import java.util.UUID;

public class Timezone {

    private final UUID id;
    private final String name;
    private final String cityName;
    private final Country country;

    public Timezone(UUID id, String name, String cityName, Country country) {
        this.id = id;
        this.name = name;
        this.cityName = cityName;
        this.country = country;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getCityName() { return cityName; }
    public Country getCountry() { return country; }
}
