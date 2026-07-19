package com.vointika.reference.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(schema = "reference", name = "country")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CountryJpaEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, unique = true, length = 2)
    private String code;

    @Column(nullable = false)
    private String name;

    // Storage key for the flag image; nullable until flags are uploaded.
    @Column(name = "flag_key", length = 500)
    private String flagKey;
}
