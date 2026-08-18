package com.vointika.metafield.application.usecase;

import com.vointika.metafield.application.dto.input.UpdateMetaobjectDefinitionInput;
import com.vointika.metafield.domain.entity.MetaobjectDefinition;
import com.vointika.metafield.domain.repository.MetaobjectDefinitionRepository;
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
 * ADMIN+ only. {@code type} and the field set are managed on their own
 * endpoints; {@code type} never changes at all.
 */
public class UpdateMetaobjectDefinitionUseCase {

    private final MetaobjectDefinitionRepository definitionRepository;
    private final TourOperatorMembershipCheck membershipCheck;
    private final TransactionRunner transactionRunner;
    private final AuditTrailPort auditTrailPort;

    public UpdateMetaobjectDefinitionUseCase(MetaobjectDefinitionRepository definitionRepository,
                                             TourOperatorMembershipCheck membershipCheck,
                                             TransactionRunner transactionRunner,
                                             AuditTrailPort auditTrailPort) {
        this.definitionRepository = definitionRepository;
        this.membershipCheck = membershipCheck;
        this.transactionRunner = transactionRunner;
        this.auditTrailPort = auditTrailPort;
    }

    public void execute(UpdateMetaobjectDefinitionInput input) {
        membershipCheck.ensureAdmin(input.callerUserId(), input.tourOperatorId());
        MetaobjectDefinition definition = definitionRepository
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
                        "METAOBJECT_DEFINITION", definition.getId(), "metaobject_definition.updated",
                        Map.of("type", definition.getType().value()),
                        changes));
            }
        });
    }
}
