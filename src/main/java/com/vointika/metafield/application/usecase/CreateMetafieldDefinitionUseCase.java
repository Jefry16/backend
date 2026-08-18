package com.vointika.metafield.application.usecase;

import com.vointika.metafield.application.dto.input.CreateMetafieldDefinitionInput;
import com.vointika.metafield.domain.entity.MetafieldDefinition;
import com.vointika.metafield.domain.repository.MetafieldDefinitionRepository;
import com.vointika.metafield.domain.repository.MetaobjectDefinitionRepository;
import com.vointika.metafield.domain.valueobject.MetafieldDefinitionName;
import com.vointika.metafield.domain.valueobject.MetafieldDescription;
import com.vointika.metafield.domain.valueobject.MetafieldKey;
import com.vointika.metafield.domain.valueobject.MetafieldNamespace;
import com.vointika.metafield.domain.valueobject.MetafieldOwnerType;
import com.vointika.metafield.domain.valueobject.MetafieldType;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.exception.ResourceAlreadyExistsException;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.service.IdGenerator;
import com.vointika.shared.valueobject.AuditActor;
import com.vointika.shared.exception.UniqueConstraintViolationException;

import java.util.Map;
import java.util.UUID;

/**
 * Creates a definition. ADMIN+ only. {@code namespace.key} is unique per
 * (operator, owner type) — a duplicate is 409, double-guarded by the
 * pre-check and the DB unique index.
 */
public class CreateMetafieldDefinitionUseCase {

    /** Thrown twice — the pre-check and the index race answer identically. */
    private static final String DUPLICATE_IDENTITY =
            "A metafield definition with this namespace and key already exists";

    private final MetafieldDefinitionRepository definitionRepository;
    private final MetaobjectDefinitionRepository metaobjectDefinitionRepository;
    private final TourOperatorMembershipCheck membershipCheck;
    private final IdGenerator idGenerator;
    private final TransactionRunner transactionRunner;
    private final AuditTrailPort auditTrailPort;

    public CreateMetafieldDefinitionUseCase(MetafieldDefinitionRepository definitionRepository,
                                            MetaobjectDefinitionRepository metaobjectDefinitionRepository,
                                            TourOperatorMembershipCheck membershipCheck,
                                            IdGenerator idGenerator,
                                            TransactionRunner transactionRunner,
                                            AuditTrailPort auditTrailPort) {
        this.definitionRepository = definitionRepository;
        this.metaobjectDefinitionRepository = metaobjectDefinitionRepository;
        this.membershipCheck = membershipCheck;
        this.idGenerator = idGenerator;
        this.transactionRunner = transactionRunner;
        this.auditTrailPort = auditTrailPort;
    }

    public UUID execute(CreateMetafieldDefinitionInput input) {
        membershipCheck.ensureAdmin(input.callerUserId(), input.tourOperatorId());

        MetafieldOwnerType ownerType = MetafieldOwnerType.fromCode(input.ownerType());
        MetafieldNamespace namespace = new MetafieldNamespace(input.namespace());
        MetafieldKey key = new MetafieldKey(input.key());
        MetafieldType type = MetafieldType.fromCode(input.type());
        MetafieldDefinitionName name = new MetafieldDefinitionName(input.name());
        MetafieldDescription description = input.description() == null || input.description().isBlank()
                ? null : new MetafieldDescription(input.description());

        // The pin travels with the reference type and only with it — a
        // reference definition without a target could validate nothing.
        if (type == MetafieldType.METAOBJECT_REFERENCE) {
            if (input.metaobjectDefinitionId() == null) {
                throw new InvalidFieldException(
                        "A metaobject_reference definition must pin a metaobjectDefinitionId");
            }
            if (metaobjectDefinitionRepository
                    .findByIdAndTourOperatorId(input.metaobjectDefinitionId(), input.tourOperatorId())
                    .isEmpty()) {
                throw new InvalidFieldException("Metaobject definition not found");
            }
        } else if (input.metaobjectDefinitionId() != null) {
            throw new InvalidFieldException(
                    "metaobjectDefinitionId only applies to the metaobject_reference type");
        }

        if (definitionRepository.existsByIdentity(
                input.tourOperatorId(), ownerType, namespace.value(), key.value())) {
            throw new ResourceAlreadyExistsException(DUPLICATE_IDENTITY);
        }

        MetafieldDefinition definition = new MetafieldDefinition(
                idGenerator.newId(), input.tourOperatorId(), ownerType,
                namespace, key, type, input.metaobjectDefinitionId(),
                name, description, input.callerUserId());
        try {
            transactionRunner.run(() -> {
                definitionRepository.save(definition);
                auditTrailPort.append(new NewAuditEntry(
                        input.tourOperatorId(), AuditActor.user(input.callerUserId()),
                        "METAFIELD_DEFINITION", definition.getId(), "metafield_definition.created",
                        Map.of("ownerType", ownerType.code(),
                                "namespace", namespace.value(),
                                "key", key.value(),
                                "type", type.code())));
            });
        } catch (UniqueConstraintViolationException e) {
            throw new ResourceAlreadyExistsException(DUPLICATE_IDENTITY);
        }
        return definition.getId();
    }
}
