package com.vointika.identity.infrastructure.persistence.repository;

import com.vointika.identity.domain.entity.VerificationToken;
import com.vointika.identity.domain.repository.VerificationTokenRepository;
import com.vointika.identity.infrastructure.persistence.mapper.VerificationTokenMapper;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class VerificationTokenRepositoryImpl implements VerificationTokenRepository {

    private final VerificationTokenJpaRepository verificationTokenJpaRepository;

    public VerificationTokenRepositoryImpl(VerificationTokenJpaRepository verificationTokenJpaRepository) {
        this.verificationTokenJpaRepository = verificationTokenJpaRepository;
    }

    @Override
    public VerificationToken save(VerificationToken token) {
        return VerificationTokenMapper.toDomain(
                verificationTokenJpaRepository.save(VerificationTokenMapper.toJpa(token))
        );
    }

    @Override
    public Optional<VerificationToken> findByTokenHash(String tokenHash) {
        return verificationTokenJpaRepository.findByTokenHash(tokenHash)
                .map(VerificationTokenMapper::toDomain);
    }

    @Override
    @Transactional
    public void expireAllByUserId(UUID userId) {
        verificationTokenJpaRepository.expireAllPendingByUserId(userId);
    }
}
