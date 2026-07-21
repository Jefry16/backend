-- Content-language reference (read-only, curated launch set) — the master list
-- operators enable a subset of for storefront/experience content. A plain
-- code/name lookup; the BCP-47 code is the natural key. No FK from operators —
-- gated via LanguageRepository.existsByCode at the write boundary. Distinct
-- from the admin-UI language (a config allowlist on identity.users).
CREATE TABLE reference.languages (
    id   UUID         NOT NULL PRIMARY KEY,
    code VARCHAR(8)   NOT NULL CONSTRAINT languages_code_unique UNIQUE,
    name VARCHAR(80)  NOT NULL
);

INSERT INTO reference.languages (id, code, name) VALUES
    ('dddd0001-0000-0000-0000-000000000001', 'en', 'English'),
    ('dddd0002-0000-0000-0000-000000000002', 'es', 'Spanish'),
    ('dddd0003-0000-0000-0000-000000000003', 'fr', 'French'),
    ('dddd0004-0000-0000-0000-000000000004', 'it', 'Italian');
