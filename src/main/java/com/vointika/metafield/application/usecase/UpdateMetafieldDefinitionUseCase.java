package com.vointika.metafield.application.usecase;

import com.vointika.metafield.application.dto.input.UpdateMetafieldDefinitionInput;
import com.vointika.metafield.domain.entity.MetafieldDefinition;
import com.vointika.metafield.domain.repository.MetafieldDefinitionRepository;
import com.vointika.metafield.domain.valueobject.MetafieldDefinitionName;
import com.vointika.metafield.domain.valueobject.MetafieldDescription;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.valueobject.AuditActor;
import com.vointika.shared.valueobject.AuditChanges;
import com.vointika.shared.valueobject.FieldChange;

import java.util.List;
import java.util.Map;

/**
 * Updates the editable attributes (name, description — null/blank clears).
 * ADMIN+ only. ownerType/namespace/key/type are immutable — stored values
 * are addressed through them.
 */
public class UpdateMetafieldDefinitionUseCase {

    private final MetafieldDefinitionRepository definitionRepository;
    private final TourOperatorMembershipCheck membershipCheck;
    private final TransactionRunner transactionRunner;
    private final AuditTrailPort auditTrailPort;

    public UpdateMetafieldDefinitionUseCase(MetafieldDefinitionRepository definitionRepository,
                                            TourOperatorMembershipCheck membershipCheck,
                                            TransactionRunner transactionRunner,
                                            AuditTrailPort auditTrailPort) {
        this.definitionRepository = definitionRepository;
        this.membershipCheck = membershipCheck;
        this.transactionRunner = transactionRunner;
        this.auditTrailPort = auditTrailPort;
    }

    public void execute(UpdateMetafieldDefinitionInput input) {
        membershipCheck.ensureAdmin(input.callerUserId(), input.tourOperatorId());
        MetafieldDefinition definition = definitionRepository
                .requireByIdAndTourOperatorId(input.definitionId(), input.tourOperatorId());

        MetafieldDefinitionName name = new MetafieldDefinitionName(input.name());
        MetafieldDescription description = input.description() == null || input.description().isBlank()
                ? null : new MetafieldDescription(input.description());

        Map<String, Object> before = definition.auditSnapshot();
        definition.update(name, description);
        // A no-op replace saves but records nothing.
        List<FieldChange> changes = AuditChanges.diff(before, definition.auditSnapshot());
        transactionRunner.run(() -> {
            definitionRepository.save(definition);
            if (!changes.isEmpty()) {
                auditTrailPort.append(new NewAuditEntry(
                        input.tourOperatorId(), AuditActor.user(input.callerUserId()),
                        "METAFIELD_DEFINITION", definition.getId(), "metafield_definition.updated",
                        Map.of("namespace", definition.getNamespace().value(),
                                "key", definition.getKey().value()),
                        changes));
            }
        });
    }
}
