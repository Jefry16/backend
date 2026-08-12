package com.vointika.storefront.application.dto.output;

/**
 * Everything the gate shows: the shop's name and the operator's own message for
 * visitors. Both are public by construction — this is the one storefront address
 * a locked store answers with content.
 */
public record PasswordPageOutput(String shopName, String passwordMessage) {}
