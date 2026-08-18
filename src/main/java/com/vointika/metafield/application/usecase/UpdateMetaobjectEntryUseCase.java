package com.vointika.metafield.application.usecase;

import com.vointika.metafield.application.dto.input.UpdateMetaobjectEntryInput;
import com.vointika.metafield.application.service.MetafieldValueValidator;
import com.vointika.metafield.domain.entity.MetaobjectDefinition;
import com.vointika.metafield.domain.entity.MetaobjectEntry;
import com.vointika.metafield.domain.entity.MetaobjectEntryValue;
import com.vointika.metafield.domain.entity.MetaobjectField;
import com.vointika.metafield.domain.repository.MetaobjectDefinitionRepository;
import com.vointika.metafield.domain.repository.MetaobjectEntryRepository;
import com.vointika.metafield.domain.valueobject.MetaobjectEntryName;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.exception.ResourceAlreadyExistsException;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.service.IdGenerator;
import com.vointika.shared.valueobject.AuditActor;
import com.vointika.shared.valueobject.FieldChange;
import com.vointika.shared.valueobject.Handle;
import com.vointika.shared.exception.UniqueConstraintViolationException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * PATCHes an entry: name and/or handle (null keeps current) and any subset of
 * field values (null/blank clears, anything else validates + replaces).
 * ADMIN+ only. Every real change lands in ONE audit entry — identity diffs
 * by attribute, values by field key. No-ops record nothing.
 */
public class UpdateMetaobjectEntryUseCase {

    private final MetaobjectDefinitionRepository definitionRepository;
    private final MetaobjectEntryRepository entryRepository;
    private final MetafieldValueValidator valueValidator;
    private final TourOperatorMembershipCheck membershipCheck;
    private final IdGenerator idGenerator;
    private final TransactionRunner transactionRunner;
    private final AuditTrailPort auditTrailPort;

    public UpdateMetaobjectEntryUseCase(MetaobjectDefinitionRepository definitionRepository,
                                        MetaobjectEntryRepository entryRepository,
                                        MetafieldValueValidator valueValidator,
                                        TourOperatorMembershipCheck membershipCheck,
                                        IdGenerator idGenerator,
                                        TransactionRunner transactionRunner,
                                        AuditTrailPort auditTrailPort) {
        this.definitionRepository = definitionRepository;
        this.entryRepository = entryRepository;
        this.valueValidator = valueValidator;
        this.membershipCheck = membershipCheck;
        this.idGenerator = idGenerator;
        this.transactionRunner = transactionRunner;
        this.auditTrailPort = auditTrailPort;
    }

    public void execute(UpdateMetaobjectEntryInput input) {
        membershipCheck.ensureAdmin(input.callerUserId(), input.tourOperatorId());
        MetaobjectEntry entry = entryRepository
                .requireByIdAndTourOperatorId(input.entryId(), input.tourOperatorId());
        MetaobjectDefinition definition = definitionRepository
                .requireByIdAndTourOperatorId(entry.getDefinitionId(), input.tourOperatorId());

        List<FieldChange> changes = new ArrayList<>();

        // Identity: null keeps the current value; a changed handle re-checks
        // uniqueness (per definition).
        MetaobjectEntryName name = input.name() == null
                ? entry.getName() : new MetaobjectEntryName(input.name());
        Handle handle = input.handle() == null ? entry.getHandle() : new Handle(input.handle());
        if (!handle.value().equals(entry.getHandle().value())
                && entryRepository.existsByDefinitionIdAndHandle(
                        entry.getDefinitionId(), handle.value())) {
            throw new ResourceAlreadyExistsException(
                    "A metaobject with this handle already exists for this type");
        }
        if (!name.value().equals(entry.getName().value())) {
            changes.add(new FieldChange("name", entry.getName().value(), name.value()));
        }
        if (!handle.value().equals(entry.getHandle().value())) {
            changes.add(new FieldChange("handle", entry.getHandle().value(), handle.value()));
        }
        entry.update(name, handle);

        // Values: validate every touched key up front, then apply in the tx.
        Map<String, MetaobjectField> fieldsByKey = new HashMap<>();
        for (MetaobjectField field : definitionRepository.fieldsOf(definition.getId())) {
            fieldsByKey.put(field.getKey().value(), field);
        }
        List<Runnable> valueWrites = new ArrayList<>();
        if (input.values() != null) {
            for (Map.Entry<String, String> provided : input.values().entrySet()) {
                MetaobjectField field = fieldsByKey.get(provided.getKey());
                if (field == null) {
                    throw new InvalidFieldException(
                            "Unknown metaobject field '" + provided.getKey() + "'");
                }
                Optional<MetaobjectEntryValue> existing =
                        entryRepository.findValue(entry.getId(), field.getId());
                String prior = existing.map(MetaobjectEntryValue::getValue).orElse(null);
                boolean clearing = provided.getValue() == null || provided.getValue().isBlank();
                if (clearing) {
                    if (existing.isPresent()) {
                        changes.add(new FieldChange(field.getKey().value(), prior, null));
                        MetaobjectEntryValue row = existing.get();
                        valueWrites.add(() -> entryRepository.deleteValue(row.getId()));
                    }
                    continue;
                }
                String normalized = valueValidator.validateAndNormalize(
                        field.getType(), provided.getValue());
                if (normalized.equals(prior)) {
                    continue;
                }
                changes.add(new FieldChange(field.getKey().value(), prior, normalized));
                if (existing.isPresent()) {
                    MetaobjectEntryValue row = existing.get();
                    row.changeValue(normalized);
                    valueWrites.add(() -> entryRepository.saveValue(row));
                } else {
                    MetaobjectEntryValue row = new MetaobjectEntryValue(
                            idGenerator.newId(), entry.getId(), field.getId(),
                            normalized, input.callerUserId());
                    valueWrites.add(() -> entryRepository.saveValue(row));
                }
            }
        }

        try {
            transactionRunner.run(() -> {
                entryRepository.save(entry);
                valueWrites.forEach(Runnable::run);
                if (!changes.isEmpty()) {
                    auditTrailPort.append(new NewAuditEntry(
                            input.tourOperatorId(), AuditActor.user(input.callerUserId()),
                            "METAOBJECT", entry.getId(), "metaobject.updated",
                            Map.of("type", definition.getType().value(),
                                    "handle", entry.getHandle().value()),
                            changes));
                }
            });
        } catch (UniqueConstraintViolationException e) {
            // Concurrent handle rename past the pre-check.
            throw new ResourceAlreadyExistsException(
                    "A metaobject with this handle already exists for this type");
        }
    }
}
