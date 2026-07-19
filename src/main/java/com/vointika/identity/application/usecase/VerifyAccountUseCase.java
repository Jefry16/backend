package com.vointika.identity.application.usecase;

import com.vointika.identity.application.dto.input.VerifyAccountInput;
import com.vointika.identity.application.port.TokenHasherPort;
import com.vointika.identity.domain.entity.User;
import com.vointika.identity.domain.entity.VerificationToken;
import com.vointika.shared.exception.UnauthorizedException;
import com.vointika.identity.domain.repository.UserRepository;
import com.vointika.identity.domain.repository.VerificationTokenRepository;
import com.vointika.shared.port.TransactionRunner;

public class VerifyAccountUseCase {

    private final VerificationTokenRepository verificationTokenRepository;
    private final UserRepository userRepository;
    private final TokenHasherPort tokenHasher;
    private final TransactionRunner transactionRunner;

    public VerifyAccountUseCase(
            VerificationTokenRepository verificationTokenRepository,
            UserRepository userRepository,
            TokenHasherPort tokenHasher,
            TransactionRunner transactionRunner
    ) {
        this.verificationTokenRepository = verificationTokenRepository;
        this.userRepository = userRepository;
        this.tokenHasher = tokenHasher;
        this.transactionRunner = transactionRunner;
    }

    public void execute(VerifyAccountInput input) {
        String tokenHash = tokenHasher.hash(input.token());
        transactionRunner.run(() -> {
            VerificationToken verificationToken = verificationTokenRepository
                    .findByTokenHash(tokenHash)
                    .orElseThrow(() -> new UnauthorizedException("Invalid verification token"));

            verificationToken.use();

            User user = userRepository
                    .findById(verificationToken.getUserId())
                    .orElseThrow(() -> new UnauthorizedException("Invalid verification token"));

            user.verify();

            verificationTokenRepository.save(verificationToken);
            userRepository.save(user);
        });
    }
}
