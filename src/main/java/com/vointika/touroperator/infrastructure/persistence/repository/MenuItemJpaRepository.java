package com.vointika.touroperator.infrastructure.persistence.repository;

import com.vointika.touroperator.infrastructure.persistence.entity.MenuItemJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface MenuItemJpaRepository extends JpaRepository<MenuItemJpaEntity, UUID> {

    List<MenuItemJpaEntity> findByMenuIdOrderByPositionAsc(UUID menuId);

    /**
     * Every item of many menus in one read — the storefront wants an operator's
     * whole navigation, and one query per menu would be a page's worth of round
     * trips for two menus that always exist.
     *
     * <p>Ordered by menu then position, so a tree built by walking this list in
     * order comes out in the order the operator arranged it.
     */
    List<MenuItemJpaEntity> findByMenuIdInOrderByMenuIdAscPositionAsc(Collection<UUID> menuIds);

    /**
     * Bulk-clears a menu's items ahead of the same-tx re-insert (translations
     * cascade at DB level). Clear + flush per the house @Modifying convention
     * so the persistence context never holds stale rows across the replace.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM MenuItemJpaEntity i WHERE i.menuId = :menuId")
    void deleteByMenuId(UUID menuId);
}
