package com.vointika.touroperator.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(schema = "touroperator", name = "menu_item_translations")
@IdClass(MenuItemTranslationId.class)
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MenuItemTranslationJpaEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID menuItemId;

    @Id
    @Column(nullable = false, updatable = false, length = 8)
    private String locale;

    @Column(nullable = false, length = 120)
    private String title;
}
