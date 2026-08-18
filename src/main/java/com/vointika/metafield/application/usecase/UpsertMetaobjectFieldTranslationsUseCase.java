package com.vointika.metafield.application.usecase;

import com.vointika.metafield.application.dto.input.UpsertMetaobjectFieldTranslationsInput;
import com.vointika.metafield.application.service.MetafieldValueValidator;
import com.vointika.metafield.domain.entity.MetaobjectEntry;
import com.vointika.metafield.domain.entity.MetaobjectEntryValue;
import com.vointika.metafield.domain.entity.MetaobjectEntryValueTranslation;
import com.vointika.metafield.domain.entity.MetaobjectField;
import com.vointika.metafield.domain.repository.MetaobjectDefinitionRepository;
import com.vointika.metafield.domain.repository.MetaobjectEntryRepository;
import com.vointika.metafield.domain.repository.MetaobjectEntryValueTranslationRepository;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.metafield.application.service.OperatorLocaleCheck;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.valueobject.AuditActor;
import com.vointika.shared.valueobject.LocaleCode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;

/**
 * Creates or replaces one metaobject entry's field translations for one locale.
 * ADMIN+.
 *
 * <p>{@code UpsertMetafieldTranslationsUseCase}'s twin, and every rule is the
 * same one: text types only, the whole payload validates before anything is
 * written, a blank clears while an absent key is untouched. What differs is the
 * address — a metaobject field has <b>no namespace</b>, so the map is keyed by
 * the bare field key.
 */
public class UpsertMetaobjectFieldTranslationsUseCase {

    private final MetaobjectEntryRepository entryRepository;
    private final MetaobjectDefinitionRepository definitionRepository;
    private final MetaobjectEntryValueTranslationRepository translationRepository;
    private final MetafieldValueValidator valueValidator;
    private final OperatorLocaleCheck operatorLocaleCheck;
    private final TourOperatorMembershipCheck membershipCheck;
    private final TransactionRunner transactionRunner;
    private final AuditTrailPort auditTrailPort;

    public UpsertMetaobjectFieldTranslationsUseCase(
            MetaobjectEntryRepository entryRepository,
            MetaobjectDefinitionRepository definitionRepository,
            MetaobjectEntryValueTranslationRepository translationRepository,
            MetafieldValueValidator valueValidator,
            OperatorLocaleCheck operatorLocaleCheck,
            TourOperatorMembershipCheck membershipCheck,
            TransactionRunner transactionRunner,
            AuditTrailPort auditTrailPort) {
        this.entryRepository = entryRepository;
        this.definitionRepository = definitionRepository;
        this.translationRepository = translationRepository;
        this.valueValidator = valueValidator;
        this.operatorLocaleCheck = operatorLocaleCheck;
        this.membershipCheck = membershipCheck;
        this.transactionRunner = transactionRunner;
        this.auditTrailPort = auditTrailPort;
    }

    public void execute(UpsertMetaobjectFieldTranslationsInput input) {
        membershipCheck.ensureAdmin(input.callerUserId(), input.tourOperatorId());

        MetaobjectEntry entry = entryRepository
                .requireByIdAndTourOperatorId(input.metaobjectId(), input.tourOperatorId());

        LocaleCode locale = operatorLocaleCheck.require(input.tourOperatorId(), input.locale());

        Map<String, MetaobjectField> fieldsByKey = new LinkedHashMap<>();
        for (MetaobjectField field : definitionRepository.fieldsOf(entry.getDefinitionId())) {
            fieldsByKey.put(field.getKey().value(), field);
        }
        Map<UUID, MetaobjectEntryValue> valuesByField = new LinkedHashMap<>();
        for (MetaobjectEntryValue value : entryRepository.valuesOf(entry.getId())) {
            valuesByField.put(value.getFieldDefinitionId(), value);
        }

        Map<String, String> submitted = input.values() == null ? Map.of() : input.values();
        List<MetaobjectEntryValueTranslation> toWrite = new ArrayList<>();
        List<MetaobjectEntryValue> toClear = new ArrayList<>();
        var touched = new TreeSet<String>();

        for (Map.Entry<String, String> submission : new LinkedHashMap<>(submitted).entrySet()) {
            MetaobjectField field = fieldsByKey.get(submission.getKey());
            if (field == null) {
                throw new ResourceNotFoundException(
                        "Metaobject field not found: " + submission.getKey());
            }
            if (!field.getType().isTranslatable()) {
                throw new InvalidFieldException("A " + field.getType().code()
                        + " field cannot be translated: " + submission.getKey());
            }
            // Same reason as the metafield twin: the overlay joins FROM the
            // stored value, so translating an unset field is a row nothing reads.
            MetaobjectEntryValue value = valuesByField.get(field.getId());
            if (value == null) {
                throw new ResourceNotFoundException(
                        "Metaobject field has no value to translate: " + submission.getKey());
            }

            touched.add(submission.getKey());
            String raw = submission.getValue();
            if (raw == null || raw.isBlank()) {
                toClear.add(value);
                continue;
            }
            toWrite.add(MetaobjectEntryValueTranslation.of(value.getId(), locale.value(),
                    valueValidator.validateAndNormalize(field.getType(), raw),
                    input.callerUserId()));
        }

        if (toWrite.isEmpty() && toClear.isEmpty()) {
            return;
        }

        transactionRunner.run(() -> {
            boolean changed = false;
            for (MetaobjectEntryValueTranslation translation : toWrite) {
                translationRepository.upsert(translation);
                changed = true;
            }
            for (MetaobjectEntryValue value : toClear) {
                changed |= translationRepository.delete(value.getId(), locale.value());
            }
            if (changed) {
                auditTrailPort.append(new NewAuditEntry(
                        input.tourOperatorId(), AuditActor.user(input.callerUserId()),
                        "METAOBJECT", entry.getId(), "metaobject.translation_updated",
                        Map.of("locale", locale.value(), "fields", String.join(", ", touched))));
            }
        });
    }
}
