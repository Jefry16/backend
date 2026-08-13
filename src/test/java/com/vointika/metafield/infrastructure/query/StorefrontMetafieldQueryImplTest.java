package com.vointika.metafield.infrastructure.query;

import com.vointika.metafield.domain.projection.MetafieldValueWithDefinition;
import com.vointika.metafield.domain.projection.PublishedMetaobjectField;
import com.vointika.metafield.domain.repository.MetafieldValueRepository;
import com.vointika.metafield.infrastructure.persistence.repository.MetaobjectEntryJpaRepository;
import com.vointika.metafield.domain.valueobject.MetafieldOwnerType;
import com.vointika.metafield.domain.valueobject.MetafieldType;
import com.vointika.shared.port.StorefrontMetafieldQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * The storefront's read of the operator's own metafields.
 *
 * <p>Nothing above this can see the two things that matter: that the owner type
 * is {@code TOUR_OPERATOR} with the operator as its own owner, and that the type
 * crosses the boundary as <b>our code</b> rather than the enum name. Get the
 * first wrong and the storefront renders an experience's values; get the second wrong
 * and a theme branches on {@code SINGLE_LINE_TEXT} instead of
 * {@code single_line_text}.
 */
class StorefrontMetafieldQueryImplTest {

    private static final UUID OPERATOR = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3ea2");
    private static final UUID ENTRY = UUID.fromString("01900000-0000-7000-8000-0000000000d5");

    private MetafieldValueRepository valueRepository;
    private MetaobjectEntryJpaRepository entryJpa;
    private StorefrontMetafieldQueryImpl query;

    @BeforeEach
    void setUp() {
        valueRepository = mock(MetafieldValueRepository.class);
        entryJpa = mock(MetaobjectEntryJpaRepository.class);
        query = new StorefrontMetafieldQueryImpl(valueRepository, entryJpa);
    }

    @Test
    void itReadsTheOperatorsOwnValuesWithTheOperatorAsOwner() {
        when(valueRepository.listForOwner(OPERATOR, MetafieldOwnerType.TOUR_OPERATOR, OPERATOR))
                .thenReturn(List.of(value("custom", "opening-hours", "Mon-Sat 09:00-18:00")));

        assertThat(query.findForOperator(OPERATOR)).singleElement().satisfies(v -> {
            assertThat(v.namespace()).isEqualTo("custom");
            assertThat(v.key()).isEqualTo("opening-hours");
            assertThat(v.value()).isEqualTo("Mon-Sat 09:00-18:00");
        });
        verify(valueRepository).listForOwner(OPERATOR, MetafieldOwnerType.TOUR_OPERATOR, OPERATOR);
    }

    /** Our vocabulary, not Shopify's and not the enum's name. */
    @Test
    void theTypeCrossesAsItsCode() {
        when(valueRepository.listForOwner(OPERATOR, MetafieldOwnerType.TOUR_OPERATOR, OPERATOR))
                .thenReturn(List.of(value("custom", "opening-hours", "x")));

        assertThat(query.findForOperator(OPERATOR).getFirst().type()).isEqualTo("single_line_text");
    }

    /** An operator who has filled in nothing has nothing to render. */
    @Test
    void noValuesIsAnEmptyList() {
        when(valueRepository.listForOwner(OPERATOR, MetafieldOwnerType.TOUR_OPERATOR, OPERATOR))
                .thenReturn(List.of());

        assertThat(query.findForOperator(OPERATOR)).isEmpty();
    }

    /**
     * The whole point of the resolve: a theme handed {@code ENTRY}'s id can do
     * nothing with it, so the entry rides along — and {@code value} keeps the id,
     * because presentation is where one is chosen over the other.
     */
    @Test
    void aReferenceCarriesTheEntryItPointsAt() {
        when(valueRepository.listForOwner(OPERATOR, MetafieldOwnerType.TOUR_OPERATOR, OPERATOR))
                .thenReturn(List.of(reference("custom", "flagship-boat", ENTRY)));
        when(entryJpa.findPublishedFields(OPERATOR, Set.of(ENTRY))).thenReturn(List.of(
                field("name", MetafieldType.SINGLE_LINE_TEXT, "Sea Swallow"),
                field("capacity", MetafieldType.NUMBER_INTEGER, "12")));

        assertThat(query.findForOperator(OPERATOR)).singleElement().satisfies(v -> {
            assertThat(v.value()).isEqualTo(ENTRY.toString());
            assertThat(v.metaobject().id()).isEqualTo(ENTRY);
            assertThat(v.metaobject().type()).isEqualTo("boat");
            assertThat(v.metaobject().handle()).isEqualTo("sea-swallow");
            assertThat(v.metaobject().name()).isEqualTo("Sea Swallow");
            assertThat(v.metaobject().fields()).containsExactly(
                    new StorefrontMetafieldQuery.MetaobjectFieldView("name", "single_line_text", "Sea Swallow"),
                    new StorefrontMetafieldQuery.MetaobjectFieldView("capacity", "number_integer", "12"));
        });
    }

    /**
     * <b>The visibility rule.</b> The query returns nothing for an unpublished,
     * deleted or foreign entry, and the metafield goes with it rather than
     * serving an id a theme cannot use — the same pruning a dead menu link gets.
     * A draft boat is exactly this case, and the dev seed keeps one.
     */
    @Test
    void aReferenceToNothingShowableIsDroppedEntirely() {
        when(valueRepository.listForOwner(OPERATOR, MetafieldOwnerType.TOUR_OPERATOR, OPERATOR))
                .thenReturn(List.of(reference("custom", "flagship-boat", ENTRY),
                        value("custom", "opening-hours", "Mon-Sat 09:00-18:00")));
        when(entryJpa.findPublishedFields(OPERATOR, Set.of(ENTRY))).thenReturn(List.of());

        assertThat(query.findForOperator(OPERATOR))
                .extracting(StorefrontMetafieldQuery.MetafieldView::key)
                .containsExactly("opening-hours");
    }

    /** No reference, no second query — the resolve is not a cost every page pays. */
    @Test
    void scalarsAloneDoNotTouchTheEntryTable() {
        when(valueRepository.listForOwner(OPERATOR, MetafieldOwnerType.TOUR_OPERATOR, OPERATOR))
                .thenReturn(List.of(value("custom", "opening-hours", "Mon-Sat 09:00-18:00")));

        assertThat(query.findForOperator(OPERATOR)).singleElement()
                .satisfies(v -> assertThat(v.metaobject()).isNull());
        verifyNoInteractions(entryJpa);
    }

    /**
     * The column is TEXT and the write path validates a UUID, so this is a row
     * that should not exist. It still must not take the page down with it.
     */
    @Test
    void aReferenceThatIsNotAnIdIsDroppedRatherThanThrowing() {
        when(valueRepository.listForOwner(OPERATOR, MetafieldOwnerType.TOUR_OPERATOR, OPERATOR))
                .thenReturn(List.of(reference("custom", "flagship-boat", "not-a-uuid")));

        assertThat(query.findForOperator(OPERATOR)).isEmpty();
        verifyNoInteractions(entryJpa);
    }

    private static MetafieldValueWithDefinition value(String namespace, String key, String value) {
        return new MetafieldValueWithDefinition(namespace, key, MetafieldType.SINGLE_LINE_TEXT,
                "Opening hours", value, Instant.parse("2026-08-12T10:00:00Z"));
    }

    private static MetafieldValueWithDefinition reference(String namespace, String key, Object entryId) {
        return new MetafieldValueWithDefinition(namespace, key, MetafieldType.METAOBJECT_REFERENCE,
                "Flagship boat", entryId.toString(), Instant.parse("2026-08-12T10:00:00Z"));
    }

    private static PublishedMetaobjectField field(String key, MetafieldType type, String value) {
        return new PublishedMetaobjectField(ENTRY, "boat", "sea-swallow", "Sea Swallow", key, type, value);
    }
}
