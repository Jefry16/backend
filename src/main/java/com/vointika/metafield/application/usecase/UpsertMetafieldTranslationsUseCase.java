package com.vointika.metafield.application.usecase;

import com.vointika.metafield.application.dto.input.UpsertMetafieldTranslationsInput;
import com.vointika.metafield.application.service.MetafieldOwnerAccess;
import com.vointika.metafield.application.service.MetafieldValueValidator;
import com.vointika.metafield.domain.entity.MetafieldDefinition;
import com.vointika.metafield.domain.entity.MetafieldValue;
import com.vointika.metafield.domain.entity.MetafieldValueTranslation;
import com.vointika.metafield.domain.repository.MetafieldDefinitionRepository;
import com.vointika.metafield.domain.repository.MetafieldValueRepository;
import com.vointika.metafield.domain.repository.MetafieldValueTranslationRepository;
import com.vointika.metafield.domain.valueobject.MetafieldKey;
import com.vointika.metafield.domain.valueobject.MetafieldNamespace;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.OperatorLocalesQuery;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.valueobject.AuditActor;
import com.vointika.shared.valueobject.LocaleCode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Creates or replaces one owner's metafield translations for one locale. ADMIN+.
 * Owner-generic — the same flow serves the operator, an experience and a page.
 *
 * <p><b>Everything is validated before anything is written.</b> A payload naming
 * ten keys where the seventh is a boolean writes none of them: half a translated
 * locale is worse than a rejected save, because nothing tells the operator which
 * half landed. The transaction is the mechanism; validating up front is what
 * makes the 422 name the offending key instead of the first one that happened to
 * fail mid-flight.
 *
 * <p><b>A blank value clears that key; an absent key is left alone.</b> The
 * distinction matters for an editor that renders one field per translatable
 * metafield: clearing a box means "not translated" and is a delete, while a
 * payload that simply does not mention a key must not delete what it never
 * showed. {@code UpsertOperatorTranslationUseCase} draws the same line.
 */
public class UpsertMetafieldTranslationsUseCase {

    private final MetafieldDefinitionRepository definitionRepository;
    private final MetafieldValueRepository valueRepository;
    private final MetafieldValueTranslationRepository translationRepository;
    private final MetafieldOwnerAccess ownerAccess;
    private final MetafieldValueValidator valueValidator;
    private final OperatorLocalesQuery operatorLocalesQuery;
    private final TourOperatorMembershipCheck membershipCheck;
    private final TransactionRunner transactionRunner;
    private final AuditTrailPort auditTrailPort;

    public UpsertMetafieldTranslationsUseCase(MetafieldDefinitionRepository definitionRepository,
                                              MetafieldValueRepository valueRepository,
                                              MetafieldValueTranslationRepository translationRepository,
                                              MetafieldOwnerAccess ownerAccess,
                                              MetafieldValueValidator valueValidator,
                                              OperatorLocalesQuery operatorLocalesQuery,
                                              TourOperatorMembershipCheck membershipCheck,
                                              TransactionRunner transactionRunner,
                                              AuditTrailPort auditTrailPort) {
        this.definitionRepository = definitionRepository;
        this.valueRepository = valueRepository;
        this.translationRepository = translationRepository;
        this.ownerAccess = ownerAccess;
        this.valueValidator = valueValidator;
        this.operatorLocalesQuery = operatorLocalesQuery;
        this.membershipCheck = membershipCheck;
        this.transactionRunner = transactionRunner;
        this.auditTrailPort = auditTrailPort;
    }

    public void execute(UpsertMetafieldTranslationsInput input) {
        membershipCheck.ensureAdmin(input.callerUserId(), input.tourOperatorId());
        ownerAccess.ensureOwned(input.ownerType(), input.ownerId(), input.tourOperatorId());

        LocaleCode locale = new LocaleCode(input.locale());
        if (!operatorLocalesQuery.findSupportedLocales(input.tourOperatorId()).contains(locale.value())) {
            throw new InvalidFieldException(
                    "Locale '" + locale.value() + "' is not supported by this operator");
        }

        Map<String, String> submitted = input.values() == null ? Map.of() : input.values();
        List<MetafieldValueTranslation> toWrite = new ArrayList<>();
        List<MetafieldValue> toClear = new ArrayList<>();
        // Sorted so the audit entry reads the same for the same edit, whatever
        // order the JSON object arrived in.
        var touched = new TreeSet<String>();

        for (Map.Entry<String, String> entry : new LinkedHashMap<>(submitted).entrySet()) {
            Target target = resolve(input, entry.getKey());
            String raw = entry.getValue();
            touched.add(entry.getKey());
            if (raw == null || raw.isBlank()) {
                toClear.add(target.value());
                continue;
            }
            toWrite.add(MetafieldValueTranslation.of(target.value().getId(), locale.value(),
                    valueValidator.validateAndNormalize(target.definition().getType(), raw),
                    input.callerUserId()));
        }

        if (toWrite.isEmpty() && toClear.isEmpty()) {
            return;
        }

        transactionRunner.run(() -> {
            boolean changed = false;
            for (MetafieldValueTranslation translation : toWrite) {
                translationRepository.upsert(translation);
                changed = true;
            }
            for (MetafieldValue value : toClear) {
                changed |= translationRepository.delete(value.getId(), locale.value());
            }
            if (changed) {
                auditTrailPort.append(new NewAuditEntry(
                        input.tourOperatorId(), AuditActor.user(input.callerUserId()),
                        input.ownerType().auditEntityType(), input.ownerId(),
                        input.ownerType().action("translation_updated"),
                        Map.of("locale", locale.value(), "keys", String.join(", ", touched))));
            }
        });
    }

    /**
     * {@code "custom.opening-hours"} → the value row it names.
     *
     * <p>Each half goes through its own value object, so a malformed namespace or
     * key is refused with the same message the canonical write gives. The split
     * is on the <b>first</b> dot: a key may contain one, a namespace may not.
     */
    private Target resolve(UpsertMetafieldTranslationsInput input, String qualifiedKey) {
        int dot = qualifiedKey == null ? -1 : qualifiedKey.indexOf('.');
        if (dot <= 0 || dot == qualifiedKey.length() - 1) {
            throw new InvalidFieldException(
                    "A metafield translation key must be 'namespace.key', not '" + qualifiedKey + "'");
        }
        MetafieldNamespace namespace = new MetafieldNamespace(qualifiedKey.substring(0, dot));
        MetafieldKey key = new MetafieldKey(qualifiedKey.substring(dot + 1));

        MetafieldDefinition definition = definitionRepository
                .findByIdentity(input.tourOperatorId(), input.ownerType(), namespace.value(), key.value())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Metafield definition not found: " + qualifiedKey));

        if (!definition.getType().isTranslatable()) {
            throw new InvalidFieldException("A " + definition.getType().code()
                    + " metafield cannot be translated: " + qualifiedKey);
        }

        // Translating a value that was never set would leave a row the storefront
        // can never reach — the overlay joins FROM the canonical value.
        MetafieldValue value = valueRepository
                .findByDefinitionIdAndOwnerId(definition.getId(), input.ownerId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Metafield has no value to translate: " + qualifiedKey));
        return new Target(definition, value);
    }

    /** The definition and the value it holds, resolved together because both halves are needed. */
    private record Target(MetafieldDefinition definition, MetafieldValue value) {}
}
