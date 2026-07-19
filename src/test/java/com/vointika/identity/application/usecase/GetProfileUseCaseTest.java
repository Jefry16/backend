package com.vointika.identity.application.usecase;

import com.vointika.identity.application.dto.input.GetProfileInput;
import com.vointika.identity.application.dto.output.GetProfileOutput;
import com.vointika.identity.domain.entity.User;
import com.vointika.identity.domain.enums.UserStatus;
import com.vointika.shared.exception.UnauthorizedException;
import com.vointika.identity.domain.repository.UserRepository;
import com.vointika.identity.domain.valueobject.Email;
import com.vointika.identity.domain.valueobject.UserName;
import com.vointika.shared.port.UserTourOperatorMembershipsQuery;
import com.vointika.shared.port.UserTourOperatorMembershipsQuery.TourOperatorMembershipView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetProfileUseCaseTest {

    @Mock private UserRepository userRepository;
    @Mock private UserTourOperatorMembershipsQuery membershipsQuery;

    private GetProfileUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetProfileUseCase(userRepository, membershipsQuery);
    }

    @Test
    void returnsProfileWithEmptyTourOperatorsWhenUserHasNoMemberships() {
        UUID userId = UUID.randomUUID();
        User user = new User(userId, new Email("test@example.com"), new UserName("John Doe"),
                "hashed", UserStatus.VERIFIED, null, "en", Instant.now(), Instant.now());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(membershipsQuery.findForUser(userId)).thenReturn(List.of());

        GetProfileOutput output = useCase.execute(new GetProfileInput(userId.toString()));

        assertEquals(userId, output.id());
        assertEquals("John Doe", output.name());
        assertNull(output.avatarKey());
        assertEquals("en", output.language());
        assertTrue(output.tourOperators().isEmpty());
    }

    @Test
    void includesTourOperatorsWithTheCallersPerOperatorRole() {
        // The caller is OWNER of one operator and STAFF of another — the profile
        // must surface the correct per-operator role for each.
        UUID userId = UUID.randomUUID();
        User user = new User(userId, new Email("test@example.com"), new UserName("John Doe"),
                "hashed", UserStatus.VERIFIED, null, "en", Instant.now(), Instant.now());
        TourOperatorMembershipView ownedOp =
                new TourOperatorMembershipView(UUID.randomUUID(), "Acme Tours", "https://cdn/acme.png", "America/New_York", true, "OWNER");
        TourOperatorMembershipView staffedOp =
                new TourOperatorMembershipView(UUID.randomUUID(), "Beta Adventures", null, "Europe/London", false, "STAFF");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(membershipsQuery.findForUser(userId)).thenReturn(List.of(ownedOp, staffedOp));

        GetProfileOutput output = useCase.execute(new GetProfileInput(userId.toString()));

        assertEquals(List.of(ownedOp, staffedOp), output.tourOperators());
        assertEquals("OWNER", output.tourOperators().get(0).role());
        assertEquals("STAFF", output.tourOperators().get(1).role());
    }

    @Test
    void shouldThrowWhenUserNotFound() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> useCase.execute(new GetProfileInput(userId.toString())));
        assertEquals("Invalid authenticated user", ex.getMessage());
        verifyNoInteractions(membershipsQuery);
    }
}
