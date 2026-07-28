package com.vointika.touroperator.domain.repository;

import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.ListQuery;
import com.vointika.touroperator.domain.entity.Menu;

import java.util.Optional;
import java.util.UUID;

public interface MenuRepository {

    Menu save(Menu menu);

    Optional<Menu> findByIdAndTourOperatorId(UUID menuId, UUID tourOperatorId);

    boolean existsByTourOperatorIdAndHandle(UUID tourOperatorId, String handle);

    CursorPage<Menu> list(ListQuery query);

    void delete(UUID menuId);
}
