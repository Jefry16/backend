package com.vointika.metafield.application.usecase;

import com.vointika.metafield.domain.entity.MetafieldDefinition;
import com.vointika.metafield.domain.repository.MetafieldDefinitionRepository;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.valueobject.AuditActor;

import java.util.Map;
import java.util.UUID;

/**
 * Deletes a definition. ADMIN+ only. This CASCADES: every resource's value
 * for this definition goes with it (FK) — which is exactly why the audit
 * entry records what was deleted.
 */
public class DeleteMetafieldDefinitionUseCase {

    private final MetafieldDefinitionRepository definitionRepository;
    private final TourOperatorMembershipCheck membershipCheck;
    private final TransactionRunner transactionRunner;
    private final AuditTrailPort auditTrailPort;

    public DeleteMetafieldDefinitionUseCase(MetafieldDefinitionRepository definitionRepository,
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
        MetafieldDefinition definition = definitionRepository
                .findByIdAndTourOperatorId(definitionId, tourOperatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Metafield definition not found"));
        transactionRunner.run(() -> {
            definitionRepository.delete(definitionId);
            auditTrailPort.append(new NewAuditEntry(
                    tourOperatorId, AuditActor.user(callerUserId),
                    "METAFIELD_DEFINITION", definitionId, "metafield_definition.deleted",
                    Map.of("ownerType", definition.getOwnerType().code(),
                            "namespace", definition.getNamespace().value(),
                            "key", definition.getKey().value(),
                            "name", definition.getName().value())));
        });
    }
}
