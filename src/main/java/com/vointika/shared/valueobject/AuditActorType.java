package com.vointika.shared.valueobject;

/**
 * Who initiated an audited action. {@code STOREFRONT} (shopper-side writes) joins
 * when the transaction half (cart → checkout) lands.
 *
 * <p>Adding a value is a migration widening {@code audit_log_actor_type_check},
 * and {@code ActorTypesMatchTheCheckConstraintTest} fails the build if you forget
 * it — without the migration the insert dies on SQLSTATE 23514, which nothing
 * translates, and it takes the business action down with it (the append shares
 * that transaction by design).
 *
 * <p>It is one line only if the new actor is <em>id-less</em>. {@link AuditActor}
 * requires an id for {@code USER} and forbids one for everything else, and
 * {@code audit_log_actor_id_presence} enforces the same shape in the database — so
 * a {@code STOREFRONT} actor carrying a shopper or session id means changing both
 * of those too.
 */
public enum AuditActorType {
    USER,
    SYSTEM
}
