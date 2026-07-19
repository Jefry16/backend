-- The reference schema is created by FlywayPerDomainConfig. Reference is a
-- read-mostly, shared-kernel-like context (curated launch data). Country first,
-- then timezones which FK onto it (a country has many timezones).

CREATE TABLE reference.country (
    id   UUID         NOT NULL PRIMARY KEY,
    code VARCHAR(2)   NOT NULL CONSTRAINT country_code_unique UNIQUE,
    name VARCHAR(100) NOT NULL
);

INSERT INTO reference.country (id, code, name) VALUES
    ('11111111-1111-1111-1111-111111111111', 'ES', 'Spain'),
    ('22222222-2222-2222-2222-222222222222', 'US', 'United States'),
    ('33333333-3333-3333-3333-333333333333', 'DO', 'Dominican Republic');

CREATE TABLE reference.timezones (
    id         UUID         NOT NULL PRIMARY KEY,
    name       VARCHAR(100) NOT NULL CONSTRAINT timezones_name_unique UNIQUE,
    city_name  VARCHAR(100) NOT NULL,
    country_id UUID         NOT NULL REFERENCES reference.country (id)
);

CREATE INDEX idx_timezones_country_id ON reference.timezones (country_id);

INSERT INTO reference.timezones (id, name, city_name, country_id) VALUES
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Africa/Ceuta',          'Ceuta',         '11111111-1111-1111-1111-111111111111'),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'America/New_York',      'New York',      '22222222-2222-2222-2222-222222222222'),
    ('cccccccc-cccc-cccc-cccc-cccccccccccc', 'America/Santo_Domingo', 'Santo Domingo', '33333333-3333-3333-3333-333333333333'),
    ('dddddddd-dddd-dddd-dddd-dddddddddddd', 'Europe/Madrid',         'Madrid',        '11111111-1111-1111-1111-111111111111');
