package com.vointika.metafield.application.usecase;

import com.vointika.metafield.domain.entity.MetaobjectDefinition;
import com.vointika.metafield.domain.repository.MetafieldDefinitionRepository;
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
 * Deletes a definition — CASCADES its fields, every entry and every stored
 * value. ADMIN+ only; the UI carries the warning.
 *
 * <p>A type still pinned by a {@code metaobject_reference} metafield is refused
 * (409). That is <b>asked</b>, not caught: the FK backing it raises SQLSTATE
 * 23503 → Spring's {@code DataIntegrityViolationException}, and the transaction
 * runner translates only {@code DuplicateKeyException} — deliberately, so a
 * genuine integrity defect stays the 500 it is. A request that loses a race with
 * a concurrently created reference therefore still 500s, which is the intended
 * outcome for an unexpected constraint failure.
 */
public class DeleteMetaobjectDefinitionUseCase {

    private final MetaobjectDefinitionRepository definitionRepository;
    private final MetafieldDefinitionRepository metafieldDefinitionRepository;
    private final TourOperatorMembershipCheck membershipCheck;
    private final TransactionRunner transactionRunner;
    private final AuditTrailPort auditTrailPort;

    public DeleteMetaobjectDefinitionUseCase(MetaobjectDefinitionRepository definitionRepository,
                                             MetafieldDefinitionRepository metafieldDefinitionRepository,
                                             TourOperatorMembershipCheck membershipCheck,
                                             TransactionRunner transactionRunner,
                                             AuditTrailPort auditTrailPort) {
        this.definitionRepository = definitionRepository;
        this.metafieldDefinitionRepository = metafieldDefinitionRepository;
        this.membershipCheck = membershipCheck;
        this.transactionRunner = transactionRunner;
        this.auditTrailPort = auditTrailPort;
    }

    public void execute(UUID tourOperatorId, UUID definitionId, UUID callerUserId) {
        membershipCheck.ensureAdmin(callerUserId, tourOperatorId);
        MetaobjectDefinition definition = definitionRepository
                .findByIdAndTourOperatorId(definitionId, tourOperatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Metaobject definition not found"));

        if (metafieldDefinitionRepository.existsPinningMetaobjectDefinition(definitionId)) {
            throw new ConflictException(
                    "A metafield definition references this metaobject type — delete it first");
        }

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
