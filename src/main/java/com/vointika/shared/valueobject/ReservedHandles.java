package com.vointika.shared.valueobject;

import java.util.Locale;
import java.util.Set;

/**
 * Labels no tour operator may hold, because a storefront lives at
 * {@code {handle}.vointika.com} and a handle is therefore a <b>hostname</b>.
 *
 * <p>An operator called "API" generates the handle {@code api} and would claim
 * {@code api.vointika.com}. The archive never had this problem because it put
 * tenants on a separate domain; this one does not, so the collision has to be
 * refused in code.
 *
 * <p><b>Why this is not on {@link Handle}.</b> Experiences, pages and menus use
 * the same value object, and there is nothing wrong with an experience called
 * "api" — its handle is a path segment, not a host. The reservation belongs to
 * whatever is addressed by subdomain, which today is only the operator.
 *
 * <p><b>Why it lives in {@code shared}.</b> Two contexts need the same answer
 * and may not import each other: {@code touroperator} at creation, and
 * {@code storefront} at the edge, so a reserved label resolves to no tenant even
 * if a row somehow holds one.
 *
 * <p>The list is deliberately short. Every entry is either a host this project
 * already documents ({@code admin}, {@code api}, {@code media}, {@code themes},
 * {@code staging}) or one that infrastructure takes by convention and would be
 * expensive to discover late. Over-reserving costs an operator their own name,
 * so a label earns its place by being a host somebody would plausibly point at
 * this domain — not by sounding technical.
 */
public final class ReservedHandles {

    private static final Set<String> RESERVED = Set.of(
            // hosts this project documents or already uses
            "admin", "api", "media", "themes", "staging",
            // the environment labels that sit beside staging
            "dev", "test", "preview", "prod", "production",
            // taken by convention almost everywhere; cheap now, disruptive later
            "www", "app", "cdn", "assets", "static", "files", "img", "images",
            "mail", "smtp", "imap", "ftp", "ns", "ns1", "ns2", "mx",
            // operational surfaces that tend to appear once something is live
            "status", "health", "metrics", "docs", "help", "support", "blog",
            "billing", "account", "accounts", "auth", "login", "signup",
            // the apex and its usual aliases, in case a handle is ever read as one
            "vointika", "localhost");

    private ReservedHandles() {
    }

    /**
     * Case-insensitive: a handle is lowercased on the way in, but callers at the
     * edge hand over whatever the Host header carried.
     *
     * <p>{@code Locale.ROOT}, never the JVM default — PATTERNS §11. Under a
     * Turkish default {@code "API".toLowerCase()} is {@code "apı"} and the label
     * would slip through.
     */
    public static boolean isReserved(String candidate) {
        return candidate != null && RESERVED.contains(candidate.toLowerCase(Locale.ROOT));
    }
}
