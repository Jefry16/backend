package com.vointika.metafield.application.usecase;

import com.vointika.metafield.domain.entity.MetaobjectDefinition;
import com.vointika.metafield.domain.entity.MetaobjectField;
import com.vointika.metafield.domain.repository.MetaobjectDefinitionRepository;
import com.vointika.shared.exception.ConflictException;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.valueobject.AuditActor;

import java.util.Map;
import java.util.UUID;

/**
 * Removes a field from a definition — CASCADES every entry's stored value
 * for it (the UI carries the warning). ADMIN+ only. The last field cannot
 * be removed (a type with no fields can hold no content) — 409.
 */
public class RemoveMetaobjectFieldUseCase {

    private final MetaobjectDefinitionRepository definitionRepository;
    private final TourOperatorMembershipCheck membershipCheck;
    private final TransactionRunner transactionRunner;
    private final AuditTrailPort auditTrailPort;

    public RemoveMetaobjectFieldUseCase(MetaobjectDefinitionRepository definitionRepository,
                                        TourOperatorMembershipCheck membershipCheck,
                                        TransactionRunner transactionRunner,
                                        AuditTrailPort auditTrailPort) {
        this.definitionRepository = definitionRepository;
        this.membershipCheck = membershipCheck;
        this.transactionRunner = transactionRunner;
        this.auditTrailPort = auditTrailPort;
    }

    public void execute(UUID tourOperatorId, UUID definitionId, String key, UUID callerUserId) {
        membershipCheck.ensureAdmin(callerUserId, tourOperatorId);
        MetaobjectDefinition definition = definitionRepository.requireByIdAndTourOperatorId(definitionId, tourOperatorId);
        MetaobjectField field = definitionRepository.requireField(definition.getId(), key);

        if (definitionRepository.countFields(definition.getId()) <= 1) {
            throw new ConflictException("A metaobject definition must keep at least one field");
        }
        transactionRunner.run(() -> {
            definitionRepository.deleteField(field.getId());
            auditTrailPort.append(new NewAuditEntry(
                    tourOperatorId, AuditActor.user(callerUserId),
                    "METAOBJECT_DEFINITION", definition.getId(),
                    "metaobject_definition.field_removed",
                    Map.of("type", definition.getType().value(), "key", field.getKey().value())));
        });
    }
}
