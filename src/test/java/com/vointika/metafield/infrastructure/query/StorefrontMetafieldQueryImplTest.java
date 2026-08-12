package com.vointika.metafield.infrastructure.query;

import com.vointika.metafield.domain.projection.MetafieldValueWithDefinition;
import com.vointika.metafield.domain.repository.MetafieldValueRepository;
import com.vointika.metafield.domain.valueobject.MetafieldOwnerType;
import com.vointika.metafield.domain.valueobject.MetafieldType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The storefront's read of the operator's own metafields.
 *
 * <p>Nothing above this can see the two things that matter: that the owner type
 * is {@code TOUR_OPERATOR} with the operator as its own owner, and that the type
 * crosses the boundary as <b>our code</b> rather than the enum name. Get the
 * first wrong and the shop renders an experience's values; get the second wrong
 * and a theme branches on {@code SINGLE_LINE_TEXT} instead of
 * {@code single_line_text}.
 */
class StorefrontMetafieldQueryImplTest {

    private static final UUID OPERATOR = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3ea2");

    private MetafieldValueRepository valueRepository;
    private StorefrontMetafieldQueryImpl query;

    @BeforeEach
    void setUp() {
        valueRepository = mock(MetafieldValueRepository.class);
        query = new StorefrontMetafieldQueryImpl(valueRepository);
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

    private static MetafieldValueWithDefinition value(String namespace, String key, String value) {
        return new MetafieldValueWithDefinition(namespace, key, MetafieldType.SINGLE_LINE_TEXT,
                "Opening hours", value, Instant.parse("2026-08-12T10:00:00Z"));
    }
}
