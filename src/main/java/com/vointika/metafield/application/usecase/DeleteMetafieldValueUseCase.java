package com.vointika.metafield.application.usecase;

import com.vointika.metafield.application.service.MetafieldOwnerAccess;
import com.vointika.metafield.domain.entity.MetafieldValue;
import com.vointika.metafield.domain.repository.MetafieldDefinitionRepository;
import com.vointika.metafield.domain.repository.MetafieldValueRepository;
import com.vointika.metafield.domain.valueobject.MetafieldKey;
import com.vointika.metafield.domain.valueobject.MetafieldNamespace;
import com.vointika.metafield.domain.valueobject.MetafieldOwnerType;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.valueobject.AuditActor;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Clears one metafield value on a resource. ADMIN+. Probe-first idempotent:
 * clearing a value that isn't set removes nothing and records nothing.
 */
public class DeleteMetafieldValueUseCase {

    private final MetafieldDefinitionRepository definitionRepository;
    private final MetafieldValueRepository valueRepository;
    private final MetafieldOwnerAccess ownerAccess;
    private final TourOperatorMembershipCheck membershipCheck;
    private final TransactionRunner transactionRunner;
    private final AuditTrailPort auditTrailPort;

    public DeleteMetafieldValueUseCase(MetafieldDefinitionRepository definitionRepository,
                                       MetafieldValueRepository valueRepository,
                                       MetafieldOwnerAccess ownerAccess,
                                       TourOperatorMembershipCheck membershipCheck,
                                       TransactionRunner transactionRunner,
                                       AuditTrailPort auditTrailPort) {
        this.definitionRepository = definitionRepository;
        this.valueRepository = valueRepository;
        this.ownerAccess = ownerAccess;
        this.membershipCheck = membershipCheck;
        this.transactionRunner = transactionRunner;
        this.auditTrailPort = auditTrailPort;
    }

    public void execute(UUID tourOperatorId, MetafieldOwnerType ownerType, UUID ownerId,
                        String rawNamespace, String rawKey, UUID callerUserId) {
        membershipCheck.ensureAdmin(callerUserId, tourOperatorId);
        ownerAccess.ensureOwned(ownerType, ownerId, tourOperatorId);

        MetafieldNamespace namespace = new MetafieldNamespace(rawNamespace);
        MetafieldKey key = new MetafieldKey(rawKey);
        var definition = definitionRepository
                .findByIdentity(tourOperatorId, ownerType, namespace.value(), key.value())
                .orElseThrow(() -> new ResourceNotFoundException("Metafield definition not found"));

        Optional<MetafieldValue> existing =
                valueRepository.findByDefinitionIdAndOwnerId(definition.getId(), ownerId);
        if (existing.isEmpty()) {
            return;
        }
        transactionRunner.run(() -> {
            valueRepository.delete(existing.get().getId());
            auditTrailPort.append(new NewAuditEntry(
                    tourOperatorId, AuditActor.user(callerUserId),
                    ownerType.auditEntityType(), ownerId,
                    ownerType.action("cleared"),
                    Map.of("namespace", namespace.value(), "key", key.value())));
        });
    }
}
