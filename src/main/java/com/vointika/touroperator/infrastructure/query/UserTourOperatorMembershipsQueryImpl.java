package com.vointika.touroperator.infrastructure.query;

import com.vointika.reference.domain.entity.Timezone;
import com.vointika.reference.domain.repository.TimezoneRepository;
import com.vointika.shared.port.UserTourOperatorMembershipsQuery;
import com.vointika.touroperator.infrastructure.persistence.entity.TourOperatorJpaEntity;
import com.vointika.touroperator.infrastructure.persistence.entity.TourOperatorMemberJpaEntity;
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
 * <p>Two lookups + a small reference join, all in memory (a user has a handful
 * of memberships): memberships → their operators → timezone names.
 * {@code logoUrl} is null until the operator-logo feature lands (the create
 * slice's model has no logo). {@code timezone} is the IANA name resolved from
 * the operator's {@code timezone_id} via {@code reference}.
 */
@Component
public class UserTourOperatorMembershipsQueryImpl implements UserTourOperatorMembershipsQuery {

    private final TourOperatorMemberJpaRepository memberRepository;
    private final TourOperatorJpaRepository operatorRepository;
    private final TimezoneRepository timezoneRepository;

    public UserTourOperatorMembershipsQueryImpl(
            TourOperatorMemberJpaRepository memberRepository,
            TourOperatorJpaRepository operatorRepository,
            TimezoneRepository timezoneRepository) {
        this.memberRepository = memberRepository;
        this.operatorRepository = operatorRepository;
        this.timezoneRepository = timezoneRepository;
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
        Map<UUID, String> timezoneNames = timezoneRepository.findAll().stream()
                .collect(Collectors.toMap(Timezone::getId, Timezone::getName));

        return memberships.stream()
                .map(m -> {
                    TourOperatorJpaEntity op = operators.get(m.getTourOperatorId());
                    if (op == null) {
                        return null; // defensive — a membership always has its operator (FK)
                    }
                    return new TourOperatorMembershipView(
                            op.getId(),
                            op.getName(),
                            null, // logoUrl — no operator logo in the model yet
                            timezoneNames.get(op.getTimezoneId()),
                            m.isDefault(),
                            m.getRole().name());
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(TourOperatorMembershipView::isDefault).reversed()
                        .thenComparing(TourOperatorMembershipView::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }
}
