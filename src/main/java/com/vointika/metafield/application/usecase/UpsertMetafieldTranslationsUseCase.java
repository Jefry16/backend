package com.vointika.metafield.application.usecase;

import com.vointika.metafield.domain.valueobject.MetafieldType;
import com.vointika.metafield.application.dto.input.UpsertMetafieldTranslationsInput;
import com.vointika.metafield.application.service.MetafieldOwnerAccess;
import com.vointika.metafield.application.service.MetafieldValueValidator;
import com.vointika.metafield.domain.entity.MetafieldValueTranslation;
import com.vointika.metafield.domain.projection.TranslatableMetafieldValue;
import com.vointika.metafield.domain.repository.MetafieldValueRepository;
import com.vointika.metafield.domain.repository.MetafieldValueTranslationRepository;
import com.vointika.metafield.domain.valueobject.MetafieldKey;
import com.vointika.metafield.domain.valueobject.MetafieldNamespace;
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

    /** Public because it is published — the documentation test builds its example from it. */
    public static String notTranslatableMessage(MetafieldType type, String qualifiedKey) {
        return "A " + type.code() + " metafield cannot be translated: " + qualifiedKey;
    }

    private final MetafieldValueRepository valueRepository;
    private final MetafieldValueTranslationRepository translationRepository;
    private final MetafieldOwnerAccess ownerAccess;
    private final MetafieldValueValidator valueValidator;
    private final OperatorLocaleCheck operatorLocaleCheck;
    private final TourOperatorMembershipCheck membershipCheck;
    private final TransactionRunner transactionRunner;
    private final AuditTrailPort auditTrailPort;

    public UpsertMetafieldTranslationsUseCase(MetafieldValueRepository valueRepository,
                                              MetafieldValueTranslationRepository translationRepository,
                                              MetafieldOwnerAccess ownerAccess,
                                              MetafieldValueValidator valueValidator,
                                              OperatorLocaleCheck operatorLocaleCheck,
                                              TourOperatorMembershipCheck membershipCheck,
                                              TransactionRunner transactionRunner,
                                              AuditTrailPort auditTrailPort) {
        this.valueRepository = valueRepository;
        this.translationRepository = translationRepository;
        this.ownerAccess = ownerAccess;
        this.valueValidator = valueValidator;
        this.operatorLocaleCheck = operatorLocaleCheck;
        this.membershipCheck = membershipCheck;
        this.transactionRunner = transactionRunner;
        this.auditTrailPort = auditTrailPort;
    }

    public void execute(UpsertMetafieldTranslationsInput input) {
        membershipCheck.ensureAdmin(input.callerUserId(), input.tourOperatorId());
        ownerAccess.ensureOwned(input.ownerType(), input.ownerId(), input.tourOperatorId());

        LocaleCode locale = operatorLocaleCheck.require(input.tourOperatorId(), input.locale());

        Map<String, String> submitted = input.values() == null ? Map.of() : input.values();
        List<MetafieldValueTranslation> toWrite = new ArrayList<>();
        List<UUID> toClear = new ArrayList<>();
        // Sorted so the audit entry reads the same for the same edit, whatever
        // order the JSON object arrived in.
        var touched = new TreeSet<String>();

        // One query for the whole payload rather than two per key. The metaobject
        // twin loads its fields and values the same way, and a per-key resolve made
        // a ten-key save cost twenty round trips.
        Map<String, TranslatableMetafieldValue> byQualifiedKey = new LinkedHashMap<>();
        for (TranslatableMetafieldValue v : valueRepository.listTranslatableForOwner(
                input.tourOperatorId(), input.ownerType(), input.ownerId())) {
            byQualifiedKey.put(v.qualifiedKey(), v);
        }

        for (Map.Entry<String, String> entry : new LinkedHashMap<>(submitted).entrySet()) {
            TranslatableMetafieldValue target = resolve(byQualifiedKey, entry.getKey());
            String raw = entry.getValue();
            touched.add(entry.getKey());
            if (raw == null || raw.isBlank()) {
                toClear.add(target.valueId());
                continue;
            }
            toWrite.add(MetafieldValueTranslation.of(target.valueId(), locale.value(),
                    valueValidator.validateAndNormalize(target.type(), raw),
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
            for (UUID valueId : toClear) {
                changed |= translationRepository.delete(valueId, locale.value());
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
     * {@code "custom.opening-hours"} → the value it names, out of the map loaded
     * above.
     *
     * <p>The key is split on the <b>first</b> dot: a key may contain one, a
     * namespace may not. Both halves still go through their value objects, so a
     * malformed name is refused with the same message the canonical write gives —
     * and refused <em>before</em> the map lookup, or a key like {@code "..."}
     * would report "not found" rather than "not a key".
     *
     * <p>A key with no stored value is a 404 rather than an empty translation
     * row: the storefront's overlay joins FROM the canonical value, so a
     * translation of nothing is a row nothing can ever read.
     */
    private static TranslatableMetafieldValue resolve(
            Map<String, TranslatableMetafieldValue> byQualifiedKey, String qualifiedKey) {
        int dot = qualifiedKey == null ? -1 : qualifiedKey.indexOf('.');
        if (dot <= 0 || dot == qualifiedKey.length() - 1) {
            throw new InvalidFieldException(
                    "A metafield translation key must be 'namespace.key', not '" + qualifiedKey + "'");
        }
        String namespace = new MetafieldNamespace(qualifiedKey.substring(0, dot)).value();
        String key = new MetafieldKey(qualifiedKey.substring(dot + 1)).value();

        TranslatableMetafieldValue target = byQualifiedKey.get(namespace + "." + key);
        if (target == null) {
            throw new ResourceNotFoundException(
                    "Metafield has no value to translate: " + qualifiedKey);
        }
        if (!target.type().isTranslatable()) {
            throw new InvalidFieldException(
                    notTranslatableMessage(target.type(), qualifiedKey));
        }
        return target;
    }
}
