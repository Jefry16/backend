package com.vointika.touroperator.presentation.controller;

import com.vointika.touroperator.application.dto.input.CreateTourOperatorInput;
import com.vointika.touroperator.application.dto.output.CreateTourOperatorOutput;
import com.vointika.touroperator.application.usecase.CreateTourOperatorUseCase;
import com.vointika.touroperator.presentation.request.CreateTourOperatorRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.vointika.touroperator.application.dto.input.UpdateTourOperatorInput;
import com.vointika.touroperator.application.usecase.GetTourOperatorUseCase;
import com.vointika.touroperator.application.usecase.UpdateTourOperatorUseCase;
import com.vointika.touroperator.presentation.response.TourOperatorDetailsResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import org.springframework.http.ResponseEntity;

import java.net.URI;

@RestController
@RequestMapping("/api/tour-operators")
public class TourOperatorController {

    private final CreateTourOperatorUseCase createTourOperatorUseCase;
    private final GetTourOperatorUseCase getUseCase;
    private final UpdateTourOperatorUseCase updateUseCase;

    public TourOperatorController(CreateTourOperatorUseCase createTourOperatorUseCase,
                                  GetTourOperatorUseCase getUseCase,
                                  UpdateTourOperatorUseCase updateUseCase) {
        this.createTourOperatorUseCase = createTourOperatorUseCase;
        this.getUseCase = getUseCase;
        this.updateUseCase = updateUseCase;
    }

    /**
     * Creates a tour operator; the authenticated caller becomes its OWNER.
     * Any logged-in user may create — there is no membership gate on create
     * (there is no operator to be a member of yet).
     */
    /**
     * The operator's own details. Member-visible. Closes the gap where the
     * controller could create an operator and never read one back.
     */
    @GetMapping("/{tourOperatorId}")
    public ResponseEntity<TourOperatorDetailsResponse> get(
            @PathVariable UUID tourOperatorId,
            @AuthenticationPrincipal String userIdStr) {
        return ResponseEntity.ok(TourOperatorDetailsResponse.from(
                getUseCase.execute(tourOperatorId, UUID.fromString(userIdStr))));
    }

    /**
     * Edits the operator's details. ADMIN+. <b>PATCH: an absent field is
     * unchanged</b>, and a blank string clears an optional one (phone, email).
     * {@code handle} is not editable — it is the storefront subdomain.
     *
     * <p>Changing {@code timezoneId} reinterprets every stored departure; see
     * {@code UpdateTourOperatorUseCase}.
     */
    @PatchMapping("/{tourOperatorId}")
    public ResponseEntity<Void> update(
            @PathVariable UUID tourOperatorId,
            @RequestBody UpdateTourOperatorInput body,
            @AuthenticationPrincipal String userIdStr) {
        updateUseCase.execute(tourOperatorId, body, UUID.fromString(userIdStr));
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    public ResponseEntity<Void> create(
            @RequestBody CreateTourOperatorRequest request,
            @AuthenticationPrincipal String userId) {
        CreateTourOperatorOutput output = createTourOperatorUseCase.execute(
                new CreateTourOperatorInput(
                        userId,
                        request.name(),
                        request.address(),
                        request.timezoneId(),
                        request.currencyId()));
        return ResponseEntity.created(URI.create("/api/tour-operators/" + output.id())).build();
    }
}
