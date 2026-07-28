package com.vointika.touroperator.infrastructure.persistence.repository;

import com.vointika.touroperator.infrastructure.persistence.entity.MenuItemTranslationId;
import com.vointika.touroperator.infrastructure.persistence.entity.MenuItemTranslationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface MenuItemTranslationJpaRepository
        extends JpaRepository<MenuItemTranslationJpaEntity, MenuItemTranslationId> {

    @Query("""
            SELECT t FROM MenuItemTranslationJpaEntity t
            WHERE t.menuItemId IN (
                SELECT i.id FROM MenuItemJpaEntity i WHERE i.menuId = :menuId)
            """)
    List<MenuItemTranslationJpaEntity> findByMenuId(UUID menuId);
}
