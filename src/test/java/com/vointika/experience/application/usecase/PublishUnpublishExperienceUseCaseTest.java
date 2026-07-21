package com.vointika.experience.application.usecase;

import com.vointika.experience.domain.entity.Experience;
import com.vointika.experience.domain.repository.ExperienceRepository;
import com.vointika.experience.domain.valueobject.BookingCutoffHours;
import com.vointika.experience.domain.valueobject.Description;
import com.vointika.experience.domain.valueobject.DurationMinutes;
import com.vointika.experience.domain.valueobject.ExperienceName;
import com.vointika.experience.domain.valueobject.ExperienceStatus;
import com.vointika.experience.domain.valueobject.LongDescription;
import com.vointika.shared.exception.ConflictException;
import com.vointika.shared.exception.ForbiddenException;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.valueobject.Slug;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PublishUnpublishExperienceUseCaseTest {

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
        publish = new PublishExperienceUseCase(repository, membershipCheck);
        unpublish = new UnpublishExperienceUseCase(repository, membershipCheck);
        when(repository.save(any())).thenAnswer(a -> a.getArgument(0));
    }

    private Experience draft() {
        return Experience.create(experienceId, operatorId, UUID.randomUUID(), new Slug("dive"),
                new ExperienceName("Dive"), new Description("d"), new LongDescription("l"),
                false, List.of(), List.of(), List.of(), List.of(),
                List.of(), null, new DurationMinutes(60), new BookingCutoffHours(0));
    }

    @Test
    void publishTransitionsDraftToPublished() {
        when(repository.findByIdAndTourOperatorId(experienceId, operatorId)).thenReturn(Optional.of(draft()));

        publish.execute(operatorId, experienceId, callerId);

        verify(membershipCheck).ensureAdmin(callerId, operatorId);
        ArgumentCaptor<Experience> saved = ArgumentCaptor.forClass(Experience.class);
        verify(repository).save(saved.capture());
        assertEquals(ExperienceStatus.PUBLISHED, saved.getValue().getStatus());
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
        assertEquals(ExperienceStatus.DRAFT, saved.getValue().getStatus());
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
