package com.vointika.experience.application.usecase;

import com.vointika.experience.application.dto.output.ExperienceTranslationView;
import com.vointika.experience.domain.entity.Experience;
import com.vointika.experience.domain.entity.ExperienceTranslation;
import com.vointika.experience.domain.repository.ExperienceRepository;
import com.vointika.experience.domain.repository.ExperienceTranslationRepository;
import com.vointika.experience.domain.valueobject.ExperienceName;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.exception.ForbiddenException;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.valueobject.LocaleCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExperienceTranslationReadDeleteUseCasesTest {

    // Executes the work inline so assertions on the wrapped calls still hold.
    private final TransactionRunner transactionRunner = executingRunner();

    private static TransactionRunner executingRunner() {
        TransactionRunner runner = mock(TransactionRunner.class);
        when(runner.call(any())).thenAnswer(i -> ((java.util.function.Supplier<?>) i.getArgument(0)).get());
        doAnswer(i -> {
            ((Runnable) i.getArgument(0)).run();
            return null;
        }).when(runner).run(any());
        return runner;
    }

    private final AuditTrailPort auditTrailPort = mock(AuditTrailPort.class);

    private ExperienceRepository experienceRepository;
    private ExperienceTranslationRepository translationRepository;
    private TourOperatorMembershipCheck membershipCheck;

    private GetExperienceTranslationUseCase getUseCase;
    private ListExperienceTranslationsUseCase listUseCase;
    private DeleteExperienceTranslationUseCase deleteUseCase;

    private final UUID operatorId = UUID.randomUUID();
    private final UUID experienceId = UUID.randomUUID();
    private final UUID callerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        experienceRepository = mock(ExperienceRepository.class);
        translationRepository = mock(ExperienceTranslationRepository.class);
        membershipCheck = mock(TourOperatorMembershipCheck.class);
        getUseCase = new GetExperienceTranslationUseCase(experienceRepository, translationRepository, membershipCheck);
        listUseCase = new ListExperienceTranslationsUseCase(experienceRepository, translationRepository, membershipCheck);
        deleteUseCase = new DeleteExperienceTranslationUseCase(experienceRepository, translationRepository, membershipCheck, transactionRunner, auditTrailPort);
        when(experienceRepository.findByIdAndTourOperatorId(experienceId, operatorId))
                .thenReturn(Optional.of(mock(Experience.class)));
    }

    @Test
    void getReturnsEmptyOverlayForUntranslatedLocale() {
        when(translationRepository.findByExperienceIdAndLocale(experienceId, "fr")).thenReturn(Optional.empty());

        ExperienceTranslationView view = getUseCase.execute(operatorId, experienceId, "fr", callerId);

        verify(membershipCheck).ensureMember(callerId, operatorId);
        assertEquals("fr", view.locale());
        assertNull(view.name());
        assertNull(view.slug());
    }

    @Test
    void getReturnsStoredOverlay() {
        ExperienceTranslation es = new ExperienceTranslation(experienceId, operatorId, LocaleCode.of("es"),
                new ExperienceName("Buceo"), null, null, null, null, null, null, null, null);
        when(translationRepository.findByExperienceIdAndLocale(experienceId, "es")).thenReturn(Optional.of(es));

        assertEquals("Buceo", getUseCase.execute(operatorId, experienceId, "es", callerId).name());
    }

    @Test
    void listMapsAllTranslatedLocales() {
        when(translationRepository.findAllByExperienceId(experienceId)).thenReturn(List.of(
                ExperienceTranslation.empty(experienceId, operatorId, LocaleCode.of("es")),
                ExperienceTranslation.empty(experienceId, operatorId, LocaleCode.of("fr"))));

        assertEquals(2, listUseCase.execute(operatorId, experienceId, callerId).size());
    }

    @Test
    void deleteRemovesExistingOverlayAndIsAdminGated() {
        when(translationRepository.findByExperienceIdAndLocale(experienceId, "es"))
                .thenReturn(Optional.of(ExperienceTranslation.empty(experienceId, operatorId, LocaleCode.of("es"))));

        deleteUseCase.execute(operatorId, experienceId, "es", callerId);

        verify(membershipCheck).ensureAdmin(callerId, operatorId);
        verify(translationRepository).deleteByExperienceIdAndLocale(experienceId, "es");
    }

    @Test
    void deleteOfAbsentOverlayIsIdempotentAndRemovesNothing() {
        deleteUseCase.execute(operatorId, experienceId, "es", callerId);
        verify(translationRepository, never()).deleteByExperienceIdAndLocale(any(), any());
    }

    @Test
    void deleteNonAdminIsRejected() {
        doThrow(new ForbiddenException("requires ADMIN")).when(membershipCheck).ensureAdmin(callerId, operatorId);
        assertThrows(ForbiddenException.class, () -> deleteUseCase.execute(operatorId, experienceId, "es", callerId));
        verify(translationRepository, never()).deleteByExperienceIdAndLocale(any(), any());
    }

    @Test
    void unknownExperienceIs404OnRead() {
        when(experienceRepository.findByIdAndTourOperatorId(eq(experienceId), eq(operatorId)))
                .thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> listUseCase.execute(operatorId, experienceId, callerId));
    }
}
