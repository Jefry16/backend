package com.vointika.pickup.presentation.controller;

import com.vointika.pickup.application.dto.input.PickupLocationInput;
import com.vointika.pickup.application.usecase.CreatePickupLocationUseCase;
import com.vointika.pickup.application.usecase.DeletePickupLocationUseCase;
import com.vointika.pickup.application.usecase.GetPickupLocationUseCase;
import com.vointika.pickup.application.usecase.ListPickupLocationsUseCase;
import com.vointika.pickup.application.usecase.UpdatePickupLocationUseCase;
import com.vointika.pickup.domain.entity.PickupLocation;
import com.vointika.pickup.presentation.request.PickupLocationRequest;
import com.vointika.pickup.presentation.response.PickupLocationResponse;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.ListQuery;
import com.vointika.shared.web.list.CursorPageResponse;
import com.vointika.shared.web.list.ListQueryParser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

/**
 * An operator's pickup locations (its meeting-point catalog). Membership on the
 * operator is enforced by the {@code /api/tour-operators/**} interceptor
 * (non-member → 404); reads are member-visible, writes are ADMIN+. The catalog
 * is SYNCED onto slots: create backfills, update propagates, delete removes.
 */
@RestController
@RequestMapping("/api/tour-operators/{tourOperatorId}/pickup-locations")
public class PickupLocationController {

    private final CreatePickupLocationUseCase createPickupLocationUseCase;
    private final UpdatePickupLocationUseCase updatePickupLocationUseCase;
    private final ListPickupLocationsUseCase listPickupLocationsUseCase;
    private final GetPickupLocationUseCase getPickupLocationUseCase;
    private final DeletePickupLocationUseCase deletePickupLocationUseCase;
    private final ListQueryParser listQueryParser;

    public PickupLocationController(CreatePickupLocationUseCase createPickupLocationUseCase,
                                    UpdatePickupLocationUseCase updatePickupLocationUseCase,
                                    ListPickupLocationsUseCase listPickupLocationsUseCase,
                                    GetPickupLocationUseCase getPickupLocationUseCase,
                                    DeletePickupLocationUseCase deletePickupLocationUseCase,
                                    ListQueryParser listQueryParser) {
        this.createPickupLocationUseCase = createPickupLocationUseCase;
        this.updatePickupLocationUseCase = updatePickupLocationUseCase;
        this.listPickupLocationsUseCase = listPickupLocationsUseCase;
        this.getPickupLocationUseCase = getPickupLocationUseCase;
        this.deletePickupLocationUseCase = deletePickupLocationUseCase;
        this.listQueryParser = listQueryParser;
    }

    /** The operator's pickup locations — cursor-paginated. Any member. */
    @GetMapping
    public ResponseEntity<CursorPageResponse<PickupLocationResponse>> list(
            @PathVariable UUID tourOperatorId,
            @AuthenticationPrincipal String callerUserId,
            HttpServletRequest request) {
        ListQuery query = listQueryParser.parse(request, ListPickupLocationsUseCase.SCHEMA, tourOperatorId);
        CursorPage<PickupLocation> page = listPickupLocationsUseCase.execute(query, UUID.fromString(callerUserId));
        return ResponseEntity.ok(CursorPageResponse.of(page, PickupLocationResponse::from));
    }

    /** A single pickup location. Any member; 404 if not under this operator. */
    @GetMapping("/{pickupLocationId}")
    public ResponseEntity<PickupLocationResponse> get(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID pickupLocationId,
            @AuthenticationPrincipal String callerUserId) {
        PickupLocation p = getPickupLocationUseCase.execute(
                tourOperatorId, pickupLocationId, UUID.fromString(callerUserId));
        return ResponseEntity.ok(PickupLocationResponse.from(p));
    }

    /** Creates a pickup location (backfills every existing slot). ADMIN+. 201 + Location. Duplicate name → 409. */
    @PostMapping
    public ResponseEntity<Void> create(
            @PathVariable UUID tourOperatorId,
            @RequestBody PickupLocationRequest body,
            @AuthenticationPrincipal String callerUserId) {
        UUID id = createPickupLocationUseCase.execute(
                tourOperatorId, UUID.fromString(callerUserId), toInput(body));
        return ResponseEntity
                .created(URI.create("/api/tour-operators/" + tourOperatorId + "/pickup-locations/" + id))
                .build();
    }

    /** Updates name and/or time (partial; propagates onto slot snapshots). ADMIN+. 204. Duplicate name → 409. */
    @PatchMapping("/{pickupLocationId}")
    public ResponseEntity<Void> update(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID pickupLocationId,
            @RequestBody PickupLocationRequest body,
            @AuthenticationPrincipal String callerUserId) {
        updatePickupLocationUseCase.execute(
                tourOperatorId, pickupLocationId, UUID.fromString(callerUserId), toInput(body));
        return ResponseEntity.noContent().build();
    }

    /** Removes a pickup location (and its slot snapshots). ADMIN+. 204. 404 if not under this operator. */
    @DeleteMapping("/{pickupLocationId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID tourOperatorId,
            @PathVariable UUID pickupLocationId,
            @AuthenticationPrincipal String callerUserId) {
        deletePickupLocationUseCase.execute(tourOperatorId, pickupLocationId, UUID.fromString(callerUserId));
        return ResponseEntity.noContent().build();
    }

    private static PickupLocationInput toInput(PickupLocationRequest b) {
        return new PickupLocationInput(b.name(), b.time());
    }
}
