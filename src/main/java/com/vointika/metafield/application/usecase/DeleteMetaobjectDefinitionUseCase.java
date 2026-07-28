package com.vointika.metafield.application.usecase;

import com.vointika.metafield.domain.entity.MetaobjectDefinition;
import com.vointika.metafield.domain.repository.MetaobjectDefinitionRepository;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.valueobject.AuditActor;

import java.util.Map;
import java.util.UUID;

/**
 * Deletes a definition — CASCADES its fields, every entry and every stored
 * value. ADMIN+ only; the UI carries the warning.
 */
public class DeleteMetaobjectDefinitionUseCase {

    private final MetaobjectDefinitionRepository definitionRepository;
    private final TourOperatorMembershipCheck membershipCheck;
    private final TransactionRunner transactionRunner;
    private final AuditTrailPort auditTrailPort;

    public DeleteMetaobjectDefinitionUseCase(MetaobjectDefinitionRepository definitionRepository,
                                             TourOperatorMembershipCheck membershipCheck,
                                             TransactionRunner transactionRunner,
                                             AuditTrailPort auditTrailPort) {
        this.definitionRepository = definitionRepository;
        this.membershipCheck = membershipCheck;
        this.transactionRunner = transactionRunner;
        this.auditTrailPort = auditTrailPort;
    }

    public void execute(UUID tourOperatorId, UUID definitionId, UUID callerUserId) {
        membershipCheck.ensureAdmin(callerUserId, tourOperatorId);
        MetaobjectDefinition definition = definitionRepository
                .findByIdAndTourOperatorId(definitionId, tourOperatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Metaobject definition not found"));
        // A delete leaves no row to diff — identity rides details.
        transactionRunner.run(() -> {
            definitionRepository.delete(definitionId);
            auditTrailPort.append(new NewAuditEntry(
                    tourOperatorId, AuditActor.user(callerUserId),
                    "METAOBJECT_DEFINITION", definitionId, "metaobject_definition.deleted",
                    Map.of("type", definition.getType().value(),
                            "name", definition.getName().value())));
        });
    }
}
