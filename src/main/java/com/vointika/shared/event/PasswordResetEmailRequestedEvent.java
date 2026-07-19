package com.vointika.shared.event;

public record PasswordResetEmailRequestedEvent(String email, String name, String token) {}
