package com.vointika.touroperator.application.usecase;

import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.FilterSpec;
import com.vointika.shared.list.ListQuery;
import com.vointika.shared.list.SortDirection;
import com.vointika.shared.list.SortSpec;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.touroperator.application.dto.output.InvitationView;
import com.vointika.touroperator.domain.entity.TourOperatorInvitation;
import com.vointika.touroperator.domain.enums.InvitationStatus;
import com.vointika.touroperator.domain.enums.MemberRole;
import com.vointika.touroperator.domain.repository.TourOperatorInvitationRepository;
import com.vointika.touroperator.domain.valueobject.InviteeEmail;
import com.vointika.touroperator.domain.valueobject.InviteeName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ListInvitationsUseCaseTest {

    private TourOperatorInvitationRepository invitationRepository;
    private TourOperatorMembershipCheck membershipCheck;
    private ListInvitationsUseCase useCase;

    private final UUID operatorId = UUID.randomUUID();
    private final UUID callerId = UUID.randomUUID();
    private final UUID inviterId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        invitationRepository = mock(TourOperatorInvitationRepository.class);
        membershipCheck = mock(TourOperatorMembershipCheck.class);
        useCase = new ListInvitationsUseCase(invitationRepository, membershipCheck);
    }

    private ListQuery query() {
        return new ListQuery(operatorId, FilterSpec.empty(),
                new SortSpec("createdAt", SortDirection.DESC), null);
    }

    private TourOperatorInvitation invitation(InvitationStatus status, Instant expiresAt) {
        return new TourOperatorInvitation(
                UUID.randomUUID(), operatorId, new InviteeEmail("teammate@example.com"), new InviteeName("Test Invitee"),
                MemberRole.STAFF, "hash", status, inviterId, "Olive Inviter",
                Instant.parse("2026-01-01T00:00:00Z"), expiresAt, null);
    }

    @Test
    void mapsEachRowToAViewAndPropagatesTheCursor() {
        when(invitationRepository.list(any())).thenReturn(new CursorPage<>(
                List.of(invitation(InvitationStatus.PENDING, Instant.parse("2999-01-01T00:00:00Z")),
                        invitation(InvitationStatus.ACCEPTED, Instant.parse("2000-01-01T00:00:00Z"))),
                "next-cursor"));

        CursorPage<InvitationView> page = useCase.execute(query(), callerId);

        verify(membershipCheck).ensureMember(callerId, operatorId);
        assertEquals(2, page.data().size());
        assertEquals("next-cursor", page.nextCursor());
        assertEquals(InvitationStatus.PENDING, page.data().get(0).status());
        // ACCEPTED past its window is NOT flagged expired.
        assertFalse(page.data().get(1).expired());
        // Inviter name batch-resolved onto each row.
        assertEquals(inviterId, page.data().get(0).invitedByUserId());
        // Inviter name is the frozen snapshot carried on the invitation row.
        assertEquals("Olive Inviter", page.data().get(0).invitedByName());
    }

    @Test
    void flagsLapsedPendingRowsExpired() {
        when(invitationRepository.list(any())).thenReturn(new CursorPage<>(
                List.of(invitation(InvitationStatus.PENDING, Instant.parse("2000-01-01T00:00:00Z"))),
                null));

        CursorPage<InvitationView> page = useCase.execute(query(), callerId);

        assertTrue(page.data().get(0).expired());
    }

    @Test
    void emptyPageStaysEmpty() {
        when(invitationRepository.list(any())).thenReturn(new CursorPage<>(List.of(), null));

        CursorPage<InvitationView> page = useCase.execute(query(), callerId);

        assertTrue(page.data().isEmpty());
    }

    @Test
    void nonMemberIs404BeforeAnyQuery() {
        doThrow(new ResourceNotFoundException(TourOperatorMembershipCheck.TENANT_NOT_FOUND))
                .when(membershipCheck).ensureMember(callerId, operatorId);

        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(query(), callerId));
        verify(invitationRepository, never()).list(any());
    }
}
