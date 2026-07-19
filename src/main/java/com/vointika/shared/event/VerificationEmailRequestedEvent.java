package com.vointika.shared.event;

public record VerificationEmailRequestedEvent(String email, String name, String token) {}
