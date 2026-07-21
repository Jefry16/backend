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
import com.vointika.shared.exception.ResourceAlreadyExistsException;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.port.OperatorLocalesQuery;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.service.SlugGenerator;
import com.vointika.shared.valueobject.LocaleCode;
import com.vointika.shared.valueobject.Slug;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/**
 * Creates or replaces the translation overlay for one (experience, locale).
 * ADMIN+; membership enforced by the interceptor.
 *
 * <p>Guards: caller not ADMIN+ → 403; experience not under this operator → 404;
 * bad-shape or unsupported locale → 422 (the locale must be in the operator's
 * supported set — {@link OperatorLocalesQuery}); an explicit localized slug
 * already used by another experience for this operator+locale → 409. A blank
 * field is treated as untranslated (null → falls back to canonical).
 *
 * <p>Slug precedence: an explicit slug is validated + uniqueness-probed (no
 * auto-suffix — the admin chose it); else a translated {@code name} derives a
 * unique localized slug (auto-suffix); else null. No audit (no audit context yet).
 */
public class UpsertExperienceTranslationUseCase {

    private final ExperienceRepository experienceRepository;
    private final ExperienceTranslationRepository translationRepository;
    private final OperatorLocalesQuery operatorLocalesQuery;
    private final SlugGenerator slugGenerator;
    private final TourOperatorMembershipCheck membershipCheck;

    public UpsertExperienceTranslationUseCase(ExperienceRepository experienceRepository,
                                              ExperienceTranslationRepository translationRepository,
                                              OperatorLocalesQuery operatorLocalesQuery,
                                              SlugGenerator slugGenerator,
                                              TourOperatorMembershipCheck membershipCheck) {
        this.experienceRepository = experienceRepository;
        this.translationRepository = translationRepository;
        this.operatorLocalesQuery = operatorLocalesQuery;
        this.slugGenerator = slugGenerator;
        this.membershipCheck = membershipCheck;
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
        Slug slug = resolveSlug(tourOperatorId, experienceId, locale, input.slug(), name);

        translationRepository.upsert(new ExperienceTranslation(
                experienceId, tourOperatorId, locale,
                name, description, longDescription, highlights, included, notIncluded, slug));
    }

    private Slug resolveSlug(UUID operatorId, UUID experienceId, LocaleCode locale,
                             String explicitSlug, ExperienceName name) {
        if (explicitSlug != null && !explicitSlug.isBlank()) {
            Slug slug = new Slug(explicitSlug.trim());
            if (translationRepository.existsByOperatorLocaleSlug(operatorId, locale.value(), slug.value(), experienceId)) {
                throw new ResourceAlreadyExistsException(
                        "The localized slug '" + slug.value() + "' is already in use for this language");
            }
            return slug;
        }
        if (name != null) {
            return slugGenerator.generateUnique(name.value(), candidate ->
                    translationRepository.existsByOperatorLocaleSlug(operatorId, locale.value(), candidate, experienceId));
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
