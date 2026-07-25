package com.vointika.pickup.application.usecase;

import com.vointika.pickup.domain.entity.PickupLocation;
import com.vointika.pickup.domain.repository.PickupLocationRepository;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.ListQuery;
import com.vointika.shared.list.ListSchema;
import com.vointika.shared.port.TourOperatorMembershipCheck;

import java.util.UUID;

/**
 * Lists an operator's pickup locations — cursor-paginated, tenant-scoped. Any
 * member; non-member → 404. Filter by {@code name} (text); sort by
 * {@code createdAt} (default, newest first), {@code name}, {@code time}, or
 * {@code id}.
 */
public class ListPickupLocationsUseCase {

    public static final ListSchema SCHEMA = ListSchema.builder()
            .tenantScoped()
            .text("name")
            .time("time")
            .instant("createdAt")
            .sortable("name")
            .sortable("time")
            .sortable("createdAt")
            .sortable("id")
            .defaultSort("-createdAt")
            .build();

    private final PickupLocationRepository pickupLocationRepository;
    private final TourOperatorMembershipCheck membershipCheck;

    public ListPickupLocationsUseCase(PickupLocationRepository pickupLocationRepository,
                                      TourOperatorMembershipCheck membershipCheck) {
        this.pickupLocationRepository = pickupLocationRepository;
        this.membershipCheck = membershipCheck;
    }

    public CursorPage<PickupLocation> execute(ListQuery query, UUID callerUserId) {
        membershipCheck.ensureMember(callerUserId, query.tenantId());
        return pickupLocationRepository.list(query);
    }
}
