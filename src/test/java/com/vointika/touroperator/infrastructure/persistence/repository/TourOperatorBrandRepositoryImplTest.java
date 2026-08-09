package com.vointika.touroperator.infrastructure.persistence.repository;

import com.vointika.touroperator.domain.entity.Brand;
import com.vointika.touroperator.domain.entity.BrandColor;
import com.vointika.touroperator.domain.entity.BrandSocialLink;
import com.vointika.touroperator.domain.enums.BrandColorRole;
import com.vointika.touroperator.domain.enums.BrandSocialPlatform;
import com.vointika.touroperator.domain.valueobject.HexColor;
import com.vointika.touroperator.domain.valueobject.SocialUrl;
import com.vointika.touroperator.infrastructure.persistence.entity.TourOperatorBrandColorJpaEntity;
import com.vointika.touroperator.infrastructure.persistence.entity.TourOperatorBrandSocialLinkJpaEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * The brand's two collections are replaced <b>wholesale</b>: cleared, then
 * reinserted from the payload.
 *
 * <p>Pinned here because the use-case tests cannot see it — they stub
 * {@code TourOperatorBrandRepository.save} and so pass whether the adapter
 * underneath diffs, appends or replaces. Nothing tested this adapter at all
 * until a review pass went looking.
 *
 * <p>The ordering is the part that matters. Insert before delete and the write
 * collides on {@code (operator, role, position)}, because those keys are reused
 * by the very payload replacing them — the realistic edit is recolouring the
 * palette in place, not appending to it.
 */
class TourOperatorBrandRepositoryImplTest {

    private static final UUID OP = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3ea2");

    private TourOperatorBrandJpaRepository brandJpa;
    private TourOperatorBrandColorJpaRepository colorJpa;
    private TourOperatorBrandSocialLinkJpaRepository socialJpa;
    private TourOperatorBrandRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        brandJpa = mock(TourOperatorBrandJpaRepository.class);
        colorJpa = mock(TourOperatorBrandColorJpaRepository.class);
        socialJpa = mock(TourOperatorBrandSocialLinkJpaRepository.class);
        repository = new TourOperatorBrandRepositoryImpl(brandJpa, colorJpa, socialJpa);
    }

    private static Brand brand(List<BrandColor> colors, List<BrandSocialLink> links) {
        return new Brand(OP, null, null, null, null, null, null,
                colors, links, Instant.EPOCH, Instant.EPOCH);
    }

    private static BrandColor color(BrandColorRole role, int position, String background) {
        return new BrandColor(role, position, new HexColor(background), new HexColor("#ffffff"));
    }

    @Test
    void bothCollectionsAreClearedBeforeAnythingIsInserted() {
        repository.save(brand(
                List.of(color(BrandColorRole.PRIMARY, 0, "#0b3d5c")),
                List.of(new BrandSocialLink(BrandSocialPlatform.INSTAGRAM,
                        new SocialUrl("https://instagram.com/acme")))));

        InOrder order = inOrder(colorJpa, socialJpa);
        order.verify(colorJpa).deleteByTourOperatorId(OP);
        order.verify(socialJpa).deleteByTourOperatorId(OP);
        order.verify(colorJpa).saveAll(any());
        order.verify(socialJpa).saveAll(any());
    }

    @Test
    void thePayloadOrderBecomesTheStoredPosition() {
        // colors.primary[0].background is an address a theme indexes into, so the
        // position the caller sent has to be the position stored.
        repository.save(brand(List.of(
                color(BrandColorRole.PRIMARY, 0, "#111111"),
                color(BrandColorRole.PRIMARY, 1, "#222222"),
                color(BrandColorRole.SECONDARY, 0, "#333333")), List.of()));

        ArgumentCaptor<List<TourOperatorBrandColorJpaEntity>> saved =
                ArgumentCaptor.forClass(List.class);
        verify(colorJpa).saveAll(saved.capture());
        assertThat(saved.getValue())
                .extracting(c -> c.getRole() + ":" + c.getPosition() + ":" + c.getBackground())
                .containsExactly("PRIMARY:0:#111111", "PRIMARY:1:#222222", "SECONDARY:0:#333333");
    }

    @Test
    void anEmptyBrandStillClearsWhatWasThere() {
        // The clearing path: a PUT that omits both collections must empty them,
        // not leave the previous ones standing.
        repository.save(brand(List.of(), List.of()));

        verify(colorJpa).deleteByTourOperatorId(OP);
        verify(socialJpa).deleteByTourOperatorId(OP);

        ArgumentCaptor<List<TourOperatorBrandColorJpaEntity>> colors =
                ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<TourOperatorBrandSocialLinkJpaEntity>> links =
                ArgumentCaptor.forClass(List.class);
        verify(colorJpa).saveAll(colors.capture());
        verify(socialJpa).saveAll(links.capture());
        assertThat(colors.getValue()).isEmpty();
        assertThat(links.getValue()).isEmpty();
    }
}
