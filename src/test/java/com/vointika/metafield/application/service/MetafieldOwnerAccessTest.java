package com.vointika.metafield.application.service;

import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.metafield.domain.valueobject.MetafieldOwnerType;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.ExperienceOwnershipQuery;
import com.vointika.shared.port.PageOwnershipQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * The ownership dispatch, and specifically the owner type that has no seam
 * behind it.
 *
 * <p>{@code TOUR_OPERATOR} is the one case where "does this belong to the
 * tenant" is answered here rather than by another context, so it is the one that
 * could quietly become no check at all — and the owner id arrives from a caller,
 * so it has to be checked like any other.
 */
class MetafieldOwnerAccessTest {

    private static final UUID OPERATOR = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3ea2");
    private static final UUID OTHER = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3eb9");

    private ExperienceOwnershipQuery experienceOwnershipQuery;
    private PageOwnershipQuery pageOwnershipQuery;
    private MetafieldOwnerAccess access;

    @BeforeEach
    void setUp() {
        experienceOwnershipQuery = mock(ExperienceOwnershipQuery.class);
        pageOwnershipQuery = mock(PageOwnershipQuery.class);
        access = new MetafieldOwnerAccess(experienceOwnershipQuery, pageOwnershipQuery);
    }

    @Test
    void anOperatorOwnsItself() {
        assertThatCode(() -> access.ensureOwned(MetafieldOwnerType.TOUR_OPERATOR, OPERATOR, OPERATOR))
                .doesNotThrowAnyException();
    }

    /**
     * The owner id comes off the URL on the other two mounts, so a caller could
     * name any UUID. Here it is the tenant's own id or it is a 404 — never a
     * write against somebody else's row.
     */
    @Test
    void anotherOperatorsIdIsNotOwned() {
        assertThatThrownBy(() -> access.ensureOwned(MetafieldOwnerType.TOUR_OPERATOR, OTHER, OPERATOR))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(TourOperatorMembershipCheck.TENANT_NOT_FOUND);
    }

    /** No seam is consulted, which is the whole reason this owner type was cheap. */
    @Test
    void theOperatorCheckAsksNoOtherContext() {
        access.ensureOwned(MetafieldOwnerType.TOUR_OPERATOR, OPERATOR, OPERATOR);

        verifyNoInteractions(experienceOwnershipQuery, pageOwnershipQuery);
    }

    @Test
    void theOtherOwnerTypesStillGoThroughTheirSeams() {
        when(experienceOwnershipQuery.existsForTourOperator(OTHER, OPERATOR)).thenReturn(true);
        when(pageOwnershipQuery.existsForTourOperator(OTHER, OPERATOR)).thenReturn(false);

        assertThatCode(() -> access.ensureOwned(MetafieldOwnerType.EXPERIENCE, OTHER, OPERATOR))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> access.ensureOwned(MetafieldOwnerType.PAGE, OTHER, OPERATOR))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Page not found");

        verify(experienceOwnershipQuery).existsForTourOperator(OTHER, OPERATOR);
        verify(pageOwnershipQuery, never()).existsForTourOperator(OPERATOR, OPERATOR);
    }
}
