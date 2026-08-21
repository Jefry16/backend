package com.vointika.experience.infrastructure.persistence.repository;

import com.vointika.experience.application.usecase.ListCategoriesUseCase;
import com.vointika.experience.domain.entity.Category;
import com.vointika.experience.domain.repository.CategoryRepository;
import com.vointika.experience.infrastructure.persistence.entity.CategoryJpaEntity;
import com.vointika.experience.infrastructure.persistence.mapper.CategoryMapper;
import com.vointika.shared.infrastructure.list.CriteriaListExecutor;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.ListQuery;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class CategoryRepositoryImpl implements CategoryRepository {

    private final CategoryJpaRepository jpaRepository;
    private final CriteriaListExecutor listExecutor;

    public CategoryRepositoryImpl(CategoryJpaRepository jpaRepository, CriteriaListExecutor listExecutor) {
        this.jpaRepository = jpaRepository;
        this.listExecutor = listExecutor;
    }

    @Override
    public Category save(Category category) {
        return CategoryMapper.toDomain(jpaRepository.save(CategoryMapper.toJpa(category)));
    }

    @Override
    public Optional<Category> findByIdAndTourOperatorId(UUID id, UUID tourOperatorId) {
        return jpaRepository.findByIdAndTourOperatorId(id, tourOperatorId).map(CategoryMapper::toDomain);
    }

    @Override
    public boolean existsByIdAndTourOperatorId(UUID id, UUID tourOperatorId) {
        return jpaRepository.existsByIdAndTourOperatorId(id, tourOperatorId);
    }

    @Override
    public CursorPage<Category> list(ListQuery query) {
        return listExecutor.list(
                CategoryJpaEntity.class,
                ListCategoriesUseCase.SCHEMA,
                query,
                CategoryMapper::toDomain);
    }

    @Override
    public boolean existsByTourOperatorIdAndName(UUID tourOperatorId, String name) {
        return jpaRepository.existsByTourOperatorIdAndNameIgnoreCase(tourOperatorId, name);
    }

    @Override
    public boolean existsByTourOperatorIdAndNameExcluding(UUID tourOperatorId, String name, UUID excludingId) {
        return jpaRepository.existsByTourOperatorIdAndNameIgnoreCaseAndIdNot(tourOperatorId, name, excludingId);
    }

    @Override
    public void delete(Category category) {
        jpaRepository.deleteById(category.getId());
    }
}
