-- The operator's legal documents: cancellation, privacy, terms, legal notice.
--
-- A dedicated table rather than a policy_type column on `page`, which already
-- has handles, bodies, translations, SEO and DRAFT/PUBLISHED and would have been
-- cheaper. The entity buys three simplifications, not just separation:
--   * no handle — the type IS the address, so there is no handle uniqueness, no
--     rename endpoint and no localized-handle rule;
--   * no publish state — a policy exists or it does not, and the row's absence is
--     the unpublished state (Shopify has no draft here either);
--   * one per type, structurally — the composite primary key says it, rather than
--     a nullable discriminator plus a partial unique index.
--
-- It lives in touroperator, not page, for the reason brand does: the defining
-- trait is one per operator from a fixed set, like the locales and the password
-- gate — not as many as the operator likes.
--
-- Four types, not Shopify's six. Shipping and Subscription have no analogue —
-- nothing ships — and their Return is our Cancellation, which is the single
-- most-asked question about a tour. A closed set has to be the right closed set;
-- same reasoning as V11's platform list.
--
-- No version history: Shopify's ShopPolicy has none, and freezing terms onto a
-- booking belongs to the transaction arc. No SEO overrides: title is the title
-- tag, and Shopify's policy object carries none either.
--
-- The type list and PolicyType must agree. Widening one without the other is the
-- drift audit_log_actor_type already shipped once, so
-- TourOperatorEnumsMatchTheCheckConstraintsTest reads this constraint and fails
-- the build instead.
CREATE TABLE touroperator.tour_operator_policies (
    tour_operator_id UUID        NOT NULL
                          REFERENCES touroperator.tour_operators (id) ON DELETE CASCADE,
    type             VARCHAR(20) NOT NULL CHECK (type IN
                          ('CANCELLATION','PRIVACY','TERMS','LEGAL_NOTICE')),
    title            VARCHAR(200) NOT NULL,
    -- Raw HTML, stored verbatim, exactly as pages.body is: escaping is a render
    -- concern, and the storefront renders this one unescaped on purpose.
    body             TEXT         NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL,
    PRIMARY KEY (tour_operator_id, type)
);

-- Overlay columns nullable, as every translation table here is: a row overlays,
-- it does not replace, so a null column falls back to the canonical value rather
-- than blanking it.
CREATE TABLE touroperator.tour_operator_policy_translations (
    tour_operator_id UUID        NOT NULL,
    type             VARCHAR(20) NOT NULL,
    locale           VARCHAR(8)  NOT NULL,
    title            VARCHAR(200),
    body             TEXT,
    PRIMARY KEY (tour_operator_id, type, locale),
    FOREIGN KEY (tour_operator_id, type)
        REFERENCES touroperator.tour_operator_policies (tour_operator_id, type) ON DELETE CASCADE
);
