package com.vointika.experience.application.usecase;

import com.vointika.experience.application.dto.input.UpsertExperienceTranslationInput;
import com.vointika.experience.domain.entity.ExperienceTranslation;
import com.vointika.experience.domain.repository.ExperienceRepository;
import com.vointika.experience.domain.repository.ExperienceTranslationRepository;
import com.vointika.experience.domain.valueobject.Description;
import com.vointika.experience.domain.valueobject.ExperienceName;
import com.vointika.experience.domain.valueobject.Highlight;
import com.vointika.experience.domain.valueobject.InclusionItem;
import com.vointika.experience.domain.valueobject.LongDescription;
import com.vointika.experience.domain.valueobject.SeoDescription;
import com.vointika.experience.domain.valueobject.SeoTitle;
import com.vointika.shared.exception.ResourceAlreadyExistsException;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.OperatorLocalesQuery;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.service.HandleGenerator;
import com.vointika.shared.valueobject.AuditActor;
import com.vointika.shared.valueobject.LocaleCode;
import com.vointika.shared.valueobject.Handle;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * Creates or replaces the translation overlay for one (experience, locale).
 * ADMIN+; membership enforced by the interceptor.
 *
 * <p>Guards: caller not ADMIN+ → 403; experience not under this operator → 404;
 * bad-shape or unsupported locale → 422 (the locale must be in the operator's
 * supported set — {@link OperatorLocalesQuery}); an explicit localized handle
 * already used by another experience — as a localized handle in this locale, or as
 * its canonical handle — → 409. A blank field is treated as untranslated (null →
 * falls back to canonical).
 *
 * <p>Handle precedence: an explicit handle is validated + uniqueness-probed (no
 * auto-suffix — the admin chose it); else a translated {@code name} derives a
 * unique localized handle (auto-suffix); else null. Both probe <em>both</em>
 * namespaces: the storefront resolves a handle against localized handles first and
 * canonical ones second, so a handle taken from either shadows the other (PATTERNS
 * §4d). No audit (no audit context yet).
 */
public class UpsertExperienceTranslationUseCase {

    private final ExperienceRepository experienceRepository;
    private final ExperienceTranslationRepository translationRepository;
    private final OperatorLocalesQuery operatorLocalesQuery;
    private final HandleGenerator handleGenerator;
    private final TourOperatorMembershipCheck membershipCheck;
    private final TransactionRunner transactionRunner;
    private final AuditTrailPort auditTrailPort;

    public UpsertExperienceTranslationUseCase(ExperienceRepository experienceRepository,
                                              ExperienceTranslationRepository translationRepository,
                                              OperatorLocalesQuery operatorLocalesQuery,
                                              HandleGenerator handleGenerator,
                                              TourOperatorMembershipCheck membershipCheck,
                                              TransactionRunner transactionRunner,
                                              AuditTrailPort auditTrailPort) {
        this.experienceRepository = experienceRepository;
        this.translationRepository = translationRepository;
        this.operatorLocalesQuery = operatorLocalesQuery;
        this.handleGenerator = handleGenerator;
        this.membershipCheck = membershipCheck;
        this.transactionRunner = transactionRunner;
        this.auditTrailPort = auditTrailPort;
    }

    public void execute(UUID tourOperatorId, UUID experienceId, String rawLocale,
                        UpsertExperienceTranslationInput input, UUID callerUserId) {
        membershipCheck.ensureAdmin(callerUserId, tourOperatorId);
        LocaleCode locale = new LocaleCode(rawLocale);

        if (experienceRepository.findByIdAndTourOperatorId(experienceId, tourOperatorId).isEmpty()) {
            throw new ResourceNotFoundException("Experience not found");
        }
        if (!operatorLocalesQuery.findSupportedLocales(tourOperatorId).contains(locale.value())) {
            throw new InvalidFieldException("Locale '" + locale.value() + "' is not supported by this operator");
        }

        ExperienceName name = input.name() == null || input.name().isBlank() ? null : new ExperienceName(input.name());
        Description description = blankNull(input.description()) == null ? null : new Description(input.description());
        LongDescription longDescription = blankNull(input.longDescription()) == null ? null : new LongDescription(input.longDescription());
        List<Highlight> highlights = mapList(input.highlights(), Highlight::new);
        List<InclusionItem> included = mapList(input.included(), InclusionItem::new);
        List<InclusionItem> notIncluded = mapList(input.notIncluded(), InclusionItem::new);
        Handle handle = resolveHandle(tourOperatorId, experienceId, locale, input.handle(), name);

        transactionRunner.run(() -> {
            translationRepository.upsert(new ExperienceTranslation(
                    experienceId, tourOperatorId, locale,
                    name, description, longDescription, highlights, included, notIncluded, handle,
                    blankNull(input.seoTitle()) == null ? null : new SeoTitle(input.seoTitle()),
                    blankNull(input.seoDescription()) == null ? null : new SeoDescription(input.seoDescription())));
            auditTrailPort.append(new NewAuditEntry(
                    tourOperatorId, AuditActor.user(callerUserId),
                    "EXPERIENCE", experienceId, "experience.translation_updated",
                    Map.of("locale", locale.value())));
        });
    }

    private Handle resolveHandle(UUID operatorId, UUID experienceId, LocaleCode locale,
                             String explicitHandle, ExperienceName name) {
        if (explicitHandle != null && !explicitHandle.isBlank()) {
            Handle handle = new Handle(explicitHandle.trim());
            if (translationRepository.existsByOperatorLocaleHandle(operatorId, locale.value(), handle.value(), experienceId)) {
                throw new ResourceAlreadyExistsException(
                        "The localized handle '" + handle.value() + "' is already in use for this language");
            }
            // Excluding this experience: matching its own canonical handle resolves
            // to the same experience, so only another one's is a clash.
            if (experienceRepository.existsByTourOperatorIdAndHandleExcluding(operatorId, handle.value(), experienceId)) {
                throw new ResourceAlreadyExistsException(
                        "Another experience already uses '" + handle.value() + "' as its handle");
            }
            return handle;
        }
        if (name != null) {
            return handleGenerator.generateUnique(name.value(), candidate ->
                    translationRepository.existsByOperatorLocaleHandle(operatorId, locale.value(), candidate, experienceId)
                            || experienceRepository.existsByTourOperatorIdAndHandleExcluding(operatorId, candidate, experienceId));
        }
        return null;
    }

    private static String blankNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    private static <T> List<T> mapList(List<String> raw, Function<String, T> vo) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        return raw.stream().map(vo).toList();
    }
}
