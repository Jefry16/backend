package com.vointika.metafield.application.usecase;

import com.vointika.metafield.application.dto.input.AddMetaobjectFieldInput;
import com.vointika.metafield.domain.entity.MetaobjectDefinition;
import com.vointika.metafield.domain.entity.MetaobjectField;
import com.vointika.metafield.domain.repository.MetaobjectDefinitionRepository;
import com.vointika.metafield.domain.valueobject.MetafieldDefinitionName;
import com.vointika.metafield.domain.valueobject.MetafieldKey;
import com.vointika.metafield.domain.valueobject.MetafieldType;
import com.vointika.shared.exception.ResourceAlreadyExistsException;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.service.IdGenerator;
import com.vointika.shared.valueobject.AuditActor;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Map;

/**
 * Appends a field to an existing definition (position = end). ADMIN+ only.
 * A duplicate key within the definition is 409, double-guarded by the
 * pre-check and the DB unique index. Existing entries simply have the new
 * field unset.
 */
public class AddMetaobjectFieldUseCase {

    private final MetaobjectDefinitionRepository definitionRepository;
    private final TourOperatorMembershipCheck membershipCheck;
    private final IdGenerator idGenerator;
    private final TransactionRunner transactionRunner;
    private final AuditTrailPort auditTrailPort;

    public AddMetaobjectFieldUseCase(MetaobjectDefinitionRepository definitionRepository,
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

    public void execute(AddMetaobjectFieldInput input) {
        membershipCheck.ensureAdmin(input.callerUserId(), input.tourOperatorId());
        MetaobjectDefinition definition = definitionRepository
                .findByIdAndTourOperatorId(input.definitionId(), input.tourOperatorId())
                .orElseThrow(() -> new ResourceNotFoundException("Metaobject definition not found"));

        MetafieldKey key = new MetafieldKey(input.key());
        MetafieldType type = MetafieldType.fromCode(input.type());
        MetafieldDefinitionName name = new MetafieldDefinitionName(input.name());

        if (definitionRepository.existsField(definition.getId(), key.value())) {
            throw new ResourceAlreadyExistsException(
                    "A field with this key already exists on this definition");
        }
        MetaobjectField field = new MetaobjectField(
                idGenerator.newId(), definition.getId(), key, type, name,
                (int) definitionRepository.countFields(definition.getId()) + 1);
        try {
            transactionRunner.run(() -> {
                definitionRepository.saveField(field);
                auditTrailPort.append(new NewAuditEntry(
                        input.tourOperatorId(), AuditActor.user(input.callerUserId()),
                        "METAOBJECT_DEFINITION", definition.getId(),
                        "metaobject_definition.field_added",
                        Map.of("type", definition.getType().value(),
                                "key", key.value(),
                                "fieldType", type.code())));
            });
        } catch (DataIntegrityViolationException e) {
            throw new ResourceAlreadyExistsException(
                    "A field with this key already exists on this definition");
        }
    }
}
