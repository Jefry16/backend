package com.vointika.metafield.application.usecase;

import com.vointika.metafield.application.dto.input.UpsertMetafieldValueInput;
import com.vointika.metafield.application.service.MetafieldOwnerAccess;
import com.vointika.metafield.application.service.MetafieldValueValidator;
import com.vointika.metafield.domain.entity.MetafieldDefinition;
import com.vointika.metafield.domain.entity.MetafieldValue;
import com.vointika.metafield.domain.repository.MetafieldDefinitionRepository;
import com.vointika.metafield.domain.repository.MetafieldValueRepository;
import com.vointika.metafield.domain.repository.MetaobjectEntryRepository;
import com.vointika.metafield.domain.valueobject.MetafieldKey;
import com.vointika.metafield.domain.valueobject.MetafieldNamespace;
import com.vointika.metafield.domain.valueobject.MetafieldType;
import com.vointika.shared.exception.ConflictException;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.service.IdGenerator;
import com.vointika.shared.valueobject.AuditActor;
import com.vointika.shared.valueobject.FieldChange;
import com.vointika.shared.exception.UniqueConstraintViolationException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Sets (creates or replaces) one metafield value on an owning resource.
 * ADMIN+. The owner must belong to the operator (byte-identical 404 per
 * kind), the {@code namespace.key} must resolve to one of the operator's
 * definitions FOR THAT OWNER TYPE (404), and the value must fit the
 * definition's type (422). Owner-generic — the same flow serves experiences
 * and pages.
 */
public class UpsertMetafieldValueUseCase {

    private final MetafieldDefinitionRepository definitionRepository;
    private final MetafieldValueRepository valueRepository;
    private final MetaobjectEntryRepository metaobjectEntryRepository;
    private final MetafieldOwnerAccess ownerAccess;
    private final MetafieldValueValidator valueValidator;
    private final TourOperatorMembershipCheck membershipCheck;
    private final IdGenerator idGenerator;
    private final TransactionRunner transactionRunner;
    private final AuditTrailPort auditTrailPort;

    public UpsertMetafieldValueUseCase(MetafieldDefinitionRepository definitionRepository,
                                       MetafieldValueRepository valueRepository,
                                       MetaobjectEntryRepository metaobjectEntryRepository,
                                       MetafieldOwnerAccess ownerAccess,
                                       MetafieldValueValidator valueValidator,
                                       TourOperatorMembershipCheck membershipCheck,
                                       IdGenerator idGenerator,
                                       TransactionRunner transactionRunner,
                                       AuditTrailPort auditTrailPort) {
        this.definitionRepository = definitionRepository;
        this.valueRepository = valueRepository;
        this.metaobjectEntryRepository = metaobjectEntryRepository;
        this.ownerAccess = ownerAccess;
        this.valueValidator = valueValidator;
        this.membershipCheck = membershipCheck;
        this.idGenerator = idGenerator;
        this.transactionRunner = transactionRunner;
        this.auditTrailPort = auditTrailPort;
    }

    public void execute(UpsertMetafieldValueInput input) {
        membershipCheck.ensureAdmin(input.callerUserId(), input.tourOperatorId());
        ownerAccess.ensureOwned(input.ownerType(), input.ownerId(), input.tourOperatorId());

        MetafieldNamespace namespace = new MetafieldNamespace(input.namespace());
        MetafieldKey key = new MetafieldKey(input.key());
        MetafieldDefinition definition = definitionRepository
                .requireByIdentity(input.tourOperatorId(), input.ownerType(), namespace.value(), key.value());

        String normalized = valueValidator.validateAndNormalize(definition.getType(), input.value());
        // The validator only checks SHAPE for references; the integrity half —
        // the entry exists, is this operator's and is of the pinned type —
        // needs repositories, so it lives here.
        if (definition.getType() == MetafieldType.METAOBJECT_REFERENCE
                && !metaobjectEntryRepository.existsByIdAndDefinitionIdAndTourOperatorId(
                        UUID.fromString(normalized),
                        definition.getMetaobjectDefinitionId(),
                        input.tourOperatorId())) {
            throw new InvalidFieldException(
                    "The value must reference an existing metaobject of the pinned type");
        }

        Optional<MetafieldValue> existing =
                valueRepository.findByDefinitionIdAndOwnerId(definition.getId(), input.ownerId());
        String priorValue = existing.map(MetafieldValue::getValue).orElse(null);

        try {
            transactionRunner.run(() -> {
                existing.ifPresentOrElse(
                        row -> {
                            row.changeValue(normalized);
                            valueRepository.save(row);
                        },
                        () -> valueRepository.save(new MetafieldValue(
                                idGenerator.newId(), definition.getId(), input.ownerId(),
                                normalized, input.callerUserId())));
                // On the OWNER's timeline — a metafield value is the owner's
                // content (like a translation); details identify namespace.key
                // and the value diffs whole (first set: null → value). An
                // idempotent re-set of the same value records nothing.
                if (!normalized.equals(priorValue)) {
                    auditTrailPort.append(new NewAuditEntry(
                            input.tourOperatorId(), AuditActor.user(input.callerUserId()),
                            input.ownerType().auditEntityType(), input.ownerId(),
                            input.ownerType().action("updated"),
                            Map.of("namespace", namespace.value(), "key", key.value()),
                            List.of(new FieldChange("value", priorValue, normalized))));
                }
            });
        } catch (UniqueConstraintViolationException e) {
            // Two concurrent FIRST-sets both passed the empty probe; the
            // (definition, owner) unique index broke the tie. 409, not 500 —
            // a retry lands as a normal replace.
            throw new ConflictException("This metafield was set concurrently — retry");
        }
    }
}
