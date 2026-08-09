package com.vointika.experience.application.usecase;

import com.vointika.experience.domain.valueobject.Description;
import com.vointika.shared.valueobject.LocaleCode;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.experience.application.dto.input.UpsertExperienceTranslationInput;
import com.vointika.experience.domain.entity.Experience;
import com.vointika.experience.domain.entity.ExperienceTranslation;
import com.vointika.experience.domain.repository.ExperienceRepository;
import com.vointika.experience.domain.repository.ExperienceTranslationRepository;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.exception.ForbiddenException;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.exception.ResourceAlreadyExistsException;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.OperatorLocalesQuery;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.service.HandleGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UpsertExperienceTranslationUseCaseTest {

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
    private OperatorLocalesQuery operatorLocalesQuery;
    private TourOperatorMembershipCheck membershipCheck;
    private UpsertExperienceTranslationUseCase useCase;

    private final UUID operatorId = UUID.randomUUID();
    private final UUID experienceId = UUID.randomUUID();
    private final UUID callerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        experienceRepository = mock(ExperienceRepository.class);
        translationRepository = mock(ExperienceTranslationRepository.class);
        operatorLocalesQuery = mock(OperatorLocalesQuery.class);
        membershipCheck = mock(TourOperatorMembershipCheck.class);
        useCase = new UpsertExperienceTranslationUseCase(experienceRepository, translationRepository,
                operatorLocalesQuery, new HandleGenerator(), membershipCheck, transactionRunner, auditTrailPort);

        when(experienceRepository.findByIdAndTourOperatorId(experienceId, operatorId))
                .thenReturn(Optional.of(mock(Experience.class)));
        when(operatorLocalesQuery.findSupportedLocales(operatorId)).thenReturn(Set.of("en", "es"));
        when(translationRepository.existsByOperatorLocaleHandle(any(), any(), any(), any())).thenReturn(false);
        when(translationRepository.upsert(any())).thenAnswer(a -> a.getArgument(0));
    }

    private UpsertExperienceTranslationInput input(String name, String handle) {
        return new UpsertExperienceTranslationInput(name, "Buceo", "Larga", List.of("Grupo pequeño"),
                List.of("Equipo"), List.of(), handle, null, null);
    }

    @Test
    void upsertsWithAnExplicitLocalizedHandle() {
        useCase.execute(operatorId, experienceId, "es", input("Buceo al atardecer", "buceo-atardecer"), callerId);

        verify(membershipCheck).ensureAdmin(callerId, operatorId);
        ArgumentCaptor<ExperienceTranslation> saved = ArgumentCaptor.forClass(ExperienceTranslation.class);
        verify(translationRepository).upsert(saved.capture());
        assertEquals("es", saved.getValue().locale().value());
        assertEquals("Buceo al atardecer", saved.getValue().name().value());
        assertEquals("buceo-atardecer", saved.getValue().handle().value());
    }

    @Test
    void derivesLocalizedHandleFromNameWhenNoExplicitHandle() {
        useCase.execute(operatorId, experienceId, "es", input("Buceo al atardecer", null), callerId);

        ArgumentCaptor<ExperienceTranslation> saved = ArgumentCaptor.forClass(ExperienceTranslation.class);
        verify(translationRepository).upsert(saved.capture());
        assertEquals("buceo-al-atardecer", saved.getValue().handle().value());
    }

    @Test
    void nullHandleAndNullNameLeavesHandleNull() {
        useCase.execute(operatorId, experienceId, "es",
                new UpsertExperienceTranslationInput(null, "only desc", null, null, null, null, null, null, null), callerId);
        ArgumentCaptor<ExperienceTranslation> saved = ArgumentCaptor.forClass(ExperienceTranslation.class);
        verify(translationRepository).upsert(saved.capture());
        assertNull(saved.getValue().handle());
        assertNull(saved.getValue().name());
        assertEquals("only desc", saved.getValue().description().value());
    }

    @Test
    void blankingEveryFieldDeletesTheOverlayInsteadOfStoringAnEmptyOne() {
        // An overlay with nothing in it falls back for every field, so it is
        // indistinguishable from no overlay — except that it shows up in the
        // translations list as a locale someone has worked on. No name and no
        // explicit handle also means no derived handle, so the row really is empty.
        when(translationRepository.deleteByExperienceIdAndLocale(experienceId, "es")).thenReturn(true);

        useCase.execute(operatorId, experienceId, "es",
                new UpsertExperienceTranslationInput("  ", "", "   ", List.of(), List.of(), List.of(),
                        null, "", null), callerId);

        verify(translationRepository).deleteByExperienceIdAndLocale(experienceId, "es");
        verify(translationRepository, never()).upsert(any());

        ArgumentCaptor<NewAuditEntry> audit = ArgumentCaptor.forClass(NewAuditEntry.class);
        verify(auditTrailPort).append(audit.capture());
        assertEquals("experience.translation_deleted", audit.getValue().action());
    }

    @Test
    void savingAnAlreadyBlankExperienceTranslationWritesNothingAndAuditsNothing() {
        when(translationRepository.deleteByExperienceIdAndLocale(experienceId, "es")).thenReturn(false);

        useCase.execute(operatorId, experienceId, "es",
                new UpsertExperienceTranslationInput(null, null, null, null, null, null, null, null, null),
                callerId);

        verify(translationRepository, never()).upsert(any());
        verify(auditTrailPort, never()).append(any());
    }

    @Test
    void anEmptyTranslationKnowsItIsEmpty() {
        // empty() and isEmpty() are two halves of one idea; if a translatable
        // field is added to the record and not to isEmpty(), this fails.
        assertTrue(ExperienceTranslation.empty(experienceId, operatorId, LocaleCode.of("es")).isEmpty());
        assertFalse(new ExperienceTranslation(experienceId, operatorId, LocaleCode.of("es"),
                null, new Description("Buceo"), null, null, null, null, null, null, null).isEmpty());
    }

    @Test
    void unsupportedLocaleIs422() {
        assertThrows(InvalidFieldException.class,
                () -> useCase.execute(operatorId, experienceId, "de", input("X", null), callerId));
        verify(translationRepository, never()).upsert(any());
    }

    @Test
    void duplicateExplicitHandleIs409() {
        when(translationRepository.existsByOperatorLocaleHandle(eq(operatorId), eq("es"), eq("taken"), eq(experienceId)))
                .thenReturn(true);
        assertThrows(ResourceAlreadyExistsException.class,
                () -> useCase.execute(operatorId, experienceId, "es", input("X", "taken"), callerId));
        verify(translationRepository, never()).upsert(any());
    }

    @Test
    void explicitHandleMatchingAnotherExperiencesCanonicalHandleIs409() {
        when(experienceRepository.existsByTourOperatorIdAndHandleExcluding(operatorId, "buceo", experienceId))
                .thenReturn(true);
        assertThrows(ResourceAlreadyExistsException.class,
                () -> useCase.execute(operatorId, experienceId, "es", input("X", "buceo"), callerId));
        verify(translationRepository, never()).upsert(any());
    }

    // The guard excludes this experience, so its own canonical handle is not a clash —
    // it resolves to the same experience. Pins the Excluding half of the query.
    @Test
    void explicitHandleMatchingItsOwnCanonicalHandleIsAllowed() {
        when(experienceRepository.existsByTourOperatorIdAndHandle(operatorId, "buceo")).thenReturn(true);

        useCase.execute(operatorId, experienceId, "es", input("Buceo", "buceo"), callerId);

        ArgumentCaptor<ExperienceTranslation> saved = ArgumentCaptor.forClass(ExperienceTranslation.class);
        verify(translationRepository).upsert(saved.capture());
        assertEquals("buceo", saved.getValue().handle().value());
    }

    @Test
    void derivedHandleSkipsAnotherExperiencesCanonicalHandle() {
        when(experienceRepository.existsByTourOperatorIdAndHandleExcluding(
                operatorId, "buceo-al-atardecer", experienceId)).thenReturn(true);

        useCase.execute(operatorId, experienceId, "es", input("Buceo al atardecer", null), callerId);

        ArgumentCaptor<ExperienceTranslation> saved = ArgumentCaptor.forClass(ExperienceTranslation.class);
        verify(translationRepository).upsert(saved.capture());
        assertEquals("buceo-al-atardecer-2", saved.getValue().handle().value());
    }

    @Test
    void unknownExperienceIs404() {
        when(experienceRepository.findByIdAndTourOperatorId(experienceId, operatorId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> useCase.execute(operatorId, experienceId, "es", input("X", null), callerId));
        verify(translationRepository, never()).upsert(any());
    }

    @Test
    void nonAdminIsRejected() {
        doThrow(new ForbiddenException("requires ADMIN")).when(membershipCheck).ensureAdmin(callerId, operatorId);
        assertThrows(ForbiddenException.class,
                () -> useCase.execute(operatorId, experienceId, "es", input("X", null), callerId));
        verify(translationRepository, never()).upsert(any());
    }
}
