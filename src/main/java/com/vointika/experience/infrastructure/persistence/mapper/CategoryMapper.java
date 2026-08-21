package com.vointika.experience.infrastructure.persistence.mapper;

import com.vointika.experience.domain.entity.Category;
import com.vointika.experience.domain.valueobject.CategoryName;
import com.vointika.experience.infrastructure.persistence.entity.CategoryJpaEntity;

public class CategoryMapper {

    public static CategoryJpaEntity toJpa(Category c) {
        return new CategoryJpaEntity(
                c.getId(),
                c.getTourOperatorId(),
                c.getName().value(),
                c.getCreatedAt());
    }

    public static Category toDomain(CategoryJpaEntity jpa) {
        return new Category(
                jpa.getId(),
                jpa.getTourOperatorId(),
                new CategoryName(jpa.getName()),
                jpa.getCreatedAt());
    }

    private CategoryMapper() {}
}
