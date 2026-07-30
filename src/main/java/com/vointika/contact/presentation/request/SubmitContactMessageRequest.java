package com.vointika.contact.presentation.request;

/** A storefront contact form, relayed by the BFF. */
public record SubmitContactMessageRequest(
        String name,
        String email,
        String summary,
        String content) {}
