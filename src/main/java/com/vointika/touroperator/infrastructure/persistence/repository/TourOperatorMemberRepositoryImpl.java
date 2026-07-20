package com.vointika.touroperator.infrastructure.persistence.repository;

import com.vointika.touroperator.domain.entity.TourOperatorMember;
import com.vointika.touroperator.domain.repository.TourOperatorMemberRepository;
import com.vointika.touroperator.infrastructure.persistence.mapper.TourOperatorMemberMapper;
import org.springframework.stereotype.Repository;

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
}
