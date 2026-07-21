package com.vointika.touroperator.infrastructure.persistence.repository;

import com.vointika.touroperator.domain.entity.TourOperatorInvitation;
import com.vointika.touroperator.domain.enums.InvitationStatus;
import com.vointika.touroperator.domain.repository.TourOperatorInvitationRepository;
import com.vointika.touroperator.infrastructure.persistence.mapper.TourOperatorInvitationMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class TourOperatorInvitationRepositoryImpl implements TourOperatorInvitationRepository {

    private final TourOperatorInvitationJpaRepository jpaRepository;

    public TourOperatorInvitationRepositoryImpl(TourOperatorInvitationJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public TourOperatorInvitation save(TourOperatorInvitation invitation) {
        return TourOperatorInvitationMapper.toDomain(
                jpaRepository.save(TourOperatorInvitationMapper.toJpa(invitation))
        );
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
