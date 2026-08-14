package com.vointika.touroperator.application.usecase;

import com.vointika.reference.domain.entity.Currency;
import com.vointika.reference.domain.entity.Timezone;
import com.vointika.reference.domain.repository.CountryRepository;
import com.vointika.reference.domain.repository.CurrencyRepository;
import com.vointika.reference.domain.repository.TimezoneRepository;
import com.vointika.shared.exception.ForbiddenException;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.valueobject.Handle;
import com.vointika.shared.valueobject.LocaleCode;
import com.vointika.touroperator.application.dto.input.UpdateTourOperatorInput;
import com.vointika.touroperator.domain.entity.TourOperator;
import com.vointika.touroperator.domain.repository.TourOperatorRepository;
import com.vointika.touroperator.domain.valueobject.TourOperatorAddress;
import com.vointika.touroperator.domain.valueobject.TourOperatorEmail;
import com.vointika.touroperator.domain.valueobject.TourOperatorName;
import com.vointika.touroperator.domain.valueobject.TourOperatorPhone;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Reading and editing the operator's own details. */
class TourOperatorDetailsUseCasesTest {

    private static final UUID OP = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3ea2");
    private static final UUID USER = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final UUID TZ = UUID.fromString("019f8200-0000-7000-8000-000000000001");
    private static final UUID CUR = UUID.fromString("019f8200-0000-7000-8000-000000000002");
    private static final UUID OTHER_TZ = UUID.fromString("019f8200-0000-7000-8000-00000000000a");

    private TourOperatorRepository operatorRepository;
    private TimezoneRepository timezoneRepository;
    private CountryRepository countryRepository;
    private CurrencyRepository currencyRepository;
    private TourOperatorMembershipCheck membershipCheck;
    private TransactionRunner transactionRunner;
    private AuditTrailPort auditTrailPort;

    @BeforeEach
    void setUp() {
        operatorRepository = mock(TourOperatorRepository.class);
        timezoneRepository = mock(TimezoneRepository.class);
        countryRepository = mock(CountryRepository.class);
        currencyRepository = mock(CurrencyRepository.class);
        membershipCheck = mock(TourOperatorMembershipCheck.class);
        transactionRunner = mock(TransactionRunner.class);
        auditTrailPort = mock(AuditTrailPort.class);
        doAnswer(i -> {
            ((Runnable) i.getArgument(0)).run();
            return null;
        }).when(transactionRunner).run(any());
        when(operatorRepository.findById(OP)).thenReturn(Optional.of(operator()));
        when(timezoneRepository.findById(OTHER_TZ)).thenReturn(Optional.of(mock(Timezone.class)));
        when(timezoneRepository.findById(TZ)).thenReturn(Optional.of(mock(Timezone.class)));
        when(currencyRepository.findById(CUR)).thenReturn(Optional.of(mock(Currency.class)));
    }

    private TourOperator operator() {
        TourOperator o = new TourOperator(OP, new TourOperatorName("Acme Tours"),
                new Handle("acme"), TZ, CUR, new TourOperatorAddress("Calle Mayor 1", null, "Palma", null, null, UUID.randomUUID()),
                USER, Instant.now(), Instant.now(),
                LocaleCode.of("en"), Set.of(LocaleCode.of("en")));
        o.updateDetails(o.getName(), o.getAddress(),
                new TourOperatorPhone("+34 600 000 000"),
                new TourOperatorEmail("hola@acme.test"), TZ, CUR);
        return o;
    }

    private UpdateTourOperatorUseCase update() {
        return new UpdateTourOperatorUseCase(operatorRepository, countryRepository, timezoneRepository,
                currencyRepository, membershipCheck, transactionRunner, auditTrailPort);
    }

    private static UpdateTourOperatorInput only(String phone, String email) {
        return new UpdateTourOperatorInput(null, null, phone, email, null, null);
    }


    @Test
    void getReturnsTheOperatorsDetails() {
        var view = new GetTourOperatorUseCase(operatorRepository, membershipCheck, countryRepository)
                .execute(OP, USER);

        verify(membershipCheck).ensureMember(USER, OP);
        assertThat(view.name()).isEqualTo("Acme Tours");
        assertThat(view.handle()).isEqualTo("acme");
        assertThat(view.phone()).isEqualTo("+34 600 000 000");
        assertThat(view.email()).isEqualTo("hola@acme.test");
    }


    @Test
    void anAbsentFieldIsUnchanged() {
        // PATCH semantics: a record cannot tell an absent JSON field from an
        // explicit null, so absence has to mean the safe thing.
        update().execute(OP, only("+34 611 111 111", null), USER);

        ArgumentCaptor<TourOperator> saved = ArgumentCaptor.forClass(TourOperator.class);
        verify(operatorRepository).save(saved.capture());
        assertThat(saved.getValue().getPhone().value()).isEqualTo("+34 611 111 111");
        assertThat(saved.getValue().getEmail().value()).isEqualTo("hola@acme.test");
        assertThat(saved.getValue().getName().value()).isEqualTo("Acme Tours");
    }

    @Test
    void aBlankStringClearsAnOptionalField() {
        // The columns are nullable and the storefront template guards on the
        // section, so "no phone" is a real state — but null already means
        // "unchanged", so clearing needs its own signal.
        update().execute(OP, only("", null), USER);

        ArgumentCaptor<TourOperator> saved = ArgumentCaptor.forClass(TourOperator.class);
        verify(operatorRepository).save(saved.capture());
        assertThat(saved.getValue().getPhone()).isNull();
        assertThat(saved.getValue().getEmail().value()).isEqualTo("hola@acme.test");
    }

    @Test
    void theAuditEntryCarriesTheFieldDiff() {
        update().execute(OP, only("+34 611 111 111", null), USER);

        ArgumentCaptor<NewAuditEntry> audit = ArgumentCaptor.forClass(NewAuditEntry.class);
        verify(auditTrailPort).append(audit.capture());
        assertThat(audit.getValue().action()).isEqualTo("tour_operator.updated");
        assertThat(audit.getValue().changes())
                .extracting(c -> c.field())
                .containsExactly("phone");
    }

    @Test
    void aNoOpPatchWritesNothingAndAuditsNothing() {
        update().execute(OP, new UpdateTourOperatorInput(null, null, null, null, null, null), USER);

        verify(operatorRepository, never()).save(any());
        verify(auditTrailPort, never()).append(any());
    }

    @Test
    void resendingTheSameValuesIsAlsoANoOp() {
        update().execute(OP, only("+34 600 000 000", "hola@acme.test"), USER);

        verify(operatorRepository, never()).save(any());
        verify(auditTrailPort, never()).append(any());
    }

    @Test
    void changingTheTimezoneIsAllowedAndAudited() {
        // Deliberate: slots hold operator-LOCAL wall-clock times, so this
        // reinterprets every stored departure. Allowed by product decision; the
        // audit entry is what makes it traceable.
        update().execute(OP,
                new UpdateTourOperatorInput(null, null, null, null, OTHER_TZ, null), USER);

        ArgumentCaptor<TourOperator> saved = ArgumentCaptor.forClass(TourOperator.class);
        verify(operatorRepository).save(saved.capture());
        assertThat(saved.getValue().getTimezoneId()).isEqualTo(OTHER_TZ);

        ArgumentCaptor<NewAuditEntry> audit = ArgumentCaptor.forClass(NewAuditEntry.class);
        verify(auditTrailPort).append(audit.capture());
        assertThat(audit.getValue().changes()).extracting(c -> c.field()).contains("timezoneId");
    }

    @Test
    void anUnknownTimezoneIs422AndWritesNothing() {
        UUID missing = UUID.randomUUID();
        when(timezoneRepository.findById(missing)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> update().execute(OP,
                new UpdateTourOperatorInput(null, null, null, null, missing, null), USER))
                .isInstanceOf(InvalidFieldException.class);
        verify(operatorRepository, never()).save(any());
    }

    @Test
    void anInvalidEmailIs422AndWritesNothing() {
        assertThatThrownBy(() -> update().execute(OP, only(null, "not-an-address"), USER))
                .isInstanceOf(InvalidFieldException.class);
        verify(operatorRepository, never()).save(any());
    }

    @Test
    void updateRequiresAdmin() {
        doThrow(new ForbiddenException("admin")).when(membershipCheck).ensureAdmin(USER, OP);
        assertThatThrownBy(() -> update().execute(OP, only("+34 611 111 111", null), USER))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void updatingAMissingOperatorIs404() {
        when(operatorRepository.findById(OP)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> update().execute(OP, only("+34 611 111 111", null), USER))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
