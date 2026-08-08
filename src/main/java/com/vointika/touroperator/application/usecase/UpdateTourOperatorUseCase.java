package com.vointika.touroperator.application.usecase;

import com.vointika.reference.domain.repository.CurrencyRepository;
import com.vointika.reference.domain.repository.TimezoneRepository;
import com.vointika.touroperator.application.dto.input.UpdateTourOperatorInput;
import com.vointika.touroperator.domain.entity.TourOperator;
import com.vointika.touroperator.domain.repository.TourOperatorRepository;
import com.vointika.touroperator.domain.valueobject.TourOperatorAddress;
import com.vointika.touroperator.domain.valueobject.TourOperatorEmail;
import com.vointika.touroperator.domain.valueobject.TourOperatorName;
import com.vointika.touroperator.domain.valueobject.TourOperatorPhone;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.valueobject.AuditActor;
import com.vointika.shared.valueobject.AuditChanges;
import com.vointika.shared.valueobject.FieldChange;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Edits the operator's own details. ADMIN+.
 *
 * <p><b>PATCH semantics: an absent (null) field is unchanged</b>, the shape
 * {@code UpdateAudienceUseCase} set. A record cannot distinguish an absent JSON
 * field from an explicit null, so absence has to mean the safe thing — and
 * clearing an optional field is therefore a <b>blank string</b>. {@code name}
 * and {@code address} are NOT NULL columns and can only be replaced.
 *
 * <p>{@code handle} is not editable and is not in the input: it is the
 * storefront subdomain, so changing it moves the shop's public address and
 * breaks every link to it. That stays a deliberate omission rather than a
 * validation error.
 *
 * <p><b>Changing the timezone reinterprets every departure already stored, and
 * this use case does not stop you.</b> Slots hold operator-LOCAL wall-clock
 * times ({@code LocalDateTime}, no zone), so a 10:00 sailing stays "10:00" and
 * silently means a different instant; {@code CreateSlotUseCase}'s past-date
 * guard also starts judging against a different today. Nothing rewrites those
 * rows. This was allowed by an explicit product decision, and the audit entry
 * carries the before/after so the reinterpretation is at least traceable — a
 * migration for existing slots is the open half.
 *
 * <p>Nothing is written when nothing changed, so a no-op PATCH records no audit
 * entry.
 */
public class UpdateTourOperatorUseCase {

    private final TourOperatorRepository tourOperatorRepository;
    private final TimezoneRepository timezoneRepository;
    private final CurrencyRepository currencyRepository;
    private final TourOperatorMembershipCheck membershipCheck;
    private final TransactionRunner transactionRunner;
    private final AuditTrailPort auditTrailPort;

    public UpdateTourOperatorUseCase(TourOperatorRepository tourOperatorRepository,
                                     TimezoneRepository timezoneRepository,
                                     CurrencyRepository currencyRepository,
                                     TourOperatorMembershipCheck membershipCheck,
                                     TransactionRunner transactionRunner,
                                     AuditTrailPort auditTrailPort) {
        this.tourOperatorRepository = tourOperatorRepository;
        this.timezoneRepository = timezoneRepository;
        this.currencyRepository = currencyRepository;
        this.membershipCheck = membershipCheck;
        this.transactionRunner = transactionRunner;
        this.auditTrailPort = auditTrailPort;
    }

    public void execute(UUID tourOperatorId, UpdateTourOperatorInput input, UUID callerUserId) {
        membershipCheck.ensureAdmin(callerUserId, tourOperatorId);
        TourOperator operator = tourOperatorRepository.findById(tourOperatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Tour operator not found"));

        Map<String, Object> before = operator.auditSnapshot();

        TourOperatorName name = input.name() == null
                ? operator.getName() : new TourOperatorName(input.name());
        TourOperatorAddress address = input.address() == null
                ? operator.getAddress() : new TourOperatorAddress(input.address());
        // Blank clears; absent keeps. Both columns are nullable, so "no phone" is
        // a real state the storefront template already guards for.
        TourOperatorPhone phone = input.phone() == null
                ? operator.getPhone()
                : (input.phone().isBlank() ? null : new TourOperatorPhone(input.phone()));
        TourOperatorEmail email = input.email() == null
                ? operator.getEmail()
                : (input.email().isBlank() ? null : new TourOperatorEmail(input.email()));

        UUID timezoneId = operator.getTimezoneId();
        if (input.timezoneId() != null && !input.timezoneId().equals(timezoneId)) {
            if (timezoneRepository.findById(input.timezoneId()).isEmpty()) {
                throw new InvalidFieldException("Timezone not found");
            }
            timezoneId = input.timezoneId();
        }
        UUID currencyId = operator.getCurrencyId();
        if (input.currencyId() != null && !input.currencyId().equals(currencyId)) {
            if (currencyRepository.findById(input.currencyId()).isEmpty()) {
                throw new InvalidFieldException("Currency not found");
            }
            currencyId = input.currencyId();
        }

        operator.updateDetails(name, address, phone, email, timezoneId, currencyId);
        List<FieldChange> changes = AuditChanges.diff(before, operator.auditSnapshot());
        if (changes.isEmpty()) {
            return;
        }

        transactionRunner.run(() -> {
            tourOperatorRepository.save(operator);
            auditTrailPort.append(new NewAuditEntry(
                    tourOperatorId, AuditActor.user(callerUserId),
                    "TOUR_OPERATOR", tourOperatorId, "tour_operator.updated", null, changes));
        });
    }
}
