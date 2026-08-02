package com.vointika.metafield.application.usecase;

import com.vointika.metafield.application.dto.input.CreateMetaobjectEntryInput;
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
import com.vointika.shared.valueobject.Handle;
import com.vointika.shared.exception.UniqueConstraintViolationException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Creates an entry of a definition — always unpublished; every provided
 * value is validated against its field's type (unknown key → 422, bad value
 * → 422; null/blank values are simply unset). ADMIN+ only. The handle is
 * unique per definition — a duplicate is 409, double-guarded.
 */
public class CreateMetaobjectEntryUseCase {

    private final MetaobjectDefinitionRepository definitionRepository;
    private final MetaobjectEntryRepository entryRepository;
    private final MetafieldValueValidator valueValidator;
    private final TourOperatorMembershipCheck membershipCheck;
    private final IdGenerator idGenerator;
    private final TransactionRunner transactionRunner;
    private final AuditTrailPort auditTrailPort;

    public CreateMetaobjectEntryUseCase(MetaobjectDefinitionRepository definitionRepository,
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

    public UUID execute(CreateMetaobjectEntryInput input) {
        membershipCheck.ensureAdmin(input.callerUserId(), input.tourOperatorId());
        MetaobjectDefinition definition = definitionRepository
                .findByIdAndTourOperatorId(input.definitionId(), input.tourOperatorId())
                .orElseThrow(() -> new ResourceNotFoundException("Metaobject definition not found"));

        Handle handle = new Handle(input.handle());
        MetaobjectEntryName name = new MetaobjectEntryName(input.name());

        Map<String, MetaobjectField> fieldsByKey = new HashMap<>();
        for (MetaobjectField field : definitionRepository.fieldsOf(definition.getId())) {
            fieldsByKey.put(field.getKey().value(), field);
        }

        MetaobjectEntry entry = new MetaobjectEntry(
                idGenerator.newId(), input.tourOperatorId(), definition.getId(),
                handle, name, input.callerUserId());

        List<MetaobjectEntryValue> values = new ArrayList<>();
        if (input.values() != null) {
            for (Map.Entry<String, String> provided : input.values().entrySet()) {
                MetaobjectField field = fieldsByKey.get(provided.getKey());
                if (field == null) {
                    throw new InvalidFieldException(
                            "Unknown metaobject field '" + provided.getKey() + "'");
                }
                if (provided.getValue() == null || provided.getValue().isBlank()) {
                    continue; // unset
                }
                String normalized = valueValidator.validateAndNormalize(
                        field.getType(), provided.getValue());
                values.add(new MetaobjectEntryValue(
                        idGenerator.newId(), entry.getId(), field.getId(),
                        normalized, input.callerUserId()));
            }
        }

        if (entryRepository.existsByDefinitionIdAndHandle(definition.getId(), handle.value())) {
            throw new ResourceAlreadyExistsException(
                    "A metaobject with this handle already exists for this type");
        }
        try {
            transactionRunner.run(() -> {
                entryRepository.save(entry);
                values.forEach(entryRepository::saveValue);
                auditTrailPort.append(new NewAuditEntry(
                        input.tourOperatorId(), AuditActor.user(input.callerUserId()),
                        "METAOBJECT", entry.getId(), "metaobject.created",
                        Map.of("type", definition.getType().value(),
                                "handle", handle.value(),
                                "name", name.value())));
            });
        } catch (UniqueConstraintViolationException e) {
            throw new ResourceAlreadyExistsException(
                    "A metaobject with this handle already exists for this type");
        }
        return entry.getId();
    }
}
