package com.vointika.touroperator.application.usecase;

import com.vointika.touroperator.application.dto.output.BrandView;
import com.vointika.touroperator.domain.entity.Brand;
import com.vointika.touroperator.domain.repository.TourOperatorBrandRepository;
import com.vointika.shared.port.TourOperatorMembershipCheck;

import java.util.UUID;

/**
 * The operator's brand, for the editor. Member-visible.
 *
 * <p><b>An operator with no brand row gets an empty brand, not a 404.</b> The
 * row's absence means "nothing filled in yet", which is the state most operators
 * are in and exactly what the form should render — the same answer the storefront
 * port gives, so the two surfaces agree.
 */
public class GetBrandUseCase {

    private final TourOperatorBrandRepository brandRepository;
    private final TourOperatorMembershipCheck membershipCheck;

    public GetBrandUseCase(TourOperatorBrandRepository brandRepository,
                           TourOperatorMembershipCheck membershipCheck) {
        this.brandRepository = brandRepository;
        this.membershipCheck = membershipCheck;
    }

    public BrandView execute(UUID tourOperatorId, UUID callerUserId) {
        membershipCheck.ensureMember(callerUserId, tourOperatorId);
        return BrandView.from(brandRepository.findByTourOperatorId(tourOperatorId)
                .orElseGet(() -> Brand.empty(tourOperatorId)));
    }
}
