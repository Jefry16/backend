package com.vointika.metafield.application.usecase;

import com.vointika.metafield.application.dto.input.UpsertMetafieldTranslationsInput;
import com.vointika.metafield.application.service.MetafieldOwnerAccess;
import com.vointika.metafield.application.service.MetafieldValueValidator;
import com.vointika.metafield.domain.entity.MetafieldDefinition;
import com.vointika.metafield.domain.entity.MetafieldValue;
import com.vointika.metafield.domain.entity.MetafieldValueTranslation;
import com.vointika.metafield.domain.projection.TranslatableMetafieldValue;
import com.vointika.metafield.domain.repository.MetafieldDefinitionRepository;
import com.vointika.metafield.domain.repository.MetafieldValueRepository;
import com.vointika.metafield.domain.repository.MetafieldValueTranslationRepository;
import com.vointika.metafield.domain.valueobject.MetafieldOwnerType;
import com.vointika.metafield.domain.valueobject.MetafieldDefinitionName;
import com.vointika.metafield.domain.valueobject.MetafieldKey;
import com.vointika.metafield.domain.valueobject.MetafieldNamespace;
import com.vointika.metafield.domain.valueobject.MetafieldType;
import com.vointika.metafield.infrastructure.port.JacksonJsonSyntaxPort;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.OperatorLocalesQuery;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * The rules that make a translated metafield safe: which types may carry one,
 * which locales, and what a half-valid payload does.
 */
class UpsertMetafieldTranslationsUseCaseTest {

    private static final UUID OPERATOR = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3ea2");
    private static final UUID CALLER = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3ea3");
    private static final UUID DEF_HOURS = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3eb1");
    private static final UUID DEF_FLAG = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3eb2");
    private static final UUID VAL_HOURS = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3ec1");
    private static final UUID VAL_FLAG = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3ec2");

    private MetafieldDefinitionRepository definitionRepository;
    private MetafieldValueRepository valueRepository;
    private MetafieldValueTranslationRepository translationRepository;
    private OperatorLocalesQuery operatorLocalesQuery;
    private AuditTrailPort auditTrailPort;
    private TransactionRunner transactionRunner;
    private UpsertMetafieldTranslationsUseCase useCase;

    @BeforeEach
    void setUp() {
        definitionRepository = mock(MetafieldDefinitionRepository.class);
        valueRepository = mock(MetafieldValueRepository.class);
        translationRepository = mock(MetafieldValueTranslationRepository.class);
        operatorLocalesQuery = mock(OperatorLocalesQuery.class);
        auditTrailPort = mock(AuditTrailPort.class);

        MetafieldOwnerAccess ownerAccess = new MetafieldOwnerAccess(
                mock(com.vointika.shared.port.ExperienceOwnershipQuery.class),
                mock(com.vointika.shared.port.PageOwnershipQuery.class));

        when(operatorLocalesQuery.findSupportedLocales(OPERATOR)).thenReturn(Set.of("es", "en"));

        transactionRunner = mock(TransactionRunner.class);
        doAnswer(call -> {
            ((Runnable) call.getArgument(0)).run();
            return null;
        }).when(transactionRunner).run(any());

        useCase = new UpsertMetafieldTranslationsUseCase(
                valueRepository, translationRepository, ownerAccess,
                new MetafieldValueValidator(new JacksonJsonSyntaxPort(new ObjectMapper())),
                operatorLocalesQuery, mock(TourOperatorMembershipCheck.class),
                transactionRunner, auditTrailPort);
    }

    @Test
    void itWritesOneRowPerTranslatedKey() {
        ownerHas(value("custom", "opening-hours", MetafieldType.MULTI_LINE_TEXT, VAL_HOURS));

        useCase.execute(input(Map.of("custom.opening-hours", "Mon-Fri 9-6")));

        verify(translationRepository).upsert(argThat((MetafieldValueTranslation t) ->
                t.metafieldValueId().equals(VAL_HOURS)
                        && t.locale().equals("en")
                        && t.value().equals("Mon-Fri 9-6")));
    }

    /**
     * <b>The type allowlist.</b> A translated {@code true} is {@code true} — the
     * value is data, not content, so there is nothing to translate and the save
     * says which key it refused.
     */
    @Test
    void aNonTextTypeIsRefusedByName() {
        ownerHas(value("custom", "accepts-groups", MetafieldType.BOOLEAN, VAL_FLAG));

        assertThatThrownBy(() -> useCase.execute(input(Map.of("custom.accepts-groups", "sí"))))
                .isInstanceOf(InvalidFieldException.class)
                .hasMessageContaining("boolean")
                .hasMessageContaining("custom.accepts-groups");

        verifyNoInteractions(auditTrailPort);
        verify(translationRepository, never()).upsert(any());
    }

    /**
     * <b>Nothing is written when anything is invalid.</b> Half a translated
     * locale is worse than a rejected save: the operator is told nothing about
     * which half landed. Validation runs over the whole payload before the
     * transaction opens, which is also why the message can name the bad key
     * rather than the first one that happened to fail.
     */
    @Test
    void oneBadKeyRejectsTheWholePayload() {
        ownerHas(value("custom", "opening-hours", MetafieldType.MULTI_LINE_TEXT, VAL_HOURS),
                value("custom", "accepts-groups", MetafieldType.BOOLEAN, VAL_FLAG));

        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("custom.opening-hours", "Mon-Fri 9-6");
        payload.put("custom.accepts-groups", "sí");

        assertThatThrownBy(() -> useCase.execute(input(payload)))
                .isInstanceOf(InvalidFieldException.class);

        verify(translationRepository, never()).upsert(any());
    }

    /** A locale the operator does not publish cannot be translated into. */
    @Test
    void anUnsupportedLocaleIs422() {
        assertThatThrownBy(() -> useCase.execute(new UpsertMetafieldTranslationsInput(
                CALLER, OPERATOR, MetafieldOwnerType.TOUR_OPERATOR, OPERATOR, "de",
                Map.of("custom.opening-hours", "Mo-Fr 9-18"))))
                .isInstanceOf(InvalidFieldException.class)
                .hasMessageContaining("de");

        verifyNoInteractions(definitionRepository);
    }

    /**
     * A blank box is how the editor says "not translated after all", so it is a
     * delete — and an absent key is left alone, which is what lets a partial form
     * save without wiping fields it never rendered.
     */
    @Test
    void aBlankValueClearsThatKeyOnly() {
        ownerHas(value("custom", "opening-hours", MetafieldType.MULTI_LINE_TEXT, VAL_HOURS));
        when(translationRepository.delete(VAL_HOURS, "en")).thenReturn(true);

        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("custom.opening-hours", "   ");
        useCase.execute(input(payload));

        verify(translationRepository).delete(VAL_HOURS, "en");
        verify(translationRepository, never()).upsert(any());
    }

    /** An empty payload is a no-op, not an empty audit entry. */
    @Test
    void nothingSubmittedWritesNothing() {
        useCase.execute(input(Map.of()));

        verifyNoInteractions(translationRepository);
        verifyNoInteractions(auditTrailPort);
    }

    /**
     * The overlay joins FROM the canonical value, so a translation of a metafield
     * nobody has set would be a row the storefront can never reach.
     */
    @Test
    void translatingAKeyWithNoValueIs404() {
        ownerHas();

        assertThatThrownBy(() -> useCase.execute(input(Map.of("custom.opening-hours", "Mon-Fri"))))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("custom.opening-hours");
    }

    /** The key is 'namespace.key'; anything else is refused before a lookup. */
    @Test
    void aMalformedQualifiedKeyIsRefused() {
        assertThatThrownBy(() -> useCase.execute(input(Map.of("openinghours", "x"))))
                .isInstanceOf(InvalidFieldException.class)
                .hasMessageContaining("namespace.key");
    }

    /**
     * A translation gets the canonical value's own limits — the same validator,
     * so a 6,000-character "translation" of a multi_line_text is refused exactly
     * as the original would be.
     */
    @Test
    void aTranslationIsValidatedLikeTheValueItOverlays() {
        ownerHas(value("custom", "opening-hours", MetafieldType.MULTI_LINE_TEXT, VAL_HOURS));

        assertThatThrownBy(() -> useCase.execute(
                input(Map.of("custom.opening-hours", "x".repeat(5_001)))))
                .isInstanceOf(InvalidFieldException.class);
    }

    private UpsertMetafieldTranslationsInput input(Map<String, String> values) {
        return new UpsertMetafieldTranslationsInput(
                CALLER, OPERATOR, MetafieldOwnerType.TOUR_OPERATOR, OPERATOR, "en", values);
    }

    /**
     * The whole owner's values arrive in one read, so a test declares the set
     * rather than stubbing a lookup per key — which is the point of the batching.
     */
    private void ownerHas(TranslatableMetafieldValue... values) {
        when(valueRepository.listTranslatableForOwner(
                OPERATOR, MetafieldOwnerType.TOUR_OPERATOR, OPERATOR))
                .thenReturn(List.of(values));
    }

    private static TranslatableMetafieldValue value(String namespace, String key,
                                                    MetafieldType type, UUID valueId) {
        return new TranslatableMetafieldValue(namespace, key, type, valueId);
    }

    private static MetafieldDefinition definition(UUID id, String namespace, String key,
                                                  MetafieldType type) {
        return new MetafieldDefinition(id, OPERATOR, MetafieldOwnerType.TOUR_OPERATOR,
                new MetafieldNamespace(namespace), new MetafieldKey(key), type, null,
                new MetafieldDefinitionName("Name"), null, CALLER);
    }

}
