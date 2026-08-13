package com.vointika.touroperator.application.usecase;

import com.vointika.shared.exception.ForbiddenException;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.MediaAssetBatchQuery;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.valueobject.Handle;
import com.vointika.shared.valueobject.LocaleCode;
import com.vointika.touroperator.application.dto.input.UpdateBrandInput;
import com.vointika.touroperator.domain.entity.Brand;
import com.vointika.touroperator.domain.entity.TourOperator;
import com.vointika.touroperator.domain.enums.BrandColorRole;
import com.vointika.touroperator.domain.enums.BrandSocialPlatform;
import com.vointika.touroperator.domain.repository.TourOperatorBrandRepository;
import com.vointika.touroperator.domain.repository.TourOperatorRepository;
import com.vointika.touroperator.domain.valueobject.TourOperatorAddress;
import com.vointika.touroperator.domain.valueobject.TourOperatorName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** The brand's two use cases — the whole object read, and replaced. */
class BrandUseCasesTest {

    private static final UUID OP = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3ea2");
    private static final UUID USER = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final UUID LOGO = UUID.fromString("019f8100-0000-7000-8000-000000000001");
    private static final UUID FOREIGN = UUID.fromString("019f8100-0000-7000-8000-0000000000ff");

    private TourOperatorRepository operatorRepository;
    private TourOperatorBrandRepository brandRepository;
    private MediaAssetBatchQuery mediaAssetBatchQuery;
    private TourOperatorMembershipCheck membershipCheck;
    private TransactionRunner transactionRunner;
    private AuditTrailPort auditTrailPort;

    @BeforeEach
    void setUp() {
        operatorRepository = mock(TourOperatorRepository.class);
        brandRepository = mock(TourOperatorBrandRepository.class);
        mediaAssetBatchQuery = mock(MediaAssetBatchQuery.class);
        membershipCheck = mock(TourOperatorMembershipCheck.class);
        transactionRunner = mock(TransactionRunner.class);
        auditTrailPort = mock(AuditTrailPort.class);
        doAnswer(i -> {
            ((Runnable) i.getArgument(0)).run();
            return null;
        }).when(transactionRunner).run(any());
        when(operatorRepository.findById(OP)).thenReturn(Optional.of(operator()));
        when(brandRepository.findByTourOperatorId(OP)).thenReturn(Optional.empty());
        when(mediaAssetBatchQuery.findAssetsByIds(OP, Set.of(LOGO)))
                .thenReturn(Map.of(LOGO, new MediaAssetBatchQuery.MediaAsset("media/logo.png", null, null, null)));
        when(mediaAssetBatchQuery.findAssetsByIds(OP, Set.of(FOREIGN))).thenReturn(Map.of());
    }

    private TourOperator operator() {
        return new TourOperator(OP, new TourOperatorName("Acme"), new Handle("acme"),
                UUID.randomUUID(), UUID.randomUUID(), new TourOperatorAddress("Somewhere 1", null, "Palma", null, null, UUID.randomUUID()),
                USER, Instant.now(), Instant.now(),
                LocaleCode.of("en"), Set.of(LocaleCode.of("en"), LocaleCode.of("es")));
    }

    private UpdateBrandUseCase update() {
        return new UpdateBrandUseCase(operatorRepository, brandRepository, mediaAssetBatchQuery,
                membershipCheck, transactionRunner, auditTrailPort);
    }

    private static UpdateBrandInput input(UpdateBrandInput.Colors colors,
                                          List<UpdateBrandInput.SocialLink> links) {
        return new UpdateBrandInput("Sail the coast", "Small-group sailing.",
                null, null, null, null, colors, links);
    }

    // ---- read ----

    @Test
    void anOperatorWithNoBrandRowReadsAnEmptyBrandNotA404() {
        // The row's absence means "nothing filled in yet", which is the state most
        // operators are in and exactly what the editor form should render.
        var view = new GetBrandUseCase(brandRepository, membershipCheck).execute(OP, USER);

        verify(membershipCheck).ensureMember(USER, OP);
        assertThat(view.slogan()).isNull();
        assertThat(view.colors().primary()).isEmpty();
        assertThat(view.socialLinks()).isEmpty();
    }

    // ---- write ----

    @Test
    void updateReplacesTheWholeBrandAndAudits() {
        update().execute(OP, input(
                new UpdateBrandInput.Colors(
                        List.of(new UpdateBrandInput.Color("#0b3d5c", "#ffffff")),
                        List.of(new UpdateBrandInput.Color("#1c7ba8", "#000000"))),
                List.of(new UpdateBrandInput.SocialLink("INSTAGRAM", "https://instagram.com/acme"))),
                USER);

        verify(membershipCheck).ensureAdmin(USER, OP);
        ArgumentCaptor<Brand> saved = ArgumentCaptor.forClass(Brand.class);
        verify(brandRepository).save(saved.capture());
        Brand brand = saved.getValue();
        assertThat(brand.slogan().value()).isEqualTo("Sail the coast");
        assertThat(brand.colorsOf(BrandColorRole.PRIMARY)).singleElement()
                .satisfies(c -> assertThat(c.background().value()).isEqualTo("#0b3d5c"));
        assertThat(brand.socialLinks()).singleElement()
                .satisfies(l -> assertThat(l.platform()).isEqualTo(BrandSocialPlatform.INSTAGRAM));

        ArgumentCaptor<NewAuditEntry> audit = ArgumentCaptor.forClass(NewAuditEntry.class);
        verify(auditTrailPort).append(audit.capture());
        assertThat(audit.getValue().action()).isEqualTo("tour_operator.brand_updated");
    }

    @Test
    void positionComesFromThePayloadOrderBecauseAThemeIndexesIntoIt() {
        // colors.primary[0].background is a real address in the published contract,
        // so the order the client sends is the order stored.
        update().execute(OP, input(
                new UpdateBrandInput.Colors(
                        List.of(new UpdateBrandInput.Color("#111111", "#ffffff"),
                                new UpdateBrandInput.Color("#222222", "#ffffff")),
                        null),
                null), USER);

        ArgumentCaptor<Brand> saved = ArgumentCaptor.forClass(Brand.class);
        verify(brandRepository).save(saved.capture());
        assertThat(saved.getValue().colorsOf(BrandColorRole.PRIMARY))
                .extracting(c -> c.position() + ":" + c.background().value())
                .containsExactly("0:#111111", "1:#222222");
    }

    @Test
    void absentCollectionsEmptyThemBecausePutIsAFullReplace() {
        update().execute(OP, input(null, null), USER);

        ArgumentCaptor<Brand> saved = ArgumentCaptor.forClass(Brand.class);
        verify(brandRepository).save(saved.capture());
        assertThat(saved.getValue().colors()).isEmpty();
        assertThat(saved.getValue().socialLinks()).isEmpty();
    }

    @Test
    void mediaFromAnotherOperatorsLibraryIs422() {
        // Otherwise the storefront renders a broken image, or worse, one the
        // operator never uploaded.
        assertThatThrownBy(() -> update().execute(OP,
                new UpdateBrandInput(null, null, FOREIGN, null, null, null, null, null), USER))
                .isInstanceOf(InvalidFieldException.class);
        verify(brandRepository, never()).save(any());
    }

    @Test
    void allFourImagesAreCheckedInOneBatchCall() {
        when(mediaAssetBatchQuery.findAssetsByIds(any(), any()))
                .thenReturn(Map.of(LOGO, new MediaAssetBatchQuery.MediaAsset("media/logo.png", null, null, null)));

        update().execute(OP,
                new UpdateBrandInput(null, null, LOGO, LOGO, LOGO, LOGO, null, null), USER);

        // Four references, one query — and the set de-duplicates, so re-using one
        // asset for all four is a single lookup.
        verify(mediaAssetBatchQuery).findAssetsByIds(OP, Set.of(LOGO));
    }

    @Test
    void aDuplicatePlatformIsNamedRatherThanLeftToTheUniqueConstraint() {
        assertThatThrownBy(() -> update().execute(OP, input(null, List.of(
                new UpdateBrandInput.SocialLink("INSTAGRAM", "https://instagram.com/a"),
                new UpdateBrandInput.SocialLink("INSTAGRAM", "https://instagram.com/b"))), USER))
                .isInstanceOf(InvalidFieldException.class)
                .hasMessageContaining("INSTAGRAM");
    }

    @Test
    void anUnknownPlatformIs422() {
        assertThatThrownBy(() -> update().execute(OP, input(null, List.of(
                new UpdateBrandInput.SocialLink("MYSPACE", "https://myspace.com/acme"))), USER))
                .isInstanceOf(InvalidFieldException.class);
    }

    @Test
    void nothingIsWrittenWhenAnyPartOfThePayloadIsInvalid() {
        // Validate-all-before-any-write: a good slogan beside a bad colour leaves
        // the brand untouched rather than half-applied.
        assertThatThrownBy(() -> update().execute(OP, input(
                new UpdateBrandInput.Colors(
                        List.of(new UpdateBrandInput.Color("#0b3d5c", "not-a-colour")), null),
                null), USER))
                .isInstanceOf(InvalidFieldException.class);
        verify(brandRepository, never()).save(any());
        verify(auditTrailPort, never()).append(any());
    }

    @Test
    void updateRequiresAdmin() {
        doThrow(new ForbiddenException("admin")).when(membershipCheck).ensureAdmin(USER, OP);
        assertThatThrownBy(() -> update().execute(OP, input(null, null), USER))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void updateOfAMissingOperatorIs404() {
        when(operatorRepository.findById(OP)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> update().execute(OP, input(null, null), USER))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
