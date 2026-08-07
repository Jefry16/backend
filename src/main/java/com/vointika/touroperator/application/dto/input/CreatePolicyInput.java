package com.vointika.touroperator.application.dto.input;

/**
 * A new policy. {@code type} is in the body rather than the path because it is
 * chosen at creation and immutable afterwards — retyping a policy would move it
 * to another storefront URL, which is a delete and a create.
 */
public record CreatePolicyInput(String type, String title, String body) {
}
