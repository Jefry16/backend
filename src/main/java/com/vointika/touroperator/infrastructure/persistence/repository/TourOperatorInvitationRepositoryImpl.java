package com.vointika.touroperator.infrastructure.persistence.repository;

import com.vointika.shared.infrastructure.list.CriteriaListExecutor;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.ListQuery;
import com.vointika.touroperator.application.usecase.ListInvitationsUseCase;
import com.vointika.touroperator.domain.entity.TourOperatorInvitation;
import com.vointika.touroperator.domain.enums.InvitationStatus;
import com.vointika.touroperator.domain.repository.TourOperatorInvitationRepository;
import com.vointika.touroperator.infrastructure.persistence.entity.TourOperatorInvitationJpaEntity;
import com.vointika.touroperator.infrastructure.persistence.mapper.TourOperatorInvitationMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class TourOperatorInvitationRepositoryImpl implements TourOperatorInvitationRepository {

    private final TourOperatorInvitationJpaRepository jpaRepository;
    private final CriteriaListExecutor listExecutor;

    public TourOperatorInvitationRepositoryImpl(TourOperatorInvitationJpaRepository jpaRepository,
                                                CriteriaListExecutor listExecutor) {
        this.jpaRepository = jpaRepository;
        this.listExecutor = listExecutor;
    }

    @Override
    public TourOperatorInvitation save(TourOperatorInvitation invitation) {
        return TourOperatorInvitationMapper.toDomain(
                jpaRepository.save(TourOperatorInvitationMapper.toJpa(invitation))
        );
    }

    @Override
    public CursorPage<TourOperatorInvitation> list(ListQuery query) {
        return listExecutor.list(
                TourOperatorInvitationJpaEntity.class,
                ListInvitationsUseCase.SCHEMA,
                query,
                TourOperatorInvitationMapper::toDomain);
    }

    @Override
    public Optional<TourOperatorInvitation> findByTokenHash(String tokenHash) {
        return jpaRepository.findByTokenHash(tokenHash)
                .map(TourOperatorInvitationMapper::toDomain);
    }

    @Override
    public Optional<TourOperatorInvitation> findByIdAndTourOperatorId(UUID id, UUID tourOperatorId) {
        return jpaRepository.findByIdAndTourOperatorId(id, tourOperatorId)
                .map(TourOperatorInvitationMapper::toDomain);
    }

    @Override
    public boolean existsPendingByTourOperatorIdAndEmail(UUID tourOperatorId, String email) {
        return jpaRepository.existsByTourOperatorIdAndEmailAndStatus(
                tourOperatorId, email, InvitationStatus.PENDING);
    }
}
