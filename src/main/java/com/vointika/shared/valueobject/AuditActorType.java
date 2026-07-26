package com.vointika.shared.valueobject;

/**
 * Who initiated an audited action. {@code STOREFRONT} (shopper-side writes)
 * joins when the transaction half (cart → checkout) lands — adding it is a
 * one-line CHECK-constraint migration.
 */
public enum AuditActorType {
    USER,
    SYSTEM
}
