package com.vointika.touroperator.infrastructure.query;

import com.vointika.reference.domain.entity.Currency;
import com.vointika.reference.domain.entity.Timezone;
import com.vointika.reference.domain.repository.CurrencyRepository;
import com.vointika.reference.domain.repository.TimezoneRepository;
import com.vointika.shared.media.MediaUrlBatchResolver;
import com.vointika.shared.port.UserTourOperatorMembershipsQuery;
import com.vointika.touroperator.infrastructure.persistence.entity.TourOperatorBrandJpaEntity;
import com.vointika.touroperator.infrastructure.persistence.entity.TourOperatorJpaEntity;
import com.vointika.touroperator.infrastructure.persistence.entity.TourOperatorMemberJpaEntity;
import com.vointika.touroperator.infrastructure.persistence.repository.TourOperatorBrandJpaRepository;
import com.vointika.touroperator.infrastructure.persistence.repository.TourOperatorJpaRepository;
import com.vointika.touroperator.infrastructure.persistence.repository.TourOperatorMemberJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * touroperator's implementation of the shared read port that surfaces a user's
 * operator memberships in their identity profile (the operator switcher).
 * Retires {@code MembershipsQueryFallbackConfig}.
 *
 * <p>Three lookups + two small reference joins, all in memory (a user has a
 * handful of memberships): memberships → their operators → their brand rows →
 * timezone names and currency codes. Both reference tables are curated and tiny,
 * which is why they are read whole and joined here rather than per row.
 * {@code timezone} is the IANA name and {@code currency} the ISO 4217 code,
 * resolved from the operator's {@code timezone_id} / {@code currency_id}.
 *
 * <p>The logo is read off the <b>brand</b> row, where V10 moved it. An operator
 * created since the migration has no brand row yet, which is simply no logo.
 */
@Component
public class UserTourOperatorMembershipsQueryImpl implements UserTourOperatorMembershipsQuery {

    private final TourOperatorMemberJpaRepository memberRepository;
    private final TourOperatorJpaRepository operatorRepository;
    private final TourOperatorBrandJpaRepository brandRepository;
    private final TimezoneRepository timezoneRepository;
    private final CurrencyRepository currencyRepository;
    private final MediaUrlBatchResolver mediaUrlBatchResolver;

    public UserTourOperatorMembershipsQueryImpl(
            TourOperatorMemberJpaRepository memberRepository,
            TourOperatorJpaRepository operatorRepository,
            TourOperatorBrandJpaRepository brandRepository,
            TimezoneRepository timezoneRepository,
            CurrencyRepository currencyRepository,
            MediaUrlBatchResolver mediaUrlBatchResolver) {
        this.memberRepository = memberRepository;
        this.operatorRepository = operatorRepository;
        this.brandRepository = brandRepository;
        this.timezoneRepository = timezoneRepository;
        this.currencyRepository = currencyRepository;
        this.mediaUrlBatchResolver = mediaUrlBatchResolver;
    }

    @Override
    public List<TourOperatorMembershipView> findForUser(UUID userId) {
        List<TourOperatorMemberJpaEntity> memberships = memberRepository.findByUserId(userId);
        if (memberships.isEmpty()) {
            return List.of();
        }

        List<UUID> operatorIds = memberships.stream()
                .map(TourOperatorMemberJpaEntity::getTourOperatorId)
                .distinct()
                .toList();
        Map<UUID, TourOperatorJpaEntity> operators = operatorRepository.findByIdIn(operatorIds).stream()
                .collect(Collectors.toMap(TourOperatorJpaEntity::getId, Function.identity()));
        Map<UUID, UUID> logoMediaIds = brandRepository.findAllById(operatorIds).stream()
                .filter(brand -> brand.getLogoMediaId() != null)
                .collect(Collectors.toMap(
                        TourOperatorBrandJpaEntity::getTourOperatorId,
                        TourOperatorBrandJpaEntity::getLogoMediaId));
        Map<UUID, String> timezoneNames = timezoneRepository.findAll().stream()
                .collect(Collectors.toMap(Timezone::getId, Timezone::getName));
        Map<UUID, String> currencyCodes = currencyRepository.findAll().stream()
                .collect(Collectors.toMap(Currency::getId, Currency::getCode));

        return memberships.stream()
                .map(m -> {
                    TourOperatorJpaEntity op = operators.get(m.getTourOperatorId());
                    if (op == null) {
                        return null; // defensive — a membership always has its operator (FK)
                    }
                    return new TourOperatorMembershipView(
                            op.getId(),
                            op.getName(),
                            // logo id → url through the media seam; null if unset or since-deleted
                            mediaUrlBatchResolver.resolveOne(op.getId(), logoMediaIds.get(op.getId())),
                            timezoneNames.get(op.getTimezoneId()),
                            currencyCodes.get(op.getCurrencyId()),
                            m.isDefault(),
                            m.getRole().name());
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(TourOperatorMembershipView::isDefault).reversed()
                        .thenComparing(TourOperatorMembershipView::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }
}
