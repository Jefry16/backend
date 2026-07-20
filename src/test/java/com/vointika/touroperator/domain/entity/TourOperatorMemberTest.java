package com.vointika.touroperator.domain.entity;

import com.vointika.touroperator.domain.enums.MemberRole;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TourOperatorMemberTest {

    @Test
    void newOwnerMembershipCarriesRoleDefaultFlagAndJoinedAt() {
        UUID id = UUID.randomUUID();
        UUID operatorId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        TourOperatorMember member = new TourOperatorMember(
                id, operatorId, userId, MemberRole.OWNER, true);

        assertEquals(id, member.getId());
        assertEquals(operatorId, member.getTourOperatorId());
        assertEquals(userId, member.getUserId());
        assertEquals(MemberRole.OWNER, member.getRole());
        assertTrue(member.isDefault());
        assertNotNull(member.getJoinedAt());
    }

    @Test
    void changeRoleUpdatesTheRole() {
        TourOperatorMember member = new TourOperatorMember(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), MemberRole.OWNER, false);

        member.changeRole(MemberRole.ADMIN);

        assertEquals(MemberRole.ADMIN, member.getRole());
    }
}
