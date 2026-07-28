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
import org.springframework.dao.DataIntegrityViolationException;

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
            fields.add(new MetaobjectField(
                    idGenerator.newId(), definition.getId(), key,
                    MetafieldType.fromCode(spec.type()),
                    new MetafieldDefinitionName(spec.name()), position++));
        }

        if (definitionRepository.existsByTourOperatorIdAndType(input.tourOperatorId(), type.value())) {
            throw new ResourceAlreadyExistsException(
                    "A metaobject definition with this type already exists");
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
        } catch (DataIntegrityViolationException e) {
            throw new ResourceAlreadyExistsException(
                    "A metaobject definition with this type already exists");
        }
        return definition.getId();
    }
}
