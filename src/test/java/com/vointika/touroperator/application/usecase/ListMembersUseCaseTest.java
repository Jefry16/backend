package com.vointika.touroperator.application.usecase;

import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.FilterSpec;
import com.vointika.shared.list.ListQuery;
import com.vointika.shared.list.SortDirection;
import com.vointika.shared.list.SortSpec;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.touroperator.application.dto.output.MemberListView;
import com.vointika.touroperator.domain.entity.TourOperatorMember;
import com.vointika.touroperator.domain.enums.MemberRole;
import com.vointika.touroperator.domain.repository.TourOperatorMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ListMembersUseCaseTest {

    private TourOperatorMemberRepository memberRepository;
    private TourOperatorMembershipCheck membershipCheck;
    private ListMembersUseCase useCase;

    private final UUID op = UUID.randomUUID();
    private final UUID caller = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        memberRepository = mock(TourOperatorMemberRepository.class);
        membershipCheck = mock(TourOperatorMembershipCheck.class);
        useCase = new ListMembersUseCase(memberRepository, membershipCheck);
    }

    private ListQuery query() {
        return new ListQuery(op, FilterSpec.empty(), new SortSpec("joinedAt", SortDirection.ASC), null);
    }

    private TourOperatorMember member(UUID userId, MemberRole role, Instant joinedAt,
                                      String name, String email) {
        return new TourOperatorMember(UUID.randomUUID(), op, userId, role, false, joinedAt, name, email);
    }

    @Test
    void nonMemberGets404_memberOnly() {
        doThrow(new ResourceNotFoundException(TourOperatorMembershipCheck.TENANT_NOT_FOUND))
                .when(membershipCheck).ensureMember(eq(caller), eq(op));
        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(query(), caller));
        verify(memberRepository, never()).list(any());
    }

    @Test
    void anyMemberMayView_notJustAdmins() {
        // A STAFF caller (member) must NOT be blocked — the roster is member-visible.
        // ensureMember passes; ensureAdmin must never be consulted.
        when(memberRepository.list(any())).thenReturn(new CursorPage<>(List.of(
                member(UUID.randomUUID(), MemberRole.STAFF, Instant.now(), "Sam Staff", "sam@example.test")), null));

        useCase.execute(query(), caller);

        verify(membershipCheck).ensureMember(caller, op);
        verify(membershipCheck, never()).ensureAdmin(any(), any());
    }

    @Test
    void mapsRowFieldsAndCarriesTheCursorThrough() {
        UUID owner = UUID.randomUUID();
        UUID staff = UUID.randomUUID();
        when(memberRepository.list(any())).thenReturn(new CursorPage<>(List.of(
                member(owner, MemberRole.OWNER, Instant.parse("2026-01-01T00:00:00Z"), "Olive Owner", "owner@example.com"),
                member(staff, MemberRole.STAFF, Instant.parse("2026-02-01T00:00:00Z"), "Sam Staff", "staff@example.com")),
                "next-cursor"));

        CursorPage<MemberListView> page = useCase.execute(query(), caller);

        assertEquals("next-cursor", page.nextCursor(), "the repo's cursor is passed through unchanged");
        assertEquals(2, page.data().size());
        assertEquals(owner, page.data().get(0).userId(), "order preserved from the paginated query");
        // name/email come straight off the denormalized member row (no enrichment).
        assertEquals("Olive Owner", page.data().get(0).name());
        assertEquals("staff@example.com", page.data().get(1).email());
    }

    @Test
    void emptyPageReturnsEmptyWithCursor() {
        when(memberRepository.list(any())).thenReturn(CursorPage.empty());
        CursorPage<MemberListView> page = useCase.execute(query(), caller);
        assertEquals(0, page.data().size());
        assertNull(page.nextCursor());
    }
}
