package com.vointika.page.application.usecase;

import com.vointika.page.application.dto.input.UpsertPageTranslationInput;
import com.vointika.page.domain.entity.PageTranslation;
import com.vointika.page.domain.repository.PageRepository;
import com.vointika.page.domain.repository.PageTranslationRepository;
import com.vointika.page.domain.valueobject.PageBody;
import com.vointika.page.domain.valueobject.PageSeoDescription;
import com.vointika.page.domain.valueobject.PageSeoTitle;
import com.vointika.page.domain.valueobject.PageTitle;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.exception.ResourceAlreadyExistsException;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.OperatorLocalesQuery;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.service.HandleGenerator;
import com.vointika.shared.valueobject.AuditActor;
import com.vointika.shared.valueobject.LocaleCode;
import com.vointika.shared.valueobject.Handle;
import com.vointika.shared.exception.UniqueConstraintViolationException;

import java.util.Map;

/**
 * Creates or replaces a page's translation for one locale — the experience
 * model. ADMIN+. The page must belong to the operator (404), the locale must
 * be one the operator supports (422), and the optional localized handle
 * follows the experience rules: an explicit value is validated and
 * uniqueness-checked per (operator, locale) excluding this page (409, no
 * auto-suffix); absent but with a translated title, one is derived with
 * numeric-suffix probing; else null (the canonical handle serves the locale).
 *
 * <p><b>Blanking every field deletes the row rather than storing one.</b> An
 * overlay with nothing in it falls back for every field, so it is
 * indistinguishable from no overlay — except in the translations list, where it
 * appears as a locale someone has worked on. Saving an already-blank form is a
 * no-op: nothing written, nothing audited.
 */
public class UpsertPageTranslationUseCase {

    private final PageRepository pageRepository;
    private final PageTranslationRepository translationRepository;
    private final OperatorLocalesQuery operatorLocalesQuery;
    private final HandleGenerator handleGenerator;
    private final TourOperatorMembershipCheck membershipCheck;
    private final TransactionRunner transactionRunner;
    private final AuditTrailPort auditTrailPort;

    public UpsertPageTranslationUseCase(PageRepository pageRepository,
                                        PageTranslationRepository translationRepository,
                                        OperatorLocalesQuery operatorLocalesQuery,
                                        HandleGenerator handleGenerator,
                                        TourOperatorMembershipCheck membershipCheck,
                                        TransactionRunner transactionRunner,
                                        AuditTrailPort auditTrailPort) {
        this.pageRepository = pageRepository;
        this.translationRepository = translationRepository;
        this.operatorLocalesQuery = operatorLocalesQuery;
        this.handleGenerator = handleGenerator;
        this.membershipCheck = membershipCheck;
        this.transactionRunner = transactionRunner;
        this.auditTrailPort = auditTrailPort;
    }

    public void execute(UpsertPageTranslationInput input) {
        membershipCheck.ensureAdmin(input.callerUserId(), input.tourOperatorId());
        if (pageRepository.findByIdAndTourOperatorId(input.pageId(), input.tourOperatorId()).isEmpty()) {
            throw new ResourceNotFoundException("Page not found");
        }

        LocaleCode locale = new LocaleCode(input.locale());
        if (!operatorLocalesQuery.findSupportedLocales(input.tourOperatorId()).contains(locale.value())) {
            throw new InvalidFieldException(
                    "Locale '" + locale.value() + "' is not supported by this operator");
        }

        Handle handle = resolveHandle(input, locale);
        PageTranslation translation = new PageTranslation(
                input.pageId(),
                input.tourOperatorId(),
                locale,
                blank(input.title()) ? null : new PageTitle(input.title()),
                blank(input.body()) ? null : new PageBody(input.body()),
                blank(input.seoTitle()) ? null : new PageSeoTitle(input.seoTitle()),
                blank(input.seoDescription()) ? null : new PageSeoDescription(input.seoDescription()),
                handle);

        if (translation.isEmpty()) {
            clear(input, locale);
            return;
        }

        try {
            transactionRunner.run(() -> {
                translationRepository.upsert(translation);
                // Event on the parent page's timeline (a whole-locale replace
                // has no meaningful field diff — the locale is the story).
                auditTrailPort.append(new NewAuditEntry(
                        input.tourOperatorId(), AuditActor.user(input.callerUserId()),
                        "PAGE", input.pageId(), "page.translation_updated",
                        Map.of("locale", locale.value())));
            });
        } catch (UniqueConstraintViolationException e) {
            // Concurrent localized-handle race — the partial unique index fired.
            throw new ResourceAlreadyExistsException(
                    "The localized handle is already in use for this language");
        }
    }

    /**
     * Blanking every field is how the editor says "this locale is not translated",
     * and the honest representation of that is no row. Audited as a delete
     * because that is what happened; a no-op when there was nothing to remove, so
     * saving an already-blank form writes nothing and logs nothing.
     */
    private void clear(UpsertPageTranslationInput input, LocaleCode locale) {
        if (translationRepository.find(input.pageId(), locale).isEmpty()) {
            return;
        }
        transactionRunner.run(() -> {
            translationRepository.delete(input.pageId(), locale);
            auditTrailPort.append(new NewAuditEntry(
                    input.tourOperatorId(), AuditActor.user(input.callerUserId()),
                    "PAGE", input.pageId(), "page.translation_deleted",
                    Map.of("locale", locale.value())));
        });
    }

    private Handle resolveHandle(UpsertPageTranslationInput input, LocaleCode locale) {
        if (!blank(input.handle())) {
            Handle explicit = new Handle(input.handle().trim());
            if (translationRepository.existsByHandle(
                    input.tourOperatorId(), locale, explicit.value(), input.pageId())) {
                throw new ResourceAlreadyExistsException(
                        "The localized handle '" + explicit.value()
                                + "' is already in use for this language");
            }
            requireNoCanonicalClash(input, explicit.value());
            return explicit;
        }
        if (!blank(input.title())) {
            // Probe both namespaces, so a derived handle never lands on another
            // page's canonical one either.
            return handleGenerator.generateUnique(input.title(), candidate ->
                    translationRepository.existsByHandle(
                            input.tourOperatorId(), locale, candidate, input.pageId())
                            || pageRepository.existsByTourOperatorIdAndHandleExcluding(
                                    input.tourOperatorId(), candidate, input.pageId()));
        }
        return null;
    }

    /**
     * The storefront resolves a handle against localized handles first and
     * canonical ones second, so a localized handle equal to ANOTHER page's
     * canonical handle silently shadows that page in this locale. Matching the
     * page's own canonical handle is harmless — it resolves to the same page.
     */
    private void requireNoCanonicalClash(UpsertPageTranslationInput input, String handle) {
        if (pageRepository.existsByTourOperatorIdAndHandleExcluding(
                input.tourOperatorId(), handle, input.pageId())) {
            throw new ResourceAlreadyExistsException(
                    "Another page already uses '" + handle + "' as its handle");
        }
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }
}
