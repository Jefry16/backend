package com.vointika.experience.infrastructure.persistence.repository;

import com.vointika.shared.infrastructure.list.CriteriaListExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The two handle pre-checks differ by one thing — whether the row being edited
 * is excluded — and that difference is invisible to every other test, because
 * the use-case tests stub
 * {@link com.vointika.experience.domain.repository.ExperienceRepository} rather
 * than exercise this adapter.
 *
 * <p>Swapping the excluding delegate for the plain one would make an experience
 * collide with <em>itself</em>: renaming nothing and saving would answer 409,
 * because its own row still holds the handle. The reverse swap is worse and
 * quieter — the create pre-check would stop seeing a genuine clash whenever the
 * excluded id happened to be null.
 */
class ExperienceRepositoryImplTest {

    private static final UUID OP = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3ea2");
    private static final UUID EXCLUDED = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3eb1");

    private ExperienceJpaRepository jpa;
    private ExperienceRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        jpa = mock(ExperienceJpaRepository.class);
        repository = new ExperienceRepositoryImpl(jpa, mock(CriteriaListExecutor.class));
    }

    @Test
    void theCreatePreCheckLooksAtEveryRow() {
        when(jpa.existsByTourOperatorIdAndHandle(OP, "sunset-sailing-tour")).thenReturn(true);

        assertThat(repository.existsByTourOperatorIdAndHandle(OP, "sunset-sailing-tour")).isTrue();

        verify(jpa).existsByTourOperatorIdAndHandle(OP, "sunset-sailing-tour");
    }

    @Test
    void theUpdatePreCheckExcludesTheRowBeingEdited() {
        when(jpa.existsByTourOperatorIdAndHandleAndIdNot(OP, "sunset-sailing-tour", EXCLUDED))
                .thenReturn(true);

        assertThat(repository.existsByTourOperatorIdAndHandleExcluding(
                OP, "sunset-sailing-tour", EXCLUDED)).isTrue();

        verify(jpa).existsByTourOperatorIdAndHandleAndIdNot(OP, "sunset-sailing-tour", EXCLUDED);
    }
}
