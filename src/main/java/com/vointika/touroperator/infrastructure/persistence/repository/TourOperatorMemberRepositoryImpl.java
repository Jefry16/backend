package com.vointika.touroperator.infrastructure.persistence.repository;

import com.vointika.shared.infrastructure.list.CriteriaListExecutor;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.ListQuery;
import com.vointika.touroperator.application.usecase.ListMembersUseCase;
import com.vointika.touroperator.domain.entity.TourOperatorMember;
import com.vointika.touroperator.domain.enums.MemberRole;
import com.vointika.touroperator.domain.repository.TourOperatorMemberRepository;
import com.vointika.touroperator.infrastructure.persistence.entity.TourOperatorMemberJpaEntity;
import com.vointika.touroperator.infrastructure.persistence.mapper.TourOperatorMemberMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
public class TourOperatorMemberRepositoryImpl implements TourOperatorMemberRepository {

    private final TourOperatorMemberJpaRepository jpaRepository;
    private final CriteriaListExecutor listExecutor;

    public TourOperatorMemberRepositoryImpl(TourOperatorMemberJpaRepository jpaRepository,
                                            CriteriaListExecutor listExecutor) {
        this.jpaRepository = jpaRepository;
        this.listExecutor = listExecutor;
    }

    @Override
    public CursorPage<TourOperatorMember> list(ListQuery query) {
        return listExecutor.list(
                TourOperatorMemberJpaEntity.class,
                ListMembersUseCase.SCHEMA,
                query,
                TourOperatorMemberMapper::toDomain);
    }

    @Override
    public TourOperatorMember save(TourOperatorMember member) {
        return TourOperatorMemberMapper.toDomain(
                jpaRepository.save(TourOperatorMemberMapper.toJpa(member))
        );
    }

    @Override
    public boolean existsByUserId(UUID userId) {
        return jpaRepository.existsByUserId(userId);
    }

    @Override
    public boolean existsByTourOperatorIdAndUserId(UUID tourOperatorId, UUID userId) {
        return jpaRepository.existsByTourOperatorIdAndUserId(tourOperatorId, userId);
    }

    @Override
    public Optional<MemberRole> findRoleByTourOperatorIdAndUserId(UUID tourOperatorId, UUID userId) {
        return jpaRepository.findByTourOperatorIdAndUserId(tourOperatorId, userId)
                .map(TourOperatorMemberJpaEntity::getRole);
    }

    @Override
    public Optional<TourOperatorMember> findByTourOperatorIdAndUserId(UUID tourOperatorId, UUID userId) {
        return jpaRepository.findByTourOperatorIdAndUserId(tourOperatorId, userId)
                .map(TourOperatorMemberMapper::toDomain);
    }

    @Override
    public long countByTourOperatorIdAndRole(UUID tourOperatorId, MemberRole role) {
        return jpaRepository.countByTourOperatorIdAndRole(tourOperatorId, role);
    }

    @Override
    @Transactional
    public void deleteByTourOperatorIdAndUserId(UUID tourOperatorId, UUID userId) {
        jpaRepository.deleteByTourOperatorIdAndUserId(tourOperatorId, userId);
    }

    @Override
    @Transactional
    public void transferOwnership(TourOperatorMember demotedOwner, TourOperatorMember promotedMember) {
        // Flush the demotion (OWNER → ADMIN) to the DB BEFORE the promotion, so at
        // no statement boundary do two rows carry role='OWNER'. saveAndFlush forces
        // the ordering that hibernate.order_updates=true would otherwise scramble.
        jpaRepository.saveAndFlush(TourOperatorMemberMapper.toJpa(demotedOwner));
        jpaRepository.saveAndFlush(TourOperatorMemberMapper.toJpa(promotedMember));
    }
}
