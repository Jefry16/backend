package com.vointika.contact.presentation.controller;

import com.vointika.contact.application.usecase.SubmitContactMessageUseCase;
import com.vointika.contact.presentation.request.SubmitContactMessageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The storefront contact form's landing point — the inbox's only writer, and the
 * half of the contact context that #63 deliberately left unbuilt until a
 * storefront existed to call it.
 *
 * <p>Separate from {@code ContactMessageController}: that one is the operator's
 * JWT-authenticated inbox, this one is an anonymous stranger's message arriving
 * through the BFF. Same data, opposite directions and opposite trust.
 */
@RestController
@RequestMapping("/api/internal/storefront/{tenantSlug}")
public class ContactIntakeController {

    private final SubmitContactMessageUseCase submitUseCase;

    public ContactIntakeController(SubmitContactMessageUseCase submitUseCase) {
        this.submitUseCase = submitUseCase;
    }

    /**
     * 204 on success: the shopper gets a thank-you page from the BFF, and there
     * is nothing about the stored row a public caller should be handed back.
     */
    @PostMapping("/contact-messages")
    public ResponseEntity<Void> submit(@PathVariable String tenantSlug,
                                       @RequestBody SubmitContactMessageRequest request) {
        submitUseCase.execute(
                tenantSlug, request.name(), request.email(), request.summary(), request.content());

        return ResponseEntity.noContent().build();
    }
}
