package com.vointika.touroperator.presentation.controller;

import com.vointika.touroperator.application.dto.input.CreateTourOperatorInput;
import com.vointika.touroperator.application.dto.output.CreateTourOperatorOutput;
import com.vointika.touroperator.application.usecase.CreateTourOperatorUseCase;
import com.vointika.touroperator.presentation.request.CreateTourOperatorRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

import java.net.URI;

@RestController
@RequestMapping("/api/tour-operators")
public class TourOperatorController {

    private final CreateTourOperatorUseCase createTourOperatorUseCase;

    public TourOperatorController(CreateTourOperatorUseCase createTourOperatorUseCase) {
        this.createTourOperatorUseCase = createTourOperatorUseCase;
    }

    /**
     * Creates a tour operator; the authenticated caller becomes its OWNER.
     * Any logged-in user may create — there is no membership gate on create
     * (there is no operator to be a member of yet).
     */
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
