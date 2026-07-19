package com.vointika.identity.infrastructure.persistence.repository;

import com.vointika.identity.domain.entity.PasswordResetToken;
import com.vointika.identity.domain.repository.PasswordResetTokenRepository;
import com.vointika.identity.infrastructure.persistence.mapper.PasswordResetTokenMapper;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class PasswordResetTokenRepositoryImpl implements PasswordResetTokenRepository {

    private final PasswordResetTokenJpaRepository passwordResetTokenJpaRepository;

    public PasswordResetTokenRepositoryImpl(PasswordResetTokenJpaRepository passwordResetTokenJpaRepository) {
        this.passwordResetTokenJpaRepository = passwordResetTokenJpaRepository;
    }

    @Override
    public PasswordResetToken save(PasswordResetToken token) {
        return PasswordResetTokenMapper.toDomain(
                passwordResetTokenJpaRepository.save(PasswordResetTokenMapper.toJpa(token))
        );
    }

    @Override
    public Optional<PasswordResetToken> findByTokenHash(String tokenHash) {
        return passwordResetTokenJpaRepository.findByTokenHash(tokenHash)
                .map(PasswordResetTokenMapper::toDomain);
    }

    @Override
    @Transactional
    public void expireAllByUserId(UUID userId) {
        passwordResetTokenJpaRepository.expireAllPendingByUserId(userId);
    }
}
