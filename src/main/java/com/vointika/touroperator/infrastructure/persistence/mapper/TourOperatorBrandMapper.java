package com.vointika.touroperator.infrastructure.persistence.mapper;

import com.vointika.touroperator.domain.entity.Brand;
import com.vointika.touroperator.domain.entity.BrandColor;
import com.vointika.touroperator.domain.entity.BrandSocialLink;
import com.vointika.touroperator.domain.valueobject.BrandShortDescription;
import com.vointika.touroperator.domain.valueobject.BrandSlogan;
import com.vointika.touroperator.domain.valueobject.HexColor;
import com.vointika.touroperator.domain.valueobject.SocialUrl;
import com.vointika.touroperator.infrastructure.persistence.entity.TourOperatorBrandColorJpaEntity;
import com.vointika.touroperator.infrastructure.persistence.entity.TourOperatorBrandJpaEntity;
import com.vointika.touroperator.infrastructure.persistence.entity.TourOperatorBrandSocialLinkJpaEntity;

import java.util.List;

public class TourOperatorBrandMapper {

    public static TourOperatorBrandJpaEntity toJpa(Brand b) {
        return new TourOperatorBrandJpaEntity(
                b.tourOperatorId(),
                b.slogan() == null ? null : b.slogan().value(),
                b.shortDescription() == null ? null : b.shortDescription().value(),
                b.logoMediaId(), b.squareLogoMediaId(),
                b.faviconMediaId(), b.coverImageMediaId(),
                b.createdAt(), b.updatedAt());
    }

    public static Brand toDomain(TourOperatorBrandJpaEntity jpa,
                                 List<TourOperatorBrandColorJpaEntity> colors,
                                 List<TourOperatorBrandSocialLinkJpaEntity> links) {
        return new Brand(
                jpa.getTourOperatorId(),
                jpa.getSlogan() == null ? null : new BrandSlogan(jpa.getSlogan()),
                jpa.getShortDescription() == null
                        ? null : new BrandShortDescription(jpa.getShortDescription()),
                jpa.getLogoMediaId(), jpa.getSquareLogoMediaId(),
                jpa.getFaviconMediaId(), jpa.getCoverImageMediaId(),
                colors.stream()
                        .map(c -> new BrandColor(c.getRole(), c.getPosition(),
                                new HexColor(c.getBackground()), new HexColor(c.getForeground())))
                        .toList(),
                links.stream()
                        .map(l -> new BrandSocialLink(l.getPlatform(), new SocialUrl(l.getUrl())))
                        .toList(),
                jpa.getCreatedAt(), jpa.getUpdatedAt());
    }

    public static TourOperatorBrandColorJpaEntity toJpa(java.util.UUID operatorId, BrandColor c) {
        return new TourOperatorBrandColorJpaEntity(
                operatorId, c.role(), (short) c.position(),
                c.background().value(), c.foreground().value());
    }

    public static TourOperatorBrandSocialLinkJpaEntity toJpa(java.util.UUID operatorId,
                                                             BrandSocialLink l) {
        return new TourOperatorBrandSocialLinkJpaEntity(
                operatorId, l.platform(), l.url().value());
    }

    private TourOperatorBrandMapper() {}
}
