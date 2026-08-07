package com.vointika.touroperator.application.dto.input;

/**
 * A policy's new text. There is no {@code type} here: it is the policy's
 * identity and its address on the storefront, so changing it is a delete and a
 * create rather than an update.
 */
public record UpdatePolicyInput(String title, String body) {
}
