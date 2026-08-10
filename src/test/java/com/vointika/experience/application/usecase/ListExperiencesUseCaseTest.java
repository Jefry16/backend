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
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.FilterSpec;
import com.vointika.shared.list.ListQuery;
import com.vointika.shared.list.SortDirection;
import com.vointika.shared.list.SortSpec;
import com.vointika.shared.media.MediaUrlBatchResolver;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.valueobject.Handle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ListExperiencesUseCaseTest {

    private ExperienceRepository repository;
    private MediaUrlBatchResolver resolver;
    private TourOperatorMembershipCheck membershipCheck;
    private ListExperiencesUseCase useCase;

    private final UUID operatorId = UUID.randomUUID();
    private final UUID callerId = UUID.randomUUID();
    private final UUID media = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        repository = mock(ExperienceRepository.class);
        resolver = mock(MediaUrlBatchResolver.class);
        membershipCheck = mock(TourOperatorMembershipCheck.class);
        useCase = new ListExperiencesUseCase(repository, resolver, membershipCheck);
    }

    private ListQuery query() {
        return new ListQuery(operatorId, FilterSpec.empty(),
                new SortSpec("createdAt", SortDirection.DESC), null);
    }

    private Experience experience() {
        return Experience.create(UUID.randomUUID(), operatorId, UUID.randomUUID(), new Handle("s" + UUID.randomUUID()),
                new ExperienceName("Dive"), new Description("d"), new LongDescription("l"),
                false, List.of(), List.of(), List.of(), List.of(),
                List.of(media), media, new DurationMinutes(60), new BookingCutoffHours(0), null, null, new Price(BigDecimal.ZERO));
    }

    @Test
    void batchResolvesMediaOncePerPage() {
        when(repository.list(any())).thenReturn(new CursorPage<>(List.of(experience(), experience()), "next"));
        when(resolver.resolve(any(), any())).thenReturn(Map.of(media, "https://cdn/m.png"));

        CursorPage<ExperienceView> page = useCase.execute(query(), callerId);

        verify(membershipCheck).ensureMember(callerId, operatorId);
        // ONE resolve call for the whole page
        verify(resolver).resolve(any(), any());
        assertEquals(2, page.data().size());
        assertEquals("next", page.nextCursor());
        assertEquals("https://cdn/m.png", page.data().get(0).thumbnailUrl());
    }

    @Test
    void emptyPageStaysEmptyWithoutResolving() {
        when(repository.list(any())).thenReturn(new CursorPage<>(List.of(), null));
        assertTrue(useCase.execute(query(), callerId).data().isEmpty());
        verify(resolver, never()).resolve(any(), any());
    }

    @Test
    void nonMemberIs404BeforeAnyQuery() {
        doThrow(new ResourceNotFoundException("Tour operator not found"))
                .when(membershipCheck).ensureMember(callerId, operatorId);
        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(query(), callerId));
        verify(repository, never()).list(any());
    }
}
