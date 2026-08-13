package com.vointika.touroperator.application.usecase;

import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.valueobject.FieldChange;
import com.vointika.shared.valueobject.LocaleCode;
import com.vointika.shared.valueobject.Handle;
import com.vointika.touroperator.application.dto.output.StorefrontPasswordView;
import com.vointika.touroperator.domain.entity.TourOperator;
import com.vointika.touroperator.domain.repository.TourOperatorRepository;
import com.vointika.touroperator.domain.valueobject.TourOperatorAddress;
import com.vointika.touroperator.domain.valueobject.TourOperatorName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StorefrontPasswordUseCasesTest {

    private static final UUID OP = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3ea2");
    private static final UUID USER = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    private TourOperatorRepository tourOperatorRepository;
    private TourOperatorMembershipCheck membershipCheck;
    private TransactionRunner transactionRunner;
    private AuditTrailPort auditTrailPort;

    @BeforeEach
    void setUp() {
        tourOperatorRepository = mock(TourOperatorRepository.class);
        membershipCheck = mock(TourOperatorMembershipCheck.class);
        transactionRunner = mock(TransactionRunner.class);
        auditTrailPort = mock(AuditTrailPort.class);
        doAnswer(i -> {
            ((Runnable) i.getArgument(0)).run();
            return null;
        }).when(transactionRunner).run(any());
        when(tourOperatorRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    private TourOperator operator(boolean enabled, String password, String message) {
        return new TourOperator(OP, new TourOperatorName("Acme Tours"), new Handle("acme"),
                UUID.randomUUID(), UUID.randomUUID(), new TourOperatorAddress("Calle Mayor 1", null, "Palma", null, null, UUID.randomUUID()),
                USER, Instant.now(), Instant.now(),
                LocaleCode.of("en"), Set.of(LocaleCode.of("en")),
                enabled, password, message, null, null, null, null, null);
    }

    private UpdateStorefrontPasswordUseCase updateUseCase() {
        return new UpdateStorefrontPasswordUseCase(
                tourOperatorRepository, membershipCheck, transactionRunner, auditTrailPort);
    }

    @Test
    void getReturnsSettingsToAnyMember() {
        when(tourOperatorRepository.findById(OP))
                .thenReturn(Optional.of(operator(true, "secret", "Launching soon")));
        GetStorefrontPasswordUseCase useCase =
                new GetStorefrontPasswordUseCase(tourOperatorRepository, membershipCheck);

        StorefrontPasswordView view = useCase.execute(OP, USER);

        verify(membershipCheck).ensureMember(USER, OP);
        assertThat(view.enabled()).isTrue();
        assertThat(view.password()).isEqualTo("secret");
        assertThat(view.message()).isEqualTo("Launching soon");
    }

    @Test
    void enableWithPasswordSavesAndAuditsEnabledFlagOnly() {
        when(tourOperatorRepository.findById(OP))
                .thenReturn(Optional.of(operator(false, null, null)));

        updateUseCase().execute(OP, true, "sunset2026", "We open in August", USER);

        verify(membershipCheck).ensureAdmin(USER, OP);
        ArgumentCaptor<TourOperator> saved = ArgumentCaptor.forClass(TourOperator.class);
        verify(tourOperatorRepository).save(saved.capture());
        assertThat(saved.getValue().isPasswordEnabled()).isTrue();
        assertThat(saved.getValue().getStorefrontPassword()).isEqualTo("sunset2026");
        assertThat(saved.getValue().getPasswordMessage()).isEqualTo("We open in August");

        ArgumentCaptor<NewAuditEntry> audit = ArgumentCaptor.forClass(NewAuditEntry.class);
        verify(auditTrailPort).append(audit.capture());
        assertThat(audit.getValue().action())
                .isEqualTo("tour_operator.storefront_password_updated");
        // THE landmine: the password value must never reach the trail.
        assertThat(audit.getValue().changes())
                .extracting(FieldChange::field)
                .containsExactly("passwordEnabled", "passwordMessage");
    }

    @Test
    void enableWithoutAnyPasswordIs422() {
        when(tourOperatorRepository.findById(OP))
                .thenReturn(Optional.of(operator(false, null, null)));

        assertThatThrownBy(() -> updateUseCase().execute(OP, true, "  ", null, USER))
                .isInstanceOf(InvalidFieldException.class)
                .hasMessageContaining("password is required");
        verify(tourOperatorRepository, never()).save(any());
    }

    @Test
    void blankPasswordKeepsStoredOneSoReEnablingWorks() {
        when(tourOperatorRepository.findById(OP))
                .thenReturn(Optional.of(operator(false, "kept-secret", "msg")));

        updateUseCase().execute(OP, true, null, "msg", USER);

        ArgumentCaptor<TourOperator> saved = ArgumentCaptor.forClass(TourOperator.class);
        verify(tourOperatorRepository).save(saved.capture());
        assertThat(saved.getValue().isPasswordEnabled()).isTrue();
        assertThat(saved.getValue().getStorefrontPassword()).isEqualTo("kept-secret");
    }

    @Test
    void disableKeepsPasswordAndMessage() {
        when(tourOperatorRepository.findById(OP))
                .thenReturn(Optional.of(operator(true, "secret", "Launching soon")));

        updateUseCase().execute(OP, false, null, "Launching soon", USER);

        ArgumentCaptor<TourOperator> saved = ArgumentCaptor.forClass(TourOperator.class);
        verify(tourOperatorRepository).save(saved.capture());
        assertThat(saved.getValue().isPasswordEnabled()).isFalse();
        assertThat(saved.getValue().getStorefrontPassword()).isEqualTo("secret");
        assertThat(saved.getValue().getPasswordMessage()).isEqualTo("Launching soon");
    }

    @Test
    void passwordOnlyChangeAuditsAsBareEventWithoutChanges() {
        when(tourOperatorRepository.findById(OP))
                .thenReturn(Optional.of(operator(true, "old-secret", null)));

        updateUseCase().execute(OP, true, "new-secret", null, USER);

        ArgumentCaptor<NewAuditEntry> audit = ArgumentCaptor.forClass(NewAuditEntry.class);
        verify(auditTrailPort).append(audit.capture());
        assertThat(audit.getValue().changes()).isEmpty();
    }

    @Test
    void trueNoOpRecordsNothing() {
        when(tourOperatorRepository.findById(OP))
                .thenReturn(Optional.of(operator(true, "secret", "msg")));

        updateUseCase().execute(OP, true, "secret", "msg", USER);

        verify(tourOperatorRepository).save(any());
        verify(auditTrailPort, never()).append(any());
    }

    @Test
    void blankMessageClears() {
        when(tourOperatorRepository.findById(OP))
                .thenReturn(Optional.of(operator(true, "secret", "old message")));

        updateUseCase().execute(OP, true, null, "  ", USER);

        ArgumentCaptor<TourOperator> saved = ArgumentCaptor.forClass(TourOperator.class);
        verify(tourOperatorRepository).save(saved.capture());
        assertThat(saved.getValue().getPasswordMessage()).isNull();
    }

    @Test
    void overlongPasswordIs422() {
        when(tourOperatorRepository.findById(OP))
                .thenReturn(Optional.of(operator(false, null, null)));

        assertThatThrownBy(() -> updateUseCase().execute(OP, true, "x".repeat(101), null, USER))
                .isInstanceOf(InvalidFieldException.class)
                .hasMessageContaining("100");
    }
}
