package com.vointika.touroperator.infrastructure.persistence.repository;

import com.vointika.touroperator.domain.entity.TourOperatorMember;
import com.vointika.touroperator.domain.enums.MemberRole;
import com.vointika.touroperator.domain.repository.TourOperatorMemberRepository;
import com.vointika.touroperator.infrastructure.persistence.entity.TourOperatorMemberJpaEntity;
import com.vointika.touroperator.infrastructure.persistence.mapper.TourOperatorMemberMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class TourOperatorMemberRepositoryImpl implements TourOperatorMemberRepository {

    private final TourOperatorMemberJpaRepository jpaRepository;

    public TourOperatorMemberRepositoryImpl(TourOperatorMemberJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
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
}
