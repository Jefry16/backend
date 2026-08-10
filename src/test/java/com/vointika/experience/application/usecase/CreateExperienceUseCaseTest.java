package com.vointika.experience.application.usecase;

import java.math.BigDecimal;
import com.vointika.experience.application.dto.input.ExperienceInput;
import com.vointika.experience.application.service.MediaReferenceValidator;
import com.vointika.experience.domain.entity.Experience;
import com.vointika.experience.domain.repository.ExperienceRepository;
import com.vointika.experience.domain.repository.ExperienceTranslationRepository;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.exception.ForbiddenException;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.service.IdGenerator;
import com.vointika.shared.service.HandleGenerator;
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

    private final AuditTrailPort auditTrailPort = mock(AuditTrailPort.class);

    private ExperienceRepository repository;
    private ExperienceTranslationRepository translationRepository;
    private MediaReferenceValidator mediaValidator;
    private TourOperatorMembershipCheck membershipCheck;
    private CreateExperienceUseCase useCase;

    private final UUID operatorId = UUID.randomUUID();
    private final UUID callerId = UUID.randomUUID();
    private final UUID newId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        repository = mock(ExperienceRepository.class);
        translationRepository = mock(ExperienceTranslationRepository.class);
        mediaValidator = mock(MediaReferenceValidator.class);
        membershipCheck = mock(TourOperatorMembershipCheck.class);
        IdGenerator idGenerator = mock(IdGenerator.class);
        when(idGenerator.newId()).thenReturn(newId);
        when(repository.existsByTourOperatorIdAndHandle(eq(operatorId), any())).thenReturn(false);
        when(repository.save(any())).thenAnswer(a -> a.getArgument(0));
        TransactionRunner tx = new TransactionRunner() {
            @Override public <T> T call(Supplier<T> work) { return work.get(); }
            @Override public void run(Runnable work) { work.run(); }
        };
        useCase = new CreateExperienceUseCase(repository, translationRepository, mediaValidator,
                membershipCheck, new HandleGenerator(), idGenerator, tx, auditTrailPort);
    }

    private ExperienceInput input(String name) {
        return new ExperienceInput(name, "A dive", "Long description", false,
                List.of(), null, 24, null, null, new BigDecimal("35.00"));
    }

    @Test
    void createsDraftWithGeneratedHandle() {
        UUID id = useCase.execute(operatorId, callerId, input("Dive Trip"));

        assertEquals(newId, id);
        verify(membershipCheck).ensureAdmin(callerId, operatorId);
        verify(mediaValidator).validate(eq(operatorId), any(), any());
        ArgumentCaptor<Experience> saved = ArgumentCaptor.forClass(Experience.class);
        verify(repository).save(saved.capture());
        org.junit.jupiter.api.Assertions.assertFalse(saved.getValue().isPublished());
        assertEquals("dive-trip", saved.getValue().getHandle().value());
        assertEquals(callerId, saved.getValue().getCreatedBy());
    }

    /** Required on every experience, drafts included — there is no unpriced state. */
    @Test
    void anOmittedStartingPriceIs422() {
        ExperienceInput noPrice = new ExperienceInput("Dive Trip", "A dive", "Long description", false,
                List.of(), null, 24, null, null, null);

        assertThrows(InvalidFieldException.class,
                () -> useCase.execute(operatorId, callerId, noPrice));
        verify(repository, never()).save(any());
    }

    @Test
    void aZeroStartingPriceIs422() {
        ExperienceInput free = new ExperienceInput("Dive Trip", "A dive", "Long description", false,
                List.of(), null, 24, null, null,
                BigDecimal.ZERO);

        assertThrows(InvalidFieldException.class,
                () -> useCase.execute(operatorId, callerId, free));
        verify(repository, never()).save(any());
    }

    @Test
    void aStartingPriceIsStoredAsGiven() {
        ExperienceInput priced = new ExperienceInput("Dive Trip", "A dive", "Long description", false,
                List.of(), null, 24, null, null,
                new BigDecimal("35.5"));

        useCase.execute(operatorId, callerId, priced);

        ArgumentCaptor<Experience> saved = ArgumentCaptor.forClass(Experience.class);
        verify(repository).save(saved.capture());
        // Price normalises to 2dp, like audience_slot.price
        assertEquals("35.50", saved.getValue().getStartingPrice().value().toPlainString());
    }

    // The generated handle is derived, not operator-chosen, so a clash with a
    // localized handle auto-suffixes rather than answering 409.
    @Test
    void generatedHandleSkipsAnExistingLocalizedHandle() {
        when(translationRepository.existsByHandleInAnyLocale(operatorId, "dive-trip")).thenReturn(true);

        useCase.execute(operatorId, callerId, input("Dive Trip"));

        ArgumentCaptor<Experience> saved = ArgumentCaptor.forClass(Experience.class);
        verify(repository).save(saved.capture());
        assertEquals("dive-trip-2", saved.getValue().getHandle().value());
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
