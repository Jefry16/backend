package com.vointika.touroperator.application.dto.input;

/**
 * A policy's canonical text. Both fields are required — a policy with no title
 * or no body is not a document, and absence is expressed by deleting the policy
 * rather than by blanking it.
 */
public record UpsertPolicyInput(String title, String body) {
}
