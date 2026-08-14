package com.vointika.identity.application.usecase;

import com.vointika.identity.application.dto.input.ChangeLanguageInput;
import com.vointika.identity.domain.entity.User;
import com.vointika.identity.domain.repository.UserRepository;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.exception.UnauthorizedException;
import com.vointika.shared.port.TransactionRunner;

import java.util.Locale;
import java.util.Set;

public class ChangeLanguageUseCase {

    private final UserRepository userRepository;
    private final TransactionRunner transactionRunner;
    private final Set<String> supportedLanguages;

    public ChangeLanguageUseCase(
            UserRepository userRepository,
            TransactionRunner transactionRunner,
            Set<String> supportedLanguages
    ) {
        this.userRepository = userRepository;
        this.transactionRunner = transactionRunner;
        this.supportedLanguages = supportedLanguages;
    }

    public void execute(ChangeLanguageInput input) {
        if (input.language() == null || input.language().isBlank()) {
            throw new InvalidFieldException("Language is required");
        }
        String language = input.language().trim().toLowerCase(Locale.ROOT);
        if (!supportedLanguages.contains(language)) {
            throw new InvalidFieldException("Unsupported language code '" + language
                    + "': supported " + String.join(", ", supportedLanguages));
        }

        User user = userRepository.findById(input.userId())
                .orElseThrow(() -> new UnauthorizedException("Invalid authenticated user"));
        if (language.equals(user.getLanguage())) {
            return; // idempotent — already set
        }

        user.changeLanguage(language);
        transactionRunner.run(() -> userRepository.save(user));
    }
}
