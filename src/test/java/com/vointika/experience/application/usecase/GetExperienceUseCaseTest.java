package com.vointika.experience.application.usecase;

import java.math.BigDecimal;
import com.vointika.experience.domain.valueobject.Price;
import com.vointika.experience.application.dto.output.ExperienceView;
import com.vointika.experience.domain.entity.Experience;
import com.vointika.experience.domain.repository.ExperienceRepository;
import com.vointika.experience.domain.valueobject.BookingCutoffHours;
import com.vointika.experience.domain.valueobject.Description;
import com.vointika.experience.domain.valueobject.DurationMinutes;
import com.vointika.experience.domain.valueobject.ExperienceName;
import com.vointika.experience.domain.valueobject.LongDescription;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.media.MediaUrlBatchResolver;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.valueobject.Handle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GetExperienceUseCaseTest {

    private ExperienceRepository repository;
    private MediaUrlBatchResolver resolver;
    private TourOperatorMembershipCheck membershipCheck;
    private GetExperienceUseCase useCase;

    private final UUID operatorId = UUID.randomUUID();
    private final UUID experienceId = UUID.randomUUID();
    private final UUID callerId = UUID.randomUUID();
    private final UUID media1 = UUID.randomUUID();
    private final UUID media2 = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        repository = mock(ExperienceRepository.class);
        resolver = mock(MediaUrlBatchResolver.class);
        membershipCheck = mock(TourOperatorMembershipCheck.class);
        useCase = new GetExperienceUseCase(repository, resolver, membershipCheck);
    }

    private Experience experience() {
        return Experience.create(experienceId, operatorId, UUID.randomUUID(), new Handle("dive"),
                new ExperienceName("Dive"), new Description("d"), new LongDescription("l"),
                false, List.of(), List.of(), List.of(), List.of(),
                List.of(media1, media2), media1, new DurationMinutes(60), new BookingCutoffHours(0), null, null, new Price(new BigDecimal("35.00")));
    }

    @Test
    void returnsViewWithResolvedMediaForAnyMember() {
        when(repository.findByIdAndTourOperatorId(experienceId, operatorId)).thenReturn(Optional.of(experience()));
        when(resolver.resolve(eq(operatorId), any()))
                .thenReturn(Map.of(media1, "https://cdn/m1.png", media2, "https://cdn/m2.png"));

        ExperienceView view = useCase.execute(operatorId, experienceId, callerId);

        verify(membershipCheck).ensureMember(callerId, operatorId);
        assertEquals("https://cdn/m1.png", view.thumbnailUrl());
        assertEquals(List.of("https://cdn/m1.png", "https://cdn/m2.png"), view.galleryUrls());
    }

    @Test
    void deletedMediaIsDroppedFromGallery() {
        when(repository.findByIdAndTourOperatorId(experienceId, operatorId)).thenReturn(Optional.of(experience()));
        // media2 no longer resolves (deleted) — dropped, not an error
        when(resolver.resolve(eq(operatorId), any())).thenReturn(Map.of(media1, "https://cdn/m1.png"));

        ExperienceView view = useCase.execute(operatorId, experienceId, callerId);

        assertEquals(List.of("https://cdn/m1.png"), view.galleryUrls());
    }

    @Test
    void nonMemberIs404() {
        doThrow(new ResourceNotFoundException("Tour operator not found"))
                .when(membershipCheck).ensureMember(callerId, operatorId);
        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(operatorId, experienceId, callerId));
    }

    @Test
    void unknownExperienceIs404() {
        when(repository.findByIdAndTourOperatorId(experienceId, operatorId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(operatorId, experienceId, callerId));
    }
}
