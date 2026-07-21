package com.vointika.experience.application.usecase;

import com.vointika.experience.application.dto.input.ExperienceInput;
import com.vointika.experience.application.service.MediaReferenceValidator;
import com.vointika.experience.domain.entity.Experience;
import com.vointika.experience.domain.repository.ExperienceRepository;
import com.vointika.experience.domain.valueobject.BookingCutoffHours;
import com.vointika.experience.domain.valueobject.Description;
import com.vointika.experience.domain.valueobject.DurationMinutes;
import com.vointika.experience.domain.valueobject.ExperienceName;
import com.vointika.experience.domain.valueobject.LongDescription;
import com.vointika.shared.exception.ForbiddenException;
import com.vointika.shared.exception.InvalidFieldException;
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

class UpdateExperienceUseCaseTest {

    private ExperienceRepository repository;
    private MediaReferenceValidator mediaValidator;
    private TourOperatorMembershipCheck membershipCheck;
    private UpdateExperienceUseCase useCase;

    private final UUID operatorId = UUID.randomUUID();
    private final UUID experienceId = UUID.randomUUID();
    private final UUID callerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        repository = mock(ExperienceRepository.class);
        mediaValidator = mock(MediaReferenceValidator.class);
        membershipCheck = mock(TourOperatorMembershipCheck.class);
        useCase = new UpdateExperienceUseCase(repository, mediaValidator, membershipCheck);
        when(repository.save(any())).thenAnswer(a -> a.getArgument(0));
    }

    private Experience existing() {
        return Experience.create(experienceId, operatorId, UUID.randomUUID(), new Slug("dive"),
                new ExperienceName("Old"), new Description("d"), new LongDescription("l"),
                false, List.of(), List.of(), List.of(), List.of(),
                List.of(), null, new DurationMinutes(60), new BookingCutoffHours(0));
    }

    private ExperienceInput input(String name) {
        return new ExperienceInput(name, "new desc", "new long", true,
                List.of(), List.of(), List.of(), List.of(), List.of(), null, 90, 12);
    }

    @Test
    void updatesEditableFieldsKeepingSlug() {
        when(repository.findByIdAndTourOperatorId(experienceId, operatorId)).thenReturn(Optional.of(existing()));

        useCase.execute(operatorId, experienceId, callerId, input("New Name"));

        verify(membershipCheck).ensureAdmin(callerId, operatorId);
        verify(mediaValidator).validate(any(), any(), any());
        ArgumentCaptor<Experience> saved = ArgumentCaptor.forClass(Experience.class);
        verify(repository).save(saved.capture());
        assertEquals("New Name", saved.getValue().getName().value());
        assertEquals("dive", saved.getValue().getSlug().value()); // slug immutable
        assertEquals(true, saved.getValue().isFeatured());
    }

    @Test
    void nonAdminIsRejected() {
        doThrow(new ForbiddenException("requires ADMIN")).when(membershipCheck).ensureAdmin(callerId, operatorId);
        assertThrows(ForbiddenException.class, () -> useCase.execute(operatorId, experienceId, callerId, input("x")));
        verify(repository, never()).save(any());
    }

    @Test
    void unknownExperienceIs404() {
        when(repository.findByIdAndTourOperatorId(experienceId, operatorId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> useCase.execute(operatorId, experienceId, callerId, input("x")));
    }

    @Test
    void foreignMediaIs422() {
        when(repository.findByIdAndTourOperatorId(experienceId, operatorId)).thenReturn(Optional.of(existing()));
        doThrow(new InvalidFieldException("bad media")).when(mediaValidator).validate(any(), any(), any());
        assertThrows(InvalidFieldException.class,
                () -> useCase.execute(operatorId, experienceId, callerId, input("x")));
        verify(repository, never()).save(any());
    }
}
