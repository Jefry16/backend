package com.vointika.experience.application.usecase;

import java.math.BigDecimal;
import com.vointika.experience.domain.valueobject.Price;
import com.vointika.experience.domain.entity.Experience;
import com.vointika.experience.domain.repository.ExperienceRepository;
import com.vointika.experience.domain.valueobject.BookingCutoffHours;
import com.vointika.experience.domain.valueobject.Description;
import com.vointika.experience.domain.valueobject.DurationMinutes;
import com.vointika.experience.domain.valueobject.ExperienceName;
import com.vointika.experience.domain.valueobject.LongDescription;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.exception.ConflictException;
import com.vointika.shared.exception.ForbiddenException;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.valueobject.Handle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PublishUnpublishExperienceUseCaseTest {

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

    private ExperienceRepository repository;
    private TourOperatorMembershipCheck membershipCheck;
    private PublishExperienceUseCase publish;
    private UnpublishExperienceUseCase unpublish;

    private final UUID operatorId = UUID.randomUUID();
    private final UUID experienceId = UUID.randomUUID();
    private final UUID callerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        repository = mock(ExperienceRepository.class);
        membershipCheck = mock(TourOperatorMembershipCheck.class);
        publish = new PublishExperienceUseCase(repository, membershipCheck, transactionRunner, auditTrailPort);
        unpublish = new UnpublishExperienceUseCase(repository, membershipCheck, transactionRunner, auditTrailPort);
        when(repository.save(any())).thenAnswer(a -> a.getArgument(0));
    }

    private Experience draft() {
        return Experience.create(experienceId, operatorId, UUID.randomUUID(), new Handle("dive"),
                new ExperienceName("Dive"), new Description("d"), new LongDescription("l"),
                false, List.of(), List.of(), List.of(),
                List.of(), null, new DurationMinutes(60), new BookingCutoffHours(0), null, null, new Price(new BigDecimal("35.00")));
    }

    @Test
    void publishTransitionsDraftToPublished() {
        when(repository.findByIdAndTourOperatorId(experienceId, operatorId)).thenReturn(Optional.of(draft()));

        publish.execute(operatorId, experienceId, callerId);

        verify(membershipCheck).ensureAdmin(callerId, operatorId);
        ArgumentCaptor<Experience> saved = ArgumentCaptor.forClass(Experience.class);
        verify(repository).save(saved.capture());
        org.junit.jupiter.api.Assertions.assertTrue(saved.getValue().isPublished());
    }

    @Test
    void publishingAnAlreadyPublishedExperienceConflicts() {
        Experience e = draft();
        e.publish();
        when(repository.findByIdAndTourOperatorId(experienceId, operatorId)).thenReturn(Optional.of(e));
        assertThrows(ConflictException.class, () -> publish.execute(operatorId, experienceId, callerId));
    }

    @Test
    void unpublishTransitionsPublishedToDraft() {
        Experience e = draft();
        e.publish();
        when(repository.findByIdAndTourOperatorId(experienceId, operatorId)).thenReturn(Optional.of(e));

        unpublish.execute(operatorId, experienceId, callerId);

        ArgumentCaptor<Experience> saved = ArgumentCaptor.forClass(Experience.class);
        verify(repository).save(saved.capture());
        org.junit.jupiter.api.Assertions.assertFalse(saved.getValue().isPublished());
    }

    @Test
    void nonAdminIsRejected() {
        doThrow(new ForbiddenException("requires ADMIN")).when(membershipCheck).ensureAdmin(callerId, operatorId);
        assertThrows(ForbiddenException.class, () -> publish.execute(operatorId, experienceId, callerId));
        verify(repository, never()).save(any());
    }

    @Test
    void unknownExperienceIs404() {
        when(repository.findByIdAndTourOperatorId(experienceId, operatorId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> publish.execute(operatorId, experienceId, callerId));
    }
}
