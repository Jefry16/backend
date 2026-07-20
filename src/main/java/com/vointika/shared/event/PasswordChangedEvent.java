package com.vointika.shared.event;

public record PasswordChangedEvent(String email, String name, String locale) {}
