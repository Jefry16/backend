package com.vointika.metafield.application.usecase;

import com.vointika.metafield.application.dto.input.CreateMetaobjectDefinitionInput;
import com.vointika.metafield.domain.entity.MetaobjectDefinition;
import com.vointika.metafield.domain.entity.MetaobjectField;
import com.vointika.metafield.domain.repository.MetaobjectDefinitionRepository;
import com.vointika.metafield.domain.valueobject.MetafieldDefinitionName;
import com.vointika.metafield.domain.valueobject.MetafieldDescription;
import com.vointika.metafield.domain.valueobject.MetafieldKey;
import com.vointika.metafield.domain.valueobject.MetafieldType;
import com.vointika.metafield.domain.valueobject.MetaobjectType;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.exception.ResourceAlreadyExistsException;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.service.IdGenerator;
import com.vointika.shared.valueobject.AuditActor;
import com.vointika.shared.exception.UniqueConstraintViolationException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Creates a metaobject definition WITH its initial fields (at least one —
 * a type with no fields can hold no content). ADMIN+ only. {@code type} is
 * unique per operator — a duplicate is 409, double-guarded by the pre-check
 * and the DB unique index. Duplicate field keys in the payload are 422.
 */
public class CreateMetaobjectDefinitionUseCase {

    /** Thrown twice — the pre-check and the index race answer identically. */
    private static final String DUPLICATE_TYPE =
            "A metaobject definition with this type already exists";

    private final MetaobjectDefinitionRepository definitionRepository;
    private final TourOperatorMembershipCheck membershipCheck;
    private final IdGenerator idGenerator;
    private final TransactionRunner transactionRunner;
    private final AuditTrailPort auditTrailPort;

    public CreateMetaobjectDefinitionUseCase(MetaobjectDefinitionRepository definitionRepository,
                                             TourOperatorMembershipCheck membershipCheck,
                                             IdGenerator idGenerator,
                                             TransactionRunner transactionRunner,
                                             AuditTrailPort auditTrailPort) {
        this.definitionRepository = definitionRepository;
        this.membershipCheck = membershipCheck;
        this.idGenerator = idGenerator;
        this.transactionRunner = transactionRunner;
        this.auditTrailPort = auditTrailPort;
    }

    public UUID execute(CreateMetaobjectDefinitionInput input) {
        membershipCheck.ensureAdmin(input.callerUserId(), input.tourOperatorId());

        MetaobjectType type = new MetaobjectType(input.type());
        MetafieldDefinitionName name = new MetafieldDefinitionName(input.name());
        MetafieldDescription description = input.description() == null || input.description().isBlank()
                ? null : new MetafieldDescription(input.description());

        if (input.fields() == null || input.fields().isEmpty()) {
            throw new InvalidFieldException("A metaobject definition needs at least one field");
        }

        MetaobjectDefinition definition = new MetaobjectDefinition(
                idGenerator.newId(), input.tourOperatorId(), type, name, description,
                input.callerUserId());

        List<MetaobjectField> fields = new ArrayList<>();
        Set<String> seenKeys = new HashSet<>();
        int position = 1;
        for (CreateMetaobjectDefinitionInput.FieldSpec spec : input.fields()) {
            MetafieldKey key = new MetafieldKey(spec.key());
            if (!seenKeys.add(key.value())) {
                throw new InvalidFieldException("Duplicate field key '" + key.value() + "'");
            }
            MetafieldType fieldType = MetafieldType.fromCode(spec.type());
            if (!fieldType.allowedAsMetaobjectField()) {
                // Nested references (metaobject → metaobject) are out for now;
                // the reference type belongs to experience/page metafields.
                throw new InvalidFieldException(
                        "Metaobject fields cannot use the metaobject_reference type");
            }
            fields.add(new MetaobjectField(
                    idGenerator.newId(), definition.getId(), key, fieldType,
                    new MetafieldDefinitionName(spec.name()), position++));
        }

        if (definitionRepository.existsByTourOperatorIdAndType(input.tourOperatorId(), type.value())) {
            throw new ResourceAlreadyExistsException(DUPLICATE_TYPE);
        }
        try {
            transactionRunner.run(() -> {
                definitionRepository.save(definition);
                fields.forEach(definitionRepository::saveField);
                auditTrailPort.append(new NewAuditEntry(
                        input.tourOperatorId(), AuditActor.user(input.callerUserId()),
                        "METAOBJECT_DEFINITION", definition.getId(), "metaobject_definition.created",
                        Map.of("type", type.value(),
                                "fields", fields.stream().map(f -> f.getKey().value()).toList())));
            });
        } catch (UniqueConstraintViolationException e) {
            throw new ResourceAlreadyExistsException(DUPLICATE_TYPE);
        }
        return definition.getId();
    }
}
