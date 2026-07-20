package com.vointika.touroperator.application.usecase;

import com.vointika.shared.exception.ForbiddenException;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.UserAccountQuery;
import com.vointika.shared.port.UserAccountView;
import com.vointika.touroperator.application.dto.output.MemberListView;
import com.vointika.touroperator.domain.entity.TourOperatorMember;
import com.vointika.touroperator.domain.enums.MemberRole;
import com.vointika.touroperator.domain.repository.TourOperatorMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ListMembersUseCaseTest {

    private TourOperatorMemberRepository memberRepository;
    private UserAccountQuery userAccountQuery;
    private TourOperatorMembershipCheck membershipCheck;
    private ListMembersUseCase useCase;

    private final UUID op = UUID.randomUUID();
    private final UUID caller = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        memberRepository = mock(TourOperatorMemberRepository.class);
        userAccountQuery = mock(UserAccountQuery.class);
        membershipCheck = mock(TourOperatorMembershipCheck.class);
        useCase = new ListMembersUseCase(memberRepository, userAccountQuery, membershipCheck);
    }

    private TourOperatorMember member(UUID userId, MemberRole role, Instant joinedAt) {
        return new TourOperatorMember(UUID.randomUUID(), op, userId, role, false, joinedAt);
    }

    @Test
    void staffCallerIsForbidden() {
        doThrow(new ForbiddenException("admin only")).when(membershipCheck).ensureAdmin(eq(caller), eq(op));
        assertThrows(ForbiddenException.class, () -> useCase.execute(op, caller));
    }

    @Test
    void ordersByJoinedAtAndEnrichesFromIdentity() {
        UUID owner = UUID.randomUUID(); // joined first
        UUID staff = UUID.randomUUID(); // joined later
        when(memberRepository.findByTourOperatorId(op)).thenReturn(List.of(
                member(staff, MemberRole.STAFF, Instant.parse("2026-02-01T00:00:00Z")),
                member(owner, MemberRole.OWNER, Instant.parse("2026-01-01T00:00:00Z"))));
        when(userAccountQuery.findAccounts(any())).thenReturn(List.of(
                new UserAccountView(owner, "owner@example.com", "Olive Owner"),
                new UserAccountView(staff, "staff@example.com", "Sam Staff")));

        List<MemberListView> rows = useCase.execute(op, caller);

        assertEquals(2, rows.size());
        assertEquals(owner, rows.get(0).userId(), "owner (earliest joinedAt) leads");
        assertEquals("Olive Owner", rows.get(0).name());
        assertEquals("staff@example.com", rows.get(1).email());
    }

    @Test
    void nullSafeWhenAnAccountCannotBeResolved() {
        UUID ghost = UUID.randomUUID();
        when(memberRepository.findByTourOperatorId(op)).thenReturn(List.of(
                member(ghost, MemberRole.STAFF, Instant.now())));
        when(userAccountQuery.findAccounts(any())).thenReturn(List.of()); // resolves nothing

        List<MemberListView> rows = useCase.execute(op, caller);

        assertEquals(1, rows.size());
        assertNull(rows.get(0).name());
        assertNull(rows.get(0).email());
        assertEquals(MemberRole.STAFF, rows.get(0).role());
    }
}
