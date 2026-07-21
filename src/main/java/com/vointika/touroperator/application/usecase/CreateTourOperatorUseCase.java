package com.vointika.touroperator.application.usecase;

import com.vointika.reference.domain.repository.CurrencyRepository;
import com.vointika.reference.domain.repository.TimezoneRepository;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.exception.ResourceAlreadyExistsException;
import com.vointika.shared.exception.UnauthorizedException;
import com.vointika.shared.event.TourOperatorWelcomeEmailRequestedEvent;
import com.vointika.shared.port.EventPublisherPort;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.port.UserAccountQuery;
import com.vointika.shared.service.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.vointika.touroperator.application.dto.input.CreateTourOperatorInput;
import com.vointika.touroperator.application.dto.output.CreateTourOperatorOutput;
import com.vointika.shared.service.SlugGenerator;
import com.vointika.touroperator.domain.entity.TourOperator;
import com.vointika.touroperator.domain.entity.TourOperatorMember;
import com.vointika.touroperator.domain.enums.MemberRole;
import com.vointika.touroperator.domain.repository.TourOperatorMemberRepository;
import com.vointika.touroperator.domain.repository.TourOperatorRepository;
import com.vointika.shared.valueobject.Slug;
import com.vointika.touroperator.domain.valueobject.TourOperatorAddress;
import com.vointika.touroperator.domain.valueobject.TourOperatorName;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.UUID;

/**
 * Creates a tour operator and makes the creator its OWNER, in one transaction —
 * operator + owner membership always commit together, so there is never an
 * orphan operator without an owner. The single-owner invariant is DB-enforced
 * (partial unique index); the "default operator" flag is the separate picker
 * concern.
 *
 * <p>Create-slice scope: subscription seeding, audit trail, and the
 * {@code TourOperatorCreated} event are deliberately not part of this — they
 * belong to contexts that do not exist yet (see MAP).
 */
public class CreateTourOperatorUseCase {

    private static final Logger log = LoggerFactory.getLogger(CreateTourOperatorUseCase.class);

    /** Bounded slug-collision retries; each attempt regenerates the slug in a fresh tx. */
    private static final int MAX_SLUG_ATTEMPTS = 5;

    private final TourOperatorRepository tourOperatorRepository;
    private final TourOperatorMemberRepository memberRepository;
    private final TimezoneRepository timezoneRepository;
    private final CurrencyRepository currencyRepository;
    private final SlugGenerator slugGenerator;
    private final TransactionRunner transactionRunner;
    private final IdGenerator idGenerator;
    private final UserAccountQuery userAccountQuery;
    private final EventPublisherPort eventPublisher;

    public CreateTourOperatorUseCase(
            TourOperatorRepository tourOperatorRepository,
            TourOperatorMemberRepository memberRepository,
            TimezoneRepository timezoneRepository,
            CurrencyRepository currencyRepository,
            SlugGenerator slugGenerator,
            TransactionRunner transactionRunner,
            IdGenerator idGenerator,
            UserAccountQuery userAccountQuery,
            EventPublisherPort eventPublisher) {
        this.tourOperatorRepository = tourOperatorRepository;
        this.memberRepository = memberRepository;
        this.timezoneRepository = timezoneRepository;
        this.currencyRepository = currencyRepository;
        this.slugGenerator = slugGenerator;
        this.transactionRunner = transactionRunner;
        this.idGenerator = idGenerator;
        this.userAccountQuery = userAccountQuery;
        this.eventPublisher = eventPublisher;
    }

    public CreateTourOperatorOutput execute(CreateTourOperatorInput input) {
        // 1. Parse the authenticated user id.
        UUID createdBy;
        try {
            createdBy = UUID.fromString(input.userId());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new UnauthorizedException("Invalid authenticated user");
        }

        // 2. Validate value objects (422 on invalid input).
        TourOperatorName name = new TourOperatorName(input.name());
        TourOperatorAddress address = new TourOperatorAddress(input.address());

        // 3. Required reference ids must be present and must exist.
        if (input.timezoneId() == null) {
            throw new InvalidFieldException("Timezone is required");
        }
        if (input.currencyId() == null) {
            throw new InvalidFieldException("Currency is required");
        }
        timezoneRepository.findById(input.timezoneId())
                .orElseThrow(() -> new InvalidFieldException("Timezone not found"));
        currencyRepository.findById(input.currencyId())
                .orElseThrow(() -> new InvalidFieldException("Currency not found"));

        // 4. Reject a duplicate name for this owner — the common double-submit
        //    case. A truly concurrent race that slips past is still caught by
        //    the DB unique index below.
        if (tourOperatorRepository.existsByOwnerAndName(createdBy, name.value())) {
            throw new ResourceAlreadyExistsException(
                    "You already have an operator named \"" + name.value() + "\"");
        }

        // 5. Generate a slug + save operator and OWNER member in one tx. On a
        //    slug collision (DIV at commit) the whole tx rolls back and we retry
        //    with a fresh slug.
        TourOperator tourOperator = null;
        for (int attempt = 0; attempt < MAX_SLUG_ATTEMPTS; attempt++) {
            Slug slug = slugGenerator.generateUnique(
                    name.value(), tourOperatorRepository::existsBySlug);
            TourOperator candidate = new TourOperator(
                    idGenerator.newId(), name, slug,
                    input.timezoneId(), input.currencyId(), address, createdBy);
            try {
                tourOperator = transactionRunner.call(() -> {
                    tourOperatorRepository.save(candidate);
                    // First operator for this user becomes their default (picker).
                    boolean isFirst = !memberRepository.existsByUserId(createdBy);
                    memberRepository.save(new TourOperatorMember(
                            idGenerator.newId(), candidate.getId(), createdBy,
                            MemberRole.OWNER, isFirst));
                    return candidate;
                });
                break;
            } catch (DataIntegrityViolationException e) {
                // Distinguish a concurrent duplicate-name from a slug race: if a
                // sibling request just committed this name, surface the duplicate
                // rather than burning retries on a collision that never clears.
                if (tourOperatorRepository.existsByOwnerAndName(createdBy, name.value())) {
                    throw new ResourceAlreadyExistsException(
                            "You already have an operator named \"" + name.value() + "\"");
                }
                // otherwise slug race — regenerate and retry
            }
        }
        if (tourOperator == null) {
            throw new InvalidFieldException(
                    "Could not generate a unique slug — try a different name");
        }

        // 6. Welcome the creator. After-commit + fire-and-forget: an email
        //    hiccup must never fail an operator that was created successfully.
        publishWelcomeEmail(createdBy, tourOperator.getName().value());

        return new CreateTourOperatorOutput(tourOperator.getId());
    }

    private void publishWelcomeEmail(UUID createdBy, String operatorName) {
        try {
            userAccountQuery.findContact(createdBy).ifPresent(contact ->
                    eventPublisher.publish(new TourOperatorWelcomeEmailRequestedEvent(
                            contact.email(), contact.name(), operatorName, contact.language())));
        } catch (Exception e) {
            log.warn("Failed to enqueue welcome email for operator '{}' (creator {}): {}",
                    operatorName, createdBy, e.getMessage(), e);
        }
    }
}
