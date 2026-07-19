package com.vointika.identity.infrastructure.config;


import com.vointika.shared.port.EventPublisherPort;
import com.vointika.shared.port.RateLimiterPort;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.port.UserTourOperatorMembershipsQuery;
import com.vointika.shared.service.IdGenerator;
import com.vointika.identity.application.port.AvatarStoragePort;
import com.vointika.identity.application.port.PasswordHasherPort;
import com.vointika.identity.application.port.TokenGeneratorPort;
import com.vointika.identity.application.port.TokenHasherPort;
import com.vointika.identity.application.usecase.*;
import com.vointika.identity.domain.repository.PasswordResetTokenRepository;
import com.vointika.identity.domain.repository.RefreshTokenRepository;
import com.vointika.identity.domain.repository.UserRepository;
import com.vointika.identity.domain.repository.VerificationTokenRepository;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration("identityUseCaseConfig")
@EnableConfigurationProperties({JwtProperties.class, IdentityS3Properties.class, UiLanguageProperties.class})
public class UseCaseConfig {

    @Bean
    public RegisterUserUseCase registerUserUseCase(
            UserRepository userRepository,
            VerificationTokenRepository verificationTokenRepository,
            PasswordHasherPort passwordHasherPort,
            TokenGeneratorPort tokenGeneratorPort,
            TokenHasherPort tokenHasherPort,
            EventPublisherPort eventPublisher,
            TransactionRunner transactionRunner,
            IdGenerator idGenerator,
            RateLimiterPort rateLimiter) {
        return new RegisterUserUseCase(
                userRepository,
                verificationTokenRepository,
                passwordHasherPort,
                tokenGeneratorPort,
                tokenHasherPort,
                eventPublisher,
                transactionRunner,
                idGenerator,
                rateLimiter
        );
    }

    @Bean
    public VerifyAccountUseCase verifyAccountUseCase(
            VerificationTokenRepository verificationTokenRepository,
            UserRepository userRepository,
            TokenHasherPort tokenHasherPort,
            TransactionRunner transactionRunner) {
        return new VerifyAccountUseCase(verificationTokenRepository, userRepository, tokenHasherPort, transactionRunner);
    }

    @Bean
    public ResendVerificationEmailUseCase resendVerificationEmailUseCase(
            UserRepository userRepository,
            VerificationTokenRepository verificationTokenRepository,
            TokenGeneratorPort tokenGeneratorPort,
            TokenHasherPort tokenHasherPort,
            EventPublisherPort eventPublisher,
            TransactionRunner transactionRunner,
            IdGenerator idGenerator,
            RateLimiterPort rateLimiter) {
        return new ResendVerificationEmailUseCase(
                userRepository,
                verificationTokenRepository,
                tokenGeneratorPort,
                tokenHasherPort,
                eventPublisher,
                transactionRunner,
                idGenerator,
                rateLimiter
        );
    }

    @Bean
    public LoginUserUseCase loginUserUseCase(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordHasherPort passwordHasherPort,
            TokenGeneratorPort tokenGeneratorPort,
            TokenHasherPort tokenHasherPort,
            IdGenerator idGenerator,
            RateLimiterPort rateLimiter) {
        return new LoginUserUseCase(
                userRepository,
                refreshTokenRepository,
                passwordHasherPort,
                tokenGeneratorPort,
                tokenHasherPort,
                idGenerator,
                rateLimiter
        );
    }

    @Bean
    public RefreshAccessTokenUseCase refreshAccessTokenUseCase(
            RefreshTokenRepository refreshTokenRepository,
            UserRepository userRepository,
            TokenGeneratorPort tokenGeneratorPort,
            TokenHasherPort tokenHasherPort,
            TransactionRunner transactionRunner,
            IdGenerator idGenerator) {
        return new RefreshAccessTokenUseCase(
                refreshTokenRepository,
                userRepository,
                tokenGeneratorPort,
                tokenHasherPort,
                transactionRunner,
                idGenerator
        );
    }

    @Bean
    public LogoutUserUseCase logoutUserUseCase(
            RefreshTokenRepository refreshTokenRepository,
            TokenHasherPort tokenHasherPort) {
        return new LogoutUserUseCase(refreshTokenRepository, tokenHasherPort);
    }

    @Bean
    public ChangePasswordUseCase changePasswordUseCase(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordHasherPort passwordHasherPort,
            EventPublisherPort eventPublisher,
            TransactionRunner transactionRunner) {
        return new ChangePasswordUseCase(
                userRepository,
                refreshTokenRepository,
                passwordHasherPort,
                eventPublisher,
                transactionRunner
        );
    }

    @Bean
    public RequestPasswordResetUseCase requestPasswordResetUseCase(
            UserRepository userRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            TokenGeneratorPort tokenGeneratorPort,
            TokenHasherPort tokenHasherPort,
            EventPublisherPort eventPublisher,
            TransactionRunner transactionRunner,
            IdGenerator idGenerator,
            RateLimiterPort rateLimiter) {
        return new RequestPasswordResetUseCase(
                userRepository,
                passwordResetTokenRepository,
                tokenGeneratorPort,
                tokenHasherPort,
                eventPublisher,
                transactionRunner,
                idGenerator,
                rateLimiter
        );
    }

    @Bean
    public SetAvatarUseCase setAvatarUseCase(
            UserRepository userRepository,
            AvatarStoragePort avatarStoragePort,
            IdGenerator idGenerator,
            TransactionRunner transactionRunner) {
        return new SetAvatarUseCase(userRepository, avatarStoragePort, idGenerator, transactionRunner);
    }

    @Bean
    public ClearAvatarUseCase clearAvatarUseCase(
            UserRepository userRepository,
            AvatarStoragePort avatarStoragePort,
            TransactionRunner transactionRunner) {
        return new ClearAvatarUseCase(userRepository, avatarStoragePort, transactionRunner);
    }

    @Bean
    public ChangeLanguageUseCase changeLanguageUseCase(
            UserRepository userRepository,
            TransactionRunner transactionRunner,
            UiLanguageProperties uiLanguageProperties) {
        java.util.Set<String> supported = uiLanguageProperties.uiLanguages().stream()
                .map(l -> l.trim().toLowerCase(java.util.Locale.ROOT))
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
        return new ChangeLanguageUseCase(userRepository, transactionRunner, supported);
    }

    @Bean
    public GetProfileUseCase getProfileUseCase(
            UserRepository userRepository,
            UserTourOperatorMembershipsQuery userTourOperatorMembershipsQuery) {
        return new GetProfileUseCase(userRepository, userTourOperatorMembershipsQuery);
    }

    @Bean
    public ResetPasswordUseCase resetPasswordUseCase(
            PasswordResetTokenRepository passwordResetTokenRepository,
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordHasherPort passwordHasherPort,
            TokenHasherPort tokenHasherPort,
            EventPublisherPort eventPublisher,
            TransactionRunner transactionRunner) {
        return new ResetPasswordUseCase(
                passwordResetTokenRepository,
                userRepository,
                refreshTokenRepository,
                passwordHasherPort,
                tokenHasherPort,
                eventPublisher,
                transactionRunner
        );
    }
}
