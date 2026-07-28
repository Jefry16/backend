package com.vointika.touroperator.infrastructure.persistence.repository;

import com.vointika.shared.infrastructure.list.CriteriaListExecutor;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.ListQuery;
import com.vointika.touroperator.application.usecase.ListMenusUseCase;
import com.vointika.touroperator.domain.entity.Menu;
import com.vointika.touroperator.domain.repository.MenuRepository;
import com.vointika.touroperator.infrastructure.persistence.entity.MenuJpaEntity;
import com.vointika.touroperator.infrastructure.persistence.mapper.MenuMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class MenuRepositoryImpl implements MenuRepository {

    private final MenuJpaRepository menuJpa;
    private final CriteriaListExecutor listExecutor;

    public MenuRepositoryImpl(MenuJpaRepository menuJpa, CriteriaListExecutor listExecutor) {
        this.menuJpa = menuJpa;
        this.listExecutor = listExecutor;
    }

    @Override
    public Menu save(Menu menu) {
        return MenuMapper.toDomain(menuJpa.save(MenuMapper.toJpa(menu)));
    }

    @Override
    public Optional<Menu> findByIdAndTourOperatorId(UUID menuId, UUID tourOperatorId) {
        return menuJpa.findByIdAndTourOperatorId(menuId, tourOperatorId)
                .map(MenuMapper::toDomain);
    }

    @Override
    public boolean existsByTourOperatorIdAndHandle(UUID tourOperatorId, String handle) {
        return menuJpa.existsByTourOperatorIdAndHandle(tourOperatorId, handle);
    }

    @Override
    public CursorPage<Menu> list(ListQuery query) {
        return listExecutor.list(
                MenuJpaEntity.class,
                ListMenusUseCase.SCHEMA,
                query,
                MenuMapper::toDomain);
    }

    @Override
    public void delete(UUID menuId) {
        menuJpa.deleteById(menuId);
    }
}
