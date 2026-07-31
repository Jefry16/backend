package com.vointika.experience.presentation.controller;

import com.vointika.experience.application.dto.input.CreateSlotInput;
import com.vointika.experience.application.dto.input.CreateSlotsInput;
import com.vointika.experience.application.dto.input.UpdateSlotInput;
import com.vointika.experience.application.dto.output.SlotView;
import com.vointika.experience.application.usecase.CancelSlotUseCase;
import com.vointika.experience.application.usecase.CreateSlotUseCase;
import com.vointika.experience.application.usecase.CreateSlotsUseCase;
import com.vointika.experience.application.usecase.GetSlotUseCase;
import com.vointika.experience.application.usecase.ListSlotsUseCase;
import com.vointika.experience.application.usecase.UpdateSlotUseCase;
import com.vointika.experience.presentation.request.CreateSlotRequest;
import com.vointika.experience.presentation.request.CreateSlotsRequest;
import com.vointika.experience.presentation.response.SlotResponse;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.ListQuery;
import com.vointika.shared.web.list.CursorPageResponse;
import com.vointika.shared.web.list.ListQueryParser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

/**
 * An operator's slots (bookable departures). Membership enforced by the
 * {@code /api/tour-operators/**} interceptor (non-member → 404). Reads are
 * member-visible; create / cancel / edit are ADMIN+. Slots span two base paths
 * ({@code /slots} and {@code /experiences/{id}/slots}), so paths are per-method.
 */
@RestController
public class SlotController {

    private final ListSlotsUseCase listSlotsUseCase;
    private final GetSlotUseCase getSlotUseCase;
    private final CreateSlotUseCase createSlotUseCase;
    private final CreateSlotsUseCase createSlotsUseCase;
    private final CancelSlotUseCase cancelSlotUseCase;
    private final UpdateSlotUseCase updateSlotUseCase;
    private final ListQueryParser listQueryParser;

    public SlotController(ListSlotsUseCase listSlotsUseCase,
                          GetSlotUseCase getSlotUseCase,
                          CreateSlotUseCase createSlotUseCase,
                          CreateSlotsUseCase createSlotsUseCase,
                          CancelSlotUseCase cancelSlotUseCase,
                          UpdateSlotUseCase updateSlotUseCase,
                          ListQueryParser listQueryParser) {
        this.listSlotsUseCase = listSlotsUseCase;
        this.getSlotUseCase = getSlotUseCase;
        this.createSlotUseCase = createSlotUseCase;
        this.createSlotsUseCase = createSlotsUseCase;
        this.cancelSlotUseCase = cancelSlotUseCase;
        this.updateSlotUseCase = updateSlotUseCase;
        this.listQueryParser = listQueryParser;
    }

    /** The operator's slots — cursor-paginated, soonest-first. Any member. */
    @GetMapping("/api/tour-operators/{tourOperatorId}/slots")
    public ResponseEntity<CursorPageResponse<SlotResponse>> list(
            @PathVariable UUID tourOperatorId,
            @AuthenticationPrincipal String callerUserId,
            HttpServletRequest request) {
        ListQuery query = listQueryParser.parse(request, ListSlotsUseCase.SCHEMA, tourOperatorId);
        CursorPage<SlotView> page = listSlotsUseCase.execute(query, UUID.fromString(callerUserId));
        return ResponseEntity.ok(CursorPageResponse.of(page, SlotResponse::from));
    }

    /** A single slot with its pricing. Any member; 404 if not under this operator. */
    @GetMapping("/api/tour-operators/{tourOperatorId}/slots/{slotId}")
    public ResponseEntity<SlotResponse> get(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID slotId,
            @AuthenticationPrincipal String callerUserId) {
        SlotView view = getSlotUseCase.execute(tourOperatorId, slotId, UUID.fromString(callerUserId));
        return ResponseEntity.ok(SlotResponse.from(view));
    }

    /**
     * Creates recurring slots (weekday pattern × date window). ADMIN+. 201.
     * A pattern no date in the window satisfies creates nothing → 422.
     */
    @PostMapping("/api/tour-operators/{tourOperatorId}/experiences/{experienceId}/slots")
    public ResponseEntity<Void> createRecurring(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID experienceId,
            @RequestBody CreateSlotsRequest body,
            @AuthenticationPrincipal String callerUserId) {
        createSlotsUseCase.execute(new CreateSlotsInput(
                UUID.fromString(callerUserId), tourOperatorId, experienceId,
                body.days(), body.startTime(), body.endTime(),
                body.validFrom(), body.validTo(), body.audiencePrices()));
        return ResponseEntity.status(201).build();
    }

    /** Creates one slot. ADMIN+. 201 + Location. */
    @PostMapping("/api/tour-operators/{tourOperatorId}/experiences/{experienceId}/slot")
    public ResponseEntity<Void> createOne(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID experienceId,
            @RequestBody CreateSlotRequest body,
            @AuthenticationPrincipal String callerUserId) {
        UUID id = createSlotUseCase.execute(new CreateSlotInput(
                UUID.fromString(callerUserId), tourOperatorId, experienceId,
                body.startAt(), body.endAt(), body.audiencePrices()));
        return ResponseEntity
                .created(URI.create("/api/tour-operators/" + tourOperatorId + "/slots/" + id))
                .build();
    }

    /** Cancels a slot (terminal). ADMIN+. 200 with the refreshed slot; already cancelled → 409. */
    @PostMapping("/api/tour-operators/{tourOperatorId}/slots/{slotId}/cancel")
    public ResponseEntity<SlotResponse> cancel(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID slotId,
            @AuthenticationPrincipal String callerUserId) {
        SlotView refreshed = cancelSlotUseCase.execute(tourOperatorId, slotId, UUID.fromString(callerUserId));
        return ResponseEntity.ok(SlotResponse.from(refreshed));
    }

    /** Sets status (AVAILABLE/SOLD_OUT) and/or per-audience capacity. ADMIN+. 200 with the refreshed slot. */
    @PatchMapping("/api/tour-operators/{tourOperatorId}/slots/{slotId}")
    public ResponseEntity<SlotResponse> update(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID slotId,
            @RequestBody(required = false) UpdateSlotInput body,
            @AuthenticationPrincipal String callerUserId) {
        // An omitted body is a legal no-op PATCH, so it binds as an empty edit.
        UpdateSlotInput input = body == null ? new UpdateSlotInput(null, null) : body;
        SlotView refreshed = updateSlotUseCase.execute(
                tourOperatorId, slotId, UUID.fromString(callerUserId), input);
        return ResponseEntity.ok(SlotResponse.from(refreshed));
    }
}
