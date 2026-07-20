package com.vointika.shared.infrastructure.kafka;

import com.vointika.shared.event.AccountAlreadyRegisteredEvent;
import com.vointika.shared.event.PasswordChangedEvent;
import com.vointika.shared.event.PasswordResetEmailRequestedEvent;
import com.vointika.shared.event.TeamInvitationRequestedEvent;
import com.vointika.shared.event.TourOperatorWelcomeEmailRequestedEvent;
import com.vointika.shared.event.VerificationEmailRequestedEvent;

import java.util.Map;

/**
 * The event → topic registry. Naming convention:
 * {@code <producing-context>.<event-kebab>}, one topic per event type. {@code shared}
 * owns the event catalog ({@code shared/event}), so it owns their topic names too —
 * add a new event's topic here when the event is introduced.
 */
public final class EventTopics {

    public static final String VERIFICATION_EMAIL_REQUESTED = "identity.verification-email-requested";
    public static final String ACCOUNT_ALREADY_REGISTERED = "identity.account-already-registered";
    public static final String PASSWORD_CHANGED = "identity.password-changed";
    public static final String PASSWORD_RESET_REQUESTED = "identity.password-reset-requested";
    public static final String TOUR_OPERATOR_WELCOME_EMAIL_REQUESTED = "touroperator.welcome-email-requested";
    public static final String TEAM_INVITATION_REQUESTED = "touroperator.team-invitation-requested";

    /** Immutable event-class → topic map consumed by {@link KafkaEventPublisher}. */
    public static final Map<Class<?>, String> BY_EVENT_TYPE = Map.of(
            VerificationEmailRequestedEvent.class, VERIFICATION_EMAIL_REQUESTED,
            AccountAlreadyRegisteredEvent.class, ACCOUNT_ALREADY_REGISTERED,
            PasswordChangedEvent.class, PASSWORD_CHANGED,
            PasswordResetEmailRequestedEvent.class, PASSWORD_RESET_REQUESTED,
            TourOperatorWelcomeEmailRequestedEvent.class, TOUR_OPERATOR_WELCOME_EMAIL_REQUESTED,
            TeamInvitationRequestedEvent.class, TEAM_INVITATION_REQUESTED);

    private EventTopics() {}
}
