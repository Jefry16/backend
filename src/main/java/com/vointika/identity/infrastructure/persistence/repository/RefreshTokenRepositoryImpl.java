package com.vointika.identity.infrastructure.persistence.repository;

import com.vointika.identity.domain.entity.RefreshToken;
import com.vointika.identity.domain.repository.RefreshTokenRepository;
import com.vointika.identity.infrastructure.persistence.mapper.RefreshTokenMapper;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class RefreshTokenRepositoryImpl implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository refreshTokenJpaRepository;

    public RefreshTokenRepositoryImpl(RefreshTokenJpaRepository refreshTokenJpaRepository) {
        this.refreshTokenJpaRepository = refreshTokenJpaRepository;
    }

    @Override
    public RefreshToken save(RefreshToken token) {
        return RefreshTokenMapper.toDomain(
                refreshTokenJpaRepository.save(RefreshTokenMapper.toJpa(token))
        );
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return refreshTokenJpaRepository.findByTokenHash(tokenHash)
                .map(RefreshTokenMapper::toDomain);
    }

    @Override
    @Transactional
    public void revokeAllByUserId(UUID userId) {
        refreshTokenJpaRepository.revokeAllByUserId(userId);
    }

    @Override
    @Transactional
    public void revokeAllByFamilyId(UUID familyId) {
        refreshTokenJpaRepository.revokeAllByFamilyId(familyId);
    }

    @Override
    @Transactional
    public boolean revokeForRotation(UUID id, UUID replacedById) {
        return refreshTokenJpaRepository.revokeForRotation(id, replacedById) == 1;
    }
}
