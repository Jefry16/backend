package com.vointika.touroperator.application.usecase;

import com.vointika.touroperator.application.dto.input.UpdateBrandInput;
import com.vointika.touroperator.domain.entity.Brand;
import com.vointika.touroperator.domain.entity.BrandColor;
import com.vointika.touroperator.domain.entity.BrandSocialLink;
import com.vointika.touroperator.domain.enums.BrandColorRole;
import com.vointika.touroperator.domain.enums.BrandSocialPlatform;
import com.vointika.touroperator.domain.repository.TourOperatorBrandRepository;
import com.vointika.touroperator.domain.repository.TourOperatorRepository;
import com.vointika.touroperator.domain.valueobject.BrandShortDescription;
import com.vointika.touroperator.domain.valueobject.BrandSlogan;
import com.vointika.touroperator.domain.valueobject.HexColor;
import com.vointika.touroperator.domain.valueobject.SocialUrl;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.MediaAssetBatchQuery;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.valueobject.AuditActor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Replaces the operator's whole brand. ADMIN+.
 *
 * <p><b>A full replace, including both collections.</b> An absent field clears
 * its value and absent collections empty them — the same semantics every other
 * {@code PUT} here has, and the reason a brand editor sends the whole object.
 * The palette and the links are deleted and reinserted rather than diffed: a
 * palette is an ordered whole, and position-diffing a partial payload is how
 * orderings drift.
 *
 * <p><b>Everything is validated before anything is written</b>, the model
 * {@code ReplaceMenuItemsUseCase} set: a payload with a good slogan and a bad
 * colour leaves the brand untouched rather than half-applied.
 *
 * <p>Media ids are checked against the operator's <em>own</em> library in one
 * batch call — four ids, one query. An id belonging to another operator is a
 * 422, not a broken image on a public page.
 *
 * <p>The audit entry names <b>which parts changed</b> and carries no content:
 * the row is the current value, and a slogan is not something the trail needs a
 * copy of.
 */
public class UpdateBrandUseCase {

    private final TourOperatorRepository tourOperatorRepository;
    private final TourOperatorBrandRepository brandRepository;
    private final MediaAssetBatchQuery mediaAssetBatchQuery;
    private final TourOperatorMembershipCheck membershipCheck;
    private final TransactionRunner transactionRunner;
    private final AuditTrailPort auditTrailPort;

    public UpdateBrandUseCase(TourOperatorRepository tourOperatorRepository,
                              TourOperatorBrandRepository brandRepository,
                              MediaAssetBatchQuery mediaAssetBatchQuery,
                              TourOperatorMembershipCheck membershipCheck,
                              TransactionRunner transactionRunner,
                              AuditTrailPort auditTrailPort) {
        this.tourOperatorRepository = tourOperatorRepository;
        this.brandRepository = brandRepository;
        this.mediaAssetBatchQuery = mediaAssetBatchQuery;
        this.membershipCheck = membershipCheck;
        this.transactionRunner = transactionRunner;
        this.auditTrailPort = auditTrailPort;
    }

    public void execute(UUID tourOperatorId, UpdateBrandInput input, UUID callerUserId) {
        membershipCheck.ensureAdmin(callerUserId, tourOperatorId);
        tourOperatorRepository.requireById(tourOperatorId);

        BrandSlogan slogan = blankNull(input.slogan()) == null
                ? null : new BrandSlogan(input.slogan());
        BrandShortDescription shortDescription = blankNull(input.shortDescription()) == null
                ? null : new BrandShortDescription(input.shortDescription());

        requireOwnedMedia(tourOperatorId, input);
        List<BrandColor> colors = readColors(input);
        List<BrandSocialLink> socialLinks = readSocialLinks(input);

        Brand existing = brandRepository.findByTourOperatorId(tourOperatorId).orElse(null);
        Instant now = Instant.now();
        Brand brand = new Brand(tourOperatorId, slogan, shortDescription,
                input.logoMediaId(), input.squareLogoMediaId(),
                input.faviconMediaId(), input.coverImageMediaId(),
                colors, socialLinks,
                existing == null || existing.createdAt() == null ? now : existing.createdAt(), now);

        transactionRunner.run(() -> {
            brandRepository.save(brand);
            auditTrailPort.append(new NewAuditEntry(
                    tourOperatorId, AuditActor.user(callerUserId),
                    "TOUR_OPERATOR", tourOperatorId, "tour_operator.brand_updated",
                    Map.of("changed", String.join(",", changedParts(existing, brand)))));
        });
    }

    /**
     * One batch call for all four images. Absent ids are skipped rather than
     * looked up, so an operator with no media can still save a slogan.
     */
    private void requireOwnedMedia(UUID tourOperatorId, UpdateBrandInput input) {
        Set<UUID> ids = new LinkedHashSet<>();
        for (UUID id : new UUID[]{input.logoMediaId(), input.squareLogoMediaId(),
                input.faviconMediaId(), input.coverImageMediaId()}) {
            if (id != null) {
                ids.add(id);
            }
        }
        if (ids.isEmpty()) {
            return;
        }
        var found = mediaAssetBatchQuery.findAssetsByIds(tourOperatorId, ids);
        for (UUID id : ids) {
            if (!found.containsKey(id)) {
                throw new InvalidFieldException(MediaAssetBatchQuery.NOT_IN_LIBRARY);
            }
        }
    }

    private static List<BrandColor> readColors(UpdateBrandInput input) {
        List<BrandColor> colors = new ArrayList<>();
        if (input.colors() == null) {
            return colors;
        }
        appendRole(colors, BrandColorRole.PRIMARY, input.colors().primary());
        appendRole(colors, BrandColorRole.SECONDARY, input.colors().secondary());
        return colors;
    }

    /** Position is the payload's order — the client sends the palette as it reads. */
    private static void appendRole(List<BrandColor> into, BrandColorRole role,
                                   List<UpdateBrandInput.Color> from) {
        if (from == null) {
            return;
        }
        for (int i = 0; i < from.size(); i++) {
            UpdateBrandInput.Color c = from.get(i);
            if (c == null) {
                throw new InvalidFieldException("A colour entry is missing");
            }
            into.add(new BrandColor(role, i,
                    new HexColor(c.background()), new HexColor(c.foreground())));
        }
    }

    private static List<BrandSocialLink> readSocialLinks(UpdateBrandInput input) {
        List<BrandSocialLink> links = new ArrayList<>();
        if (input.socialLinks() == null) {
            return links;
        }
        Set<BrandSocialPlatform> seen = EnumSet.noneOf(BrandSocialPlatform.class);
        for (UpdateBrandInput.SocialLink link : input.socialLinks()) {
            if (link == null) {
                throw new InvalidFieldException("A social link entry is missing");
            }
            BrandSocialPlatform platform = platformOf(link.platform());
            // The table's key is (operator, platform); catching the repeat here
            // names it, instead of letting the insert fail with a 23505.
            if (!seen.add(platform)) {
                throw new InvalidFieldException(
                        "Duplicate social link for " + platform.name());
            }
            links.add(new BrandSocialLink(platform, new SocialUrl(link.url())));
        }
        return links;
    }

    private static BrandSocialPlatform platformOf(String name) {
        return Arrays.stream(BrandSocialPlatform.values())
                .filter(p -> p.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new InvalidFieldException("Unknown social platform: " + name));
    }

    /** Which parts of the brand this write changed — for the audit trail, no content. */
    private static List<String> changedParts(Brand before, Brand after) {
        List<String> changed = new ArrayList<>();
        if (before == null) {
            changed.add("brand");
            return changed;
        }
        if (!java.util.Objects.equals(before.slogan(), after.slogan())) {
            changed.add("slogan");
        }
        if (!java.util.Objects.equals(before.shortDescription(), after.shortDescription())) {
            changed.add("shortDescription");
        }
        if (!java.util.Objects.equals(before.logoMediaId(), after.logoMediaId())) {
            changed.add("logo");
        }
        if (!java.util.Objects.equals(before.squareLogoMediaId(), after.squareLogoMediaId())) {
            changed.add("squareLogo");
        }
        if (!java.util.Objects.equals(before.faviconMediaId(), after.faviconMediaId())) {
            changed.add("favicon");
        }
        if (!java.util.Objects.equals(before.coverImageMediaId(), after.coverImageMediaId())) {
            changed.add("coverImage");
        }
        if (!before.colors().equals(after.colors())) {
            changed.add("colors");
        }
        if (!before.socialLinks().equals(after.socialLinks())) {
            changed.add("socialLinks");
        }
        return changed;
    }

    private static String blankNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }
}
