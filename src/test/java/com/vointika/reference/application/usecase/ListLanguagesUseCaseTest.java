package com.vointika.reference.application.usecase;

import com.vointika.reference.domain.entity.Language;
import com.vointika.reference.domain.repository.LanguageRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ListLanguagesUseCaseTest {

    private final LanguageRepository repository = mock(LanguageRepository.class);
    private final ListLanguagesUseCase useCase = new ListLanguagesUseCase(repository);

    @Test
    void returnsRepositoryFindAll() {
        Language en = new Language(UUID.randomUUID(), "en", "English");
        Language es = new Language(UUID.randomUUID(), "es", "Spanish");
        when(repository.findAll()).thenReturn(List.of(en, es));

        List<Language> result = useCase.execute();

        assertThat(result).containsExactly(en, es);
        verify(repository).findAll();
    }

    @Test
    void returnsEmptyListWhenNoneExist() {
        when(repository.findAll()).thenReturn(List.of());

        assertThat(useCase.execute()).isEmpty();
    }
}
