package com.vointika.reference.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(schema = "reference", name = "currencies")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CurrencyJpaEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, unique = true, length = 3)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String symbol;
}
