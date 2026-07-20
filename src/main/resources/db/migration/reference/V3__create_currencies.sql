-- Currency reference data (read-only, curated launch set). No relationships to
-- the other reference tables — a plain code/name/symbol lookup.
CREATE TABLE reference.currencies (
    id     UUID         NOT NULL PRIMARY KEY,
    code   VARCHAR(3)   NOT NULL CONSTRAINT currencies_code_unique UNIQUE,
    name   VARCHAR(100) NOT NULL,
    symbol VARCHAR(10)  NOT NULL
);

INSERT INTO reference.currencies (id, code, name, symbol) VALUES
    ('cccc0001-0000-0000-0000-000000000001', 'DOP', 'Dominican Peso', 'RD$'),
    ('cccc0002-0000-0000-0000-000000000002', 'EUR', 'Euro',           '€'),
    ('cccc0003-0000-0000-0000-000000000003', 'USD', 'US Dollar',      '$');
