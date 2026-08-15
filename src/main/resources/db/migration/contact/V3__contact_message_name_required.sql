-- Who wrote the message is not optional.
--
-- V1 made `name` nullable on the argument that refusing a message over a
-- missing name loses the operator an enquiry. That trade was wrong twice over.
--
-- First, the data: this is an inbox a human reads and replies to. A row with an
-- address and no name is a worse enquiry, not a saved one — the operator has to
-- open it to find out who it is from, and the list column is blank.
--
-- Second, and the reason it surfaced now: a nullable column here is invisible to
-- a negative filter. `WHERE NOT (name LIKE 'x')` is UNKNOWN when name is NULL,
-- and WHERE keeps only true rows, so `filter[name][neq]=…` and
-- `filter[name][not_contains]=…` silently drop every nameless row. Measured on
-- the dev database before this migration: 12 messages, `not_contains=zzz`
-- returned 11 — a filter that excludes nothing still lost a row.
--
-- NOT NULL is the fix for THIS column. It is not the fix for the framework:
-- `audit.audit_log.actor_name` is nullable by design (SYSTEM actors, deleted
-- accounts) and carries the same defect. That is a separate change.
--
-- The backfill uses the email's local part rather than a placeholder. The
-- intake endpoint has never shipped — every existing row is dev-seeded — so
-- this touches fixtures only, and a derived value beats inventing 'Anonymous'
-- for rows we can still describe honestly.
UPDATE contact.contact_messages
SET name = split_part(email, '@', 1)
WHERE name IS NULL OR btrim(name) = '';

ALTER TABLE contact.contact_messages
    ALTER COLUMN name SET NOT NULL;
