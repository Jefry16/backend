package com.vointika.experience.application.usecase;

import com.vointika.experience.application.dto.output.SlotView;
import com.vointika.experience.domain.entity.Slot;
import com.vointika.experience.domain.entity.SlotAudiencePricing;
import com.vointika.experience.domain.repository.SlotAudiencePricingRepository;
import com.vointika.experience.domain.repository.SlotRepository;
import com.vointika.experience.domain.valueobject.SlotStatus;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.ListQuery;
import com.vointika.shared.list.ListSchema;
import com.vointika.shared.port.TourOperatorMembershipCheck;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Lists an operator's slots across all experiences — cursor-paginated,
 * tenant-scoped, soonest-first. Any member; non-member → 404. Filter by
 * {@code status} / {@code day} / {@code experienceId} (sets) or
 * {@code experienceName} (text); sort by {@code startAt} (default),
 * {@code experienceName}, {@code createdAt}, {@code status}, {@code day}, or
 * {@code id}. Pricing for the whole page is loaded in ONE batched lookup (no N+1).
 */
public class ListSlotsUseCase {

    public static final ListSchema SCHEMA = ListSchema.builder()
            .tenantScoped()
            .set("status", SlotStatus.class)
            .set("day", Integer.class)
            .set("experienceId", UUID.class)
            .text("experienceName")
            .instant("createdAt")
            .sortable("startAt")
            .sortable("experienceName")
            .sortable("createdAt")
            .sortable("status")
            .sortable("day")
            .sortable("id")
            .defaultSort("startAt")
            .build();

    private final SlotRepository slotRepository;
    private final SlotAudiencePricingRepository pricingRepository;
    private final TourOperatorMembershipCheck membershipCheck;

    public ListSlotsUseCase(SlotRepository slotRepository,
                            SlotAudiencePricingRepository pricingRepository,
                            TourOperatorMembershipCheck membershipCheck) {
        this.slotRepository = slotRepository;
        this.pricingRepository = pricingRepository;
        this.membershipCheck = membershipCheck;
    }

    public CursorPage<SlotView> execute(ListQuery query, UUID callerUserId) {
        membershipCheck.ensureMember(callerUserId, query.tenantId());

        CursorPage<Slot> page = slotRepository.list(query);
        if (page.data().isEmpty()) {
            return new CursorPage<>(List.of(), page.nextCursor());
        }

        List<UUID> slotIds = page.data().stream().map(Slot::id).toList();
        Map<UUID, List<SlotAudiencePricing>> pricingBySlot =
                pricingRepository.findBySlotIds(slotIds).stream()
                        .collect(Collectors.groupingBy(SlotAudiencePricing::slotId));

        return new CursorPage<>(
                page.data().stream()
                        .map(s -> SlotView.from(s, pricingBySlot.getOrDefault(s.id(), List.of())))
                        .toList(),
                page.nextCursor());
    }
}
