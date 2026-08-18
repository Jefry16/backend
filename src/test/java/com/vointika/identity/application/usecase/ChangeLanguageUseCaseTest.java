package com.vointika.identity.application.usecase;

import com.vointika.identity.application.dto.input.ChangeLanguageInput;
import com.vointika.identity.domain.entity.User;
import com.vointika.identity.domain.enums.UserStatus;
import com.vointika.identity.domain.repository.UserRepository;
import com.vointika.shared.valueobject.Email;
import com.vointika.identity.domain.valueobject.UserName;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.exception.UnauthorizedException;
import com.vointika.shared.port.TransactionRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChangeLanguageUseCaseTest {

    @Mock private UserRepository userRepository;

    private final TransactionRunner transactionRunner = new TransactionRunner() {
        @Override public <T> T call(java.util.function.Supplier<T> work) { return work.get(); }
        @Override public void run(Runnable work) { work.run(); }
    };

    private ChangeLanguageUseCase useCase;
    private UUID userId;

    @BeforeEach
    void setUp() {
        // requireById is a default method: Mockito would stub it to null and every
        // "invalid principal" assertion below would pass without running the branch.
        // lenient() because not every test in this class reaches the lookup.
        lenient().doCallRealMethod().when(userRepository).requireById(any());
        useCase = new ChangeLanguageUseCase(userRepository, transactionRunner,
                new LinkedHashSet<>(List.of("en", "es")));
        userId = UUID.randomUUID();
    }

    @Test
    void shouldChangeLanguage() {
        User user = userWithLanguage("en");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        useCase.execute(new ChangeLanguageInput(userId, "es"));

        assertEquals("es", user.getLanguage());
        verify(userRepository).save(user);
    }

    @Test
    void shouldNormalizeCaseAndWhitespace() {
        User user = userWithLanguage("en");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        useCase.execute(new ChangeLanguageInput(userId, "  ES "));

        assertEquals("es", user.getLanguage());
        verify(userRepository).save(user);
    }

    @Test
    void shouldBeNoOpWhenLanguageUnchanged() {
        User user = userWithLanguage("es");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        useCase.execute(new ChangeLanguageInput(userId, "es"));

        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldRejectUnsupportedLanguage() {
        InvalidFieldException ex = assertThrows(InvalidFieldException.class,
                () -> useCase.execute(new ChangeLanguageInput(userId, "fr")));
        assertEquals("Unsupported language code 'fr': supported en, es", ex.getMessage());
        verifyNoInteractions(userRepository);
    }

    @Test
    void shouldRejectMissingLanguage() {
        assertThrows(InvalidFieldException.class,
                () -> useCase.execute(new ChangeLanguageInput(userId, null)));
        assertThrows(InvalidFieldException.class,
                () -> useCase.execute(new ChangeLanguageInput(userId, "  ")));
        verifyNoInteractions(userRepository);
    }

    @Test
    void shouldThrowWhenUserNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class,
                () -> useCase.execute(new ChangeLanguageInput(userId, "es")));
    }

    private User userWithLanguage(String language) {
        return new User(userId, new Email("test@example.com"), new UserName("Test"),
                "hashed", UserStatus.VERIFIED, null, language, Instant.now(), Instant.now());
    }
}
