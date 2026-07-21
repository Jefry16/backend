-- Grow the supported content-language set: Portuguese + German. Data-only —
-- operators can enable these immediately (validated via existsByCode). Continues
-- the fixed-UUID scheme from V4 (dddd000X).
INSERT INTO reference.languages (id, code, name) VALUES
    ('dddd0005-0000-0000-0000-000000000005', 'pt', 'Portuguese'),
    ('dddd0006-0000-0000-0000-000000000006', 'de', 'German');
