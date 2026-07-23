-- Team invitations: an email is invited to an operator's team, accepts via an
-- emailed raw-token link (only the SHA-256 hash is stored). Keys on email (the
-- invitee may have no account yet). Registered after identity + tour_operators,
-- both of which it FKs into.

CREATE TABLE touroperator.tour_operator_invitations (
    id                   UUID          NOT NULL PRIMARY KEY,
    tour_operator_id     UUID          NOT NULL REFERENCES touroperator.tour_operators (id) ON DELETE CASCADE,
    email                VARCHAR(255)  NOT NULL,
    -- The inviter's label for the invitee: greets them in the invite email and
    -- shows in the pending-invitations list before they have an account. NOT
    -- NULL (the invite form requires it). Distinct from the eventual member name,
    -- which comes from identity on accept.
    name                 VARCHAR(255)  NOT NULL,
    role                 VARCHAR(20)   NOT NULL,
    token_hash           VARCHAR(64)   NOT NULL
        CONSTRAINT tour_operator_invitations_token_hash_unique UNIQUE,
    status               VARCHAR(20)   NOT NULL
        CHECK (status IN ('PENDING', 'ACCEPTED', 'REVOKED', 'EXPIRED')),
    invited_by_user_id   UUID          NOT NULL REFERENCES identity.users (id),
    created_at           TIMESTAMPTZ   NOT NULL,
    expires_at           TIMESTAMPTZ   NOT NULL,
    accepted_at          TIMESTAMPTZ
);

-- At most ONE live (PENDING) invitation per (operator, email) — the DB half of
-- the duplicate-pending 409 guard. Revoked/expired/accepted rows don't block a
-- re-invite.
CREATE UNIQUE INDEX tour_operator_invitations_pending_email_unique
    ON touroperator.tour_operator_invitations (tour_operator_id, email)
    WHERE status = 'PENDING';

CREATE INDEX idx_tour_operator_invitations_operator_status
    ON touroperator.tour_operator_invitations (tour_operator_id, status);
