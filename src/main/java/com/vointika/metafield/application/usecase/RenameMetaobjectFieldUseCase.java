package com.vointika.metafield.application.usecase;

import com.vointika.metafield.application.dto.input.RenameMetaobjectFieldInput;
import com.vointika.metafield.domain.entity.MetaobjectDefinition;
import com.vointika.metafield.domain.entity.MetaobjectField;
import com.vointika.metafield.domain.repository.MetaobjectDefinitionRepository;
import com.vointika.metafield.domain.valueobject.MetafieldDefinitionName;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.valueobject.AuditActor;
import com.vointika.shared.valueobject.FieldChange;

import java.util.List;
import java.util.Map;

/**
 * Renames a field's display name (key/type are immutable — values are
 * addressed through them). ADMIN+ only. A no-op rename records nothing.
 */
public class RenameMetaobjectFieldUseCase {

    private final MetaobjectDefinitionRepository definitionRepository;
    private final TourOperatorMembershipCheck membershipCheck;
    private final TransactionRunner transactionRunner;
    private final AuditTrailPort auditTrailPort;

    public RenameMetaobjectFieldUseCase(MetaobjectDefinitionRepository definitionRepository,
                                        TourOperatorMembershipCheck membershipCheck,
                                        TransactionRunner transactionRunner,
                                        AuditTrailPort auditTrailPort) {
        this.definitionRepository = definitionRepository;
        this.membershipCheck = membershipCheck;
        this.transactionRunner = transactionRunner;
        this.auditTrailPort = auditTrailPort;
    }

    public void execute(RenameMetaobjectFieldInput input) {
        membershipCheck.ensureAdmin(input.callerUserId(), input.tourOperatorId());
        MetaobjectDefinition definition = definitionRepository
                .requireByIdAndTourOperatorId(input.definitionId(), input.tourOperatorId());
        MetaobjectField field = definitionRepository.requireField(definition.getId(), input.key());

        MetafieldDefinitionName name = new MetafieldDefinitionName(input.name());
        String before = field.getName().value();
        field.rename(name);
        transactionRunner.run(() -> {
            definitionRepository.saveField(field);
            if (!before.equals(field.getName().value())) {
                auditTrailPort.append(new NewAuditEntry(
                        input.tourOperatorId(), AuditActor.user(input.callerUserId()),
                        "METAOBJECT_DEFINITION", definition.getId(),
                        "metaobject_definition.field_updated",
                        Map.of("type", definition.getType().value(), "key", field.getKey().value()),
                        List.of(new FieldChange("name", before, field.getName().value()))));
            }
        });
    }
}
