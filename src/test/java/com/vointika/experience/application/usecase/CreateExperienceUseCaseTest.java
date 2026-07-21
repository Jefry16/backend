package com.vointika.experience.application.usecase;

import com.vointika.experience.application.dto.input.ExperienceInput;
import com.vointika.experience.application.service.MediaReferenceValidator;
import com.vointika.experience.domain.entity.Experience;
import com.vointika.experience.domain.repository.ExperienceRepository;
import com.vointika.experience.domain.valueobject.ExperienceStatus;
import com.vointika.shared.exception.ForbiddenException;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.service.IdGenerator;
import com.vointika.shared.service.SlugGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CreateExperienceUseCaseTest {

    private ExperienceRepository repository;
    private MediaReferenceValidator mediaValidator;
    private TourOperatorMembershipCheck membershipCheck;
    private CreateExperienceUseCase useCase;

    private final UUID operatorId = UUID.randomUUID();
    private final UUID callerId = UUID.randomUUID();
    private final UUID newId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        repository = mock(ExperienceRepository.class);
        mediaValidator = mock(MediaReferenceValidator.class);
        membershipCheck = mock(TourOperatorMembershipCheck.class);
        IdGenerator idGenerator = mock(IdGenerator.class);
        when(idGenerator.newId()).thenReturn(newId);
        when(repository.existsByTourOperatorIdAndSlug(eq(operatorId), any())).thenReturn(false);
        when(repository.save(any())).thenAnswer(a -> a.getArgument(0));
        TransactionRunner tx = new TransactionRunner() {
            @Override public <T> T call(Supplier<T> work) { return work.get(); }
            @Override public void run(Runnable work) { work.run(); }
        };
        useCase = new CreateExperienceUseCase(repository, mediaValidator, membershipCheck,
                new SlugGenerator(), idGenerator, tx);
    }

    private ExperienceInput input(String name) {
        return new ExperienceInput(name, "A dive", "Long description", false,
                List.of(), List.of(), List.of(), List.of(), List.of(), null, 120, 24);
    }

    @Test
    void createsDraftWithGeneratedSlug() {
        UUID id = useCase.execute(operatorId, callerId, input("Dive Trip"));

        assertEquals(newId, id);
        verify(membershipCheck).ensureAdmin(callerId, operatorId);
        verify(mediaValidator).validate(eq(operatorId), any(), any());
        ArgumentCaptor<Experience> saved = ArgumentCaptor.forClass(Experience.class);
        verify(repository).save(saved.capture());
        assertEquals(ExperienceStatus.DRAFT, saved.getValue().getStatus());
        assertEquals("dive-trip", saved.getValue().getSlug().value());
        assertEquals(callerId, saved.getValue().getCreatedBy());
    }

    @Test
    void nonAdminIsRejectedBeforeAnyWork() {
        doThrow(new ForbiddenException("requires ADMIN")).when(membershipCheck).ensureAdmin(callerId, operatorId);
        assertThrows(ForbiddenException.class, () -> useCase.execute(operatorId, callerId, input("Dive Trip")));
        verify(repository, never()).save(any());
    }

    @Test
    void invalidFieldIs422() {
        assertThrows(InvalidFieldException.class, () -> useCase.execute(operatorId, callerId, input("  ")));
        verify(repository, never()).save(any());
    }

    @Test
    void foreignMediaIs422() {
        doThrow(new InvalidFieldException("bad media")).when(mediaValidator).validate(any(), any(), any());
        assertThrows(InvalidFieldException.class, () -> useCase.execute(operatorId, callerId, input("Dive Trip")));
        verify(repository, never()).save(any());
    }
}
