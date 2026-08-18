package com.vointika.experience.application.service;

import com.vointika.experience.application.dto.input.AudiencePricingInput;
import com.vointika.experience.application.service.AudiencePricingResolver.PricedAudience;
import com.vointika.experience.domain.entity.SlotAudiencePricing;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.AudienceOwnershipQuery;
import com.vointika.shared.port.AudienceView;
import com.vointika.shared.service.IdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The resolver's own rules — which nothing exercised before this.
 *
 * <p>{@code SlotUseCasesTest} mocks {@link AudiencePricingResolver} wholesale, so
 * the duplicate-audience check, the input ordering and the 404 on a foreign
 * audience all ran in production and in no test. That gap is what let
 * {@code buildRows} re-validate every price: the double construction was invisible
 * because the only caller of the real code was the application itself.
 */
class AudiencePricingResolverTest {

    private static final UUID OPERATOR = UUID.fromString("019f8b02-77aa-7c31-8de4-0a2b19f6c001");
    private static final UUID ADULT = UUID.fromString("019f8b02-77aa-7c31-8de4-0a2b19f6c002");
    private static final UUID CHILD = UUID.fromString("019f8b02-77aa-7c31-8de4-0a2b19f6c003");
    private static final UUID SLOT = UUID.fromString("019f8b02-77aa-7c31-8de4-0a2b19f6c004");

    private AudienceOwnershipQuery audiences;
    private AudiencePricingResolver resolver;

    @BeforeEach
    void setUp() {
        audiences = mock(AudienceOwnershipQuery.class);
        IdGenerator idGenerator = mock(IdGenerator.class);
        when(idGenerator.newId()).thenReturn(UUID.randomUUID());
        resolver = new AudiencePricingResolver(audiences, idGenerator);
    }

    private void known(UUID id, String name, int paxPerUnit) {
        when(audiences.findForTourOperator(id, OPERATOR))
                .thenReturn(Optional.of(new AudienceView(id, name, paxPerUnit)));
    }

    private static AudiencePricingInput price(UUID audienceId, String amount, int capacity) {
        return new AudiencePricingInput(audienceId, new BigDecimal(amount), capacity);
    }

    @Test
    void resolvesInInputOrder() {
        known(ADULT, "Adult", 1);
        known(CHILD, "Child", 1);

        List<PricedAudience> resolved = resolver.validateAndResolve(
                List.of(price(CHILD, "10.00", 5), price(ADULT, "20.00", 8)), OPERATOR);

        assertThat(resolved).extracting(p -> p.audience().id()).containsExactly(CHILD, ADULT);
    }

    @Test
    void freezesTheValidatedPriceAndCapacityOntoEveryRow() {
        known(ADULT, "Adult", 2);

        List<PricedAudience> resolved =
                resolver.validateAndResolve(List.of(price(ADULT, "20.5", 8)), OPERATOR);
        List<SlotAudiencePricing> rows = resolver.buildRows(SLOT, resolved);

        assertThat(rows).singleElement().satisfies(row -> {
            assertThat(row.slotId()).isEqualTo(SLOT);
            assertThat(row.audienceId()).isEqualTo(ADULT);
            assertThat(row.audienceName()).isEqualTo("Adult");
            assertThat(row.paxPerUnit()).isEqualTo(2);
            assertThat(row.capacity()).isEqualTo(8);
            // Price normalises to 2dp, so the row carries the value object's
            // scale rather than whatever the payload happened to send.
            assertThat(row.price()).isEqualByComparingTo("20.50");
            assertThat(row.price().scale()).isEqualTo(2);
        });
    }

    /**
     * The recurring creator calls {@code buildRows} once per generated slot off one
     * {@code validateAndResolve}, so the resolved list has to be safely reusable.
     */
    @Test
    void buildRowsCanRunRepeatedlyOffOneResolution() {
        known(ADULT, "Adult", 1);
        List<PricedAudience> resolved =
                resolver.validateAndResolve(List.of(price(ADULT, "20.00", 8)), OPERATOR);

        List<SlotAudiencePricing> first = resolver.buildRows(SLOT, resolved);
        List<SlotAudiencePricing> second = resolver.buildRows(SLOT, resolved);

        assertThat(second).hasSameSizeAs(first);
        assertThat(second.get(0).audienceId()).isEqualTo(first.get(0).audienceId());
        assertThat(second.get(0).price()).isEqualByComparingTo(first.get(0).price());
        assertThat(second.get(0).capacity()).isEqualTo(first.get(0).capacity());
    }

    @Test
    void refusesAnEmptyPricingList() {
        assertThatThrownBy(() -> resolver.validateAndResolve(List.of(), OPERATOR))
                .isInstanceOf(InvalidFieldException.class)
                .hasMessage("At least one audience price is required");
    }

    @Test
    void refusesTheSameAudienceTwice() {
        known(ADULT, "Adult", 1);

        assertThatThrownBy(() -> resolver.validateAndResolve(
                List.of(price(ADULT, "20.00", 8), price(ADULT, "25.00", 4)), OPERATOR))
                .isInstanceOf(InvalidFieldException.class)
                .hasMessage("Duplicate or missing audience in audiencePrices");
    }

    @Test
    void refusesAnAudienceThisOperatorDoesNotOwn() {
        when(audiences.findForTourOperator(ADULT, OPERATOR)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                resolver.validateAndResolve(List.of(price(ADULT, "20.00", 8)), OPERATOR))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Audience not found");
    }

    /** A bad price fails the whole batch before any audience is looked up. */
    @Test
    void refusesABadPriceBeforeResolvingAnything() {
        assertThatThrownBy(() -> resolver.validateAndResolve(
                List.of(new AudiencePricingInput(ADULT, new BigDecimal("-1"), 8)), OPERATOR))
                .isInstanceOf(InvalidFieldException.class)
                .hasMessage("Price must be non-negative");
    }

    @Test
    void refusesABadCapacity() {
        assertThatThrownBy(() ->
                resolver.validateAndResolve(List.of(price(ADULT, "20.00", 0)), OPERATOR))
                .isInstanceOf(InvalidFieldException.class)
                .hasMessage("Capacity must be positive");
    }
}
