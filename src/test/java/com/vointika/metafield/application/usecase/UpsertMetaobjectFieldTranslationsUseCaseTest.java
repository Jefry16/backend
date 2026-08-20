package com.vointika.metafield.application.usecase;

import com.vointika.shared.service.OperatorLocaleCheck;
import com.vointika.metafield.application.dto.input.UpsertMetaobjectFieldTranslationsInput;
import com.vointika.metafield.application.service.MetafieldValueValidator;
import com.vointika.metafield.domain.entity.MetaobjectEntry;
import com.vointika.metafield.domain.entity.MetaobjectEntryValue;
import com.vointika.metafield.domain.entity.MetaobjectEntryValueTranslation;
import com.vointika.metafield.domain.entity.MetaobjectField;
import com.vointika.metafield.domain.repository.MetaobjectDefinitionRepository;
import com.vointika.metafield.domain.repository.MetaobjectEntryRepository;
import com.vointika.metafield.domain.repository.MetaobjectEntryValueTranslationRepository;
import com.vointika.metafield.domain.valueobject.MetafieldKey;
import com.vointika.metafield.domain.valueobject.MetafieldType;
import com.vointika.metafield.domain.valueobject.MetafieldDefinitionName;
import com.vointika.metafield.domain.valueobject.MetaobjectEntryName;
import com.vointika.metafield.infrastructure.port.JacksonJsonSyntaxPort;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.valueobject.Handle;
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
import java.util.Set;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Slice 2 of the translation work, and every rule is slice 1's — text types
 * only, all-or-nothing, blank clears. What is worth its own test is the part
 * that differs: keys are bare field keys, and the entry must be this operator's.
 */
class UpsertMetaobjectFieldTranslationsUseCaseTest {

    private static final UUID OPERATOR = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3ea2");
    private static final UUID CALLER = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3ea3");
    private static final UUID DEFINITION = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3eb0");
    private static final UUID ENTRY = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3eb1");
    private static final UUID FIELD_NOTES = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3ec1");
    private static final UUID FIELD_CAPACITY = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3ec2");
    private static final UUID VALUE_NOTES = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3ed1");
    private static final UUID VALUE_CAPACITY = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3ed2");

    private MetaobjectEntryRepository entryRepository;
    private MetaobjectDefinitionRepository definitionRepository;
    private MetaobjectEntryValueTranslationRepository translationRepository;
    private AuditTrailPort auditTrailPort;
    private UpsertMetaobjectFieldTranslationsUseCase useCase;

    @BeforeEach
    void setUp() {
        entryRepository = mock(MetaobjectEntryRepository.class);
        definitionRepository = mock(MetaobjectDefinitionRepository.class);
        translationRepository = mock(MetaobjectEntryValueTranslationRepository.class);
        auditTrailPort = mock(AuditTrailPort.class);

        OperatorLocalesQuery operatorLocalesQuery = mock(OperatorLocalesQuery.class);
        when(operatorLocalesQuery.findSupportedLocales(OPERATOR)).thenReturn(Set.of("es", "en"));

        TransactionRunner transactionRunner = mock(TransactionRunner.class);
        doAnswer(call -> {
            ((Runnable) call.getArgument(0)).run();
            return null;
        }).when(transactionRunner).run(any());

        when(entryRepository.findByIdAndTourOperatorId(ENTRY, OPERATOR)).thenReturn(Optional.of(new MetaobjectEntry(
                        ENTRY, OPERATOR, DEFINITION, new Handle("sea-swallow"),
                        new MetaobjectEntryName("Sea Swallow"), CALLER)));
        when(definitionRepository.fieldsOf(DEFINITION)).thenReturn(List.of(
                field(FIELD_NOTES, "notes", MetafieldType.MULTI_LINE_TEXT, 0),
                field(FIELD_CAPACITY, "capacity", MetafieldType.NUMBER_INTEGER, 1)));
        when(entryRepository.valuesOf(ENTRY)).thenReturn(List.of(
                new MetaobjectEntryValue(VALUE_NOTES, ENTRY, FIELD_NOTES, "Balandro, 11 m.", CALLER),
                new MetaobjectEntryValue(VALUE_CAPACITY, ENTRY, FIELD_CAPACITY, "12", CALLER)));

        useCase = new UpsertMetaobjectFieldTranslationsUseCase(
                entryRepository, definitionRepository, translationRepository,
                new MetafieldValueValidator(new JacksonJsonSyntaxPort(new ObjectMapper())),
                new OperatorLocaleCheck(operatorLocalesQuery), mock(TourOperatorMembershipCheck.class),
                transactionRunner, auditTrailPort);
        // Run the real default: Mockito stubs a `default` like any other method,
        // so stubbing require* directly would make the assertions below hold for
        // any value (PATTERNS §9).
        doCallRealMethod().when(entryRepository).requireByIdAndTourOperatorId(any(), any());
    }

    /** The bare field key, because a metaobject field has no namespace. */
    @Test
    void itWritesOneRowPerTranslatedField() {
        useCase.execute(input(Map.of("notes", "Sloop, 11 m.")));

        verify(translationRepository).upsert(argThat((MetaobjectEntryValueTranslation t) ->
                t.entryValueId().equals(VALUE_NOTES)
                        && t.locale().equals("en")
                        && t.value().equals("Sloop, 11 m.")));
    }

    /** Same allowlist as slice 1: a capacity of 12 is 12 in every language. */
    @Test
    void aNonTextFieldIsRefusedByName() {
        assertThatThrownBy(() -> useCase.execute(input(Map.of("capacity", "twelve"))))
                .isInstanceOf(InvalidFieldException.class)
                .hasMessageContaining("number_integer")
                .hasMessageContaining("capacity");

        verify(translationRepository, never()).upsert(any());
        verifyNoInteractions(auditTrailPort);
    }

    @Test
    void oneBadFieldRejectsTheWholePayload() {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("notes", "Sloop, 11 m.");
        payload.put("capacity", "twelve");

        assertThatThrownBy(() -> useCase.execute(input(payload)))
                .isInstanceOf(InvalidFieldException.class);

        verify(translationRepository, never()).upsert(any());
    }

    /** A field the definition does not have cannot be translated into existence. */
    @Test
    void anUnknownFieldKeyIs404() {
        assertThatThrownBy(() -> useCase.execute(input(Map.of("skipper", "Ana"))))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("skipper");
    }

    /**
     * <b>The tenant guard.</b> Another operator's entry id is a 404, not a
     * translated row — the entry is looked up scoped, never by id alone.
     *
     * <p>The refusal itself belongs to
     * {@code MetaobjectEntryRepository.requireByIdAndTourOperatorId} and is pinned
     * with its message in {@code TenantScopedLookupTest}. What this asserts is that
     * the use case does not swallow it, so the message is deliberately not repeated
     * here.
     */
    @Test
    void anotherOperatorsEntryIs404() {
        UUID foreign = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3eff");
        when(entryRepository.findByIdAndTourOperatorId(foreign, OPERATOR)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new UpsertMetaobjectFieldTranslationsInput(
                CALLER, OPERATOR, foreign, "en", Map.of("notes", "x"))))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void aBlankValueClearsThatField() {
        when(translationRepository.delete(VALUE_NOTES, "en")).thenReturn(true);

        useCase.execute(input(Map.of("notes", "  ")));

        verify(translationRepository).delete(VALUE_NOTES, "en");
        verify(translationRepository, never()).upsert(any());
    }

    private UpsertMetaobjectFieldTranslationsInput input(Map<String, String> values) {
        return new UpsertMetaobjectFieldTranslationsInput(CALLER, OPERATOR, ENTRY, "en", values);
    }

    private static MetaobjectField field(UUID id, String key, MetafieldType type, int position) {
        return new MetaobjectField(id, DEFINITION, new MetafieldKey(key), type,
                new MetafieldDefinitionName(key), position);
    }
}
