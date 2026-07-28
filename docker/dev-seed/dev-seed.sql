-- Dev-only seed data. Applied by the docker-compose `seed` service after the
-- app's Flyway migrations finish. NOT a Flyway migration; never runs in
-- production. The method is the archive's: pure SQL (no cross-context Java),
-- idempotent, fail-loud.
--
-- Idempotent: every insert is ON CONFLICT DO NOTHING, so the service re-runs
-- on every `docker compose up` without duplicating rows. Fixed UUIDs keep the
-- cross-row foreign keys stable across re-runs. ON_ERROR_STOP is set by
-- run.sh: schema drift (a renamed/dropped column) aborts the WHOLE seed
-- loudly instead of half-applying — if this file fails after a migration,
-- update it in the same PR (renames-must-update-dev-seed rule).
--
-- Seeds: a verified admin user, the demo tour operator (slug `acme`, en+es
-- locales), OWNER membership, two audiences (+es overlay), two pickup
-- locations, three PUBLISHED experiences (+es overlay on one), two AVAILABLE
-- slots per experience dated relative to today with per-audience pricing
-- (one tier carries booked seats so the capacity floor is exercisable),
-- three CMS pages (2 published, 1 draft, +es overlay on About), and the two
-- default navigation menus with a small demo tree (+es overlay on Home),
-- and three contact-inbox messages (2 unread, 1 read).
--
-- Deliberately NOT seeded: media rows (a row without its MinIO object renders
-- broken images — upload through the admin instead) and audit entries (the
-- trail records real actions only; seeded history would be fiction).
--
-- Login: admin@vointika.test / password

\set user_id            '01900000-0000-7000-8000-000000000001'
\set operator_id        '01900000-0000-7000-8000-000000000002'
\set member_id          '01900000-0000-7000-8000-000000000003'

\set audience_adult_id  '01900000-0000-7000-8000-000000000010'
\set audience_child_id  '01900000-0000-7000-8000-000000000011'

\set pickup_port_id     '01900000-0000-7000-8000-000000000015'
\set pickup_marina_id   '01900000-0000-7000-8000-000000000016'

\set experience_a_id    '01900000-0000-7000-8000-000000000020'
\set experience_b_id    '01900000-0000-7000-8000-000000000021'
\set experience_c_id    '01900000-0000-7000-8000-000000000022'

\set slot_a1_id         '01900000-0000-7000-8000-000000000030'
\set slot_a2_id         '01900000-0000-7000-8000-000000000031'
\set slot_b1_id         '01900000-0000-7000-8000-000000000032'
\set slot_b2_id         '01900000-0000-7000-8000-000000000033'
\set slot_c1_id         '01900000-0000-7000-8000-000000000034'
\set slot_c2_id         '01900000-0000-7000-8000-000000000035'

\set sap_a1_adult_id    '01900000-0000-7000-8000-000000000040'
\set sap_a1_child_id    '01900000-0000-7000-8000-000000000041'
\set sap_a2_adult_id    '01900000-0000-7000-8000-000000000042'
\set sap_a2_child_id    '01900000-0000-7000-8000-000000000043'
\set sap_b1_adult_id    '01900000-0000-7000-8000-000000000044'
\set sap_b1_child_id    '01900000-0000-7000-8000-000000000045'
\set sap_b2_adult_id    '01900000-0000-7000-8000-000000000046'
\set sap_b2_child_id    '01900000-0000-7000-8000-000000000047'
\set sap_c1_adult_id    '01900000-0000-7000-8000-000000000048'
\set sap_c1_child_id    '01900000-0000-7000-8000-000000000049'
\set sap_c2_adult_id    '01900000-0000-7000-8000-00000000004a'
\set sap_c2_child_id    '01900000-0000-7000-8000-00000000004b'

\set page_about_id      '01900000-0000-7000-8000-000000000050'
\set page_contact_id    '01900000-0000-7000-8000-000000000051'
\set page_faq_id        '01900000-0000-7000-8000-000000000052'

\set menu_main_id       '01900000-0000-7000-8000-000000000060'
\set menu_footer_id     '01900000-0000-7000-8000-000000000061'
\set mi_home_id         '01900000-0000-7000-8000-000000000062'
\set mi_experiences_id  '01900000-0000-7000-8000-000000000063'
\set mi_about_id        '01900000-0000-7000-8000-000000000064'
\set mi_contact_id      '01900000-0000-7000-8000-000000000065'

\set cm_sizes_id        '01900000-0000-7000-8000-000000000070'
\set cm_group_id        '01900000-0000-7000-8000-000000000071'
\set cm_gift_id         '01900000-0000-7000-8000-000000000072'

-- 1. Admin user. Password is the literal string "password" — the hash is a
-- fixed, precomputed BCrypt ($2a$, cost 10) digest accepted by
-- BCryptPasswordEncoder.matches(...).
--
-- Heads-up if you regenerate: do NOT reuse $2a$10$N9qo8uLOickgx2ZMRZoMy... —
-- that string is the SENTINEL_HASH inside the login use case (constant-time
-- guard when no user is found) and is NOT the hash of "password". Planting it
-- here would make every login attempt 401.
INSERT INTO identity.users
    (id, email, name, hashed_password, status, language, created_at, updated_at)
VALUES
    (:'user_id', 'admin@vointika.test', 'Dev Admin',
     '$2a$10$g/AqLa2GMVbNRPyYas8h1e/.Gtfk8YsvHq8g0QTJrCZaRhmnRYr3m',
     'VERIFIED', 'en', NOW(), NOW())
ON CONFLICT DO NOTHING;

-- 2. Tour operator. Timezone/currency resolve by name/code with a fallback to
-- the first reference row, so a changed reference set still seeds.
INSERT INTO touroperator.tour_operators
    (id, name, slug, timezone_id, currency_id, address, primary_locale,
     created_by, created_at, updated_at)
VALUES
    (:'operator_id', 'Acme Tours', 'acme',
     COALESCE(
         (SELECT id FROM reference.timezones WHERE name = 'Europe/Madrid'),
         (SELECT id FROM reference.timezones ORDER BY name LIMIT 1)),
     COALESCE(
         (SELECT id FROM reference.currencies WHERE code = 'EUR'),
         (SELECT id FROM reference.currencies ORDER BY code LIMIT 1)),
     'Calle Mayor 1, 28013 Madrid', 'en',
     :'user_id', NOW(), NOW())
ON CONFLICT DO NOTHING;

INSERT INTO touroperator.tour_operator_locales (tour_operator_id, locale)
VALUES
    (:'operator_id', 'en'),
    (:'operator_id', 'es')
ON CONFLICT DO NOTHING;

-- 3. OWNER membership (name/email denormalized per the roster model).
INSERT INTO touroperator.tour_operator_members
    (id, tour_operator_id, user_id, role, is_default, joined_at, name, email)
VALUES
    (:'member_id', :'operator_id', :'user_id', 'OWNER', TRUE, NOW(),
     'Dev Admin', 'admin@vointika.test')
ON CONFLICT DO NOTHING;

-- 4. Audiences (+es overlay on Adult).
INSERT INTO audience.audiences
    (id, tour_operator_id, name, pax_per_unit, created_by, created_at)
VALUES
    (:'audience_adult_id', :'operator_id', 'Adult', 1, :'user_id', NOW()),
    (:'audience_child_id', :'operator_id', 'Child', 1, :'user_id', NOW())
ON CONFLICT DO NOTHING;

INSERT INTO audience.audience_translations
    (audience_id, tour_operator_id, locale, name)
VALUES
    (:'audience_adult_id', :'operator_id', 'es', 'Adulto'),
    (:'audience_child_id', :'operator_id', 'es', 'Niño')
ON CONFLICT DO NOTHING;

-- 5. Pickup locations (standalone catalog — no slot relationship by design).
INSERT INTO pickup.pickup_locations
    (id, tour_operator_id, created_by, name, "time", created_at)
VALUES
    (:'pickup_port_id',   :'operator_id', :'user_id', 'Old Port',      TIME '09:30', NOW()),
    (:'pickup_marina_id', :'operator_id', :'user_id', 'Marina Gate B', TIME '08:45', NOW())
ON CONFLICT DO NOTHING;

-- 6. Experiences: PUBLISHED so lists/pickers have real rows. media_ids stay
-- empty (see header — no seeded media). +es overlay on the sunset sail.
INSERT INTO experience.experiences
    (id, tour_operator_id, created_by, slug, name, description, long_description,
     featured, tags, included, not_included, highlights,
     duration_minutes, booking_cutoff_hours, published, created_at)
VALUES
    (:'experience_a_id', :'operator_id', :'user_id', 'sunset-sailing-tour',
     'Sunset Sailing Tour', 'Golden-hour cruise along the coast with a local skipper.',
     'Board at the old port and glide past the cliffs as the sun drops. Includes a drink on board and a stop for a swim when the sea allows.',
     TRUE, '{boat,sunset}', '{"Skipper","Drink on board"}', '{"Hotel pickup"}',
     '{"Golden-hour light","Small groups"}',
     150, 12, TRUE, NOW()),
    (:'experience_b_id', :'operator_id', :'user_id', 'old-town-food-walk',
     'Old Town Food Walk', 'Tastings across the historic quarter with a local guide.',
     'Five stops, seven tastings: market stalls, a century-old bakery, and the bar the guides go to after work.',
     FALSE, '{food,walking}', '{"All tastings","Local guide"}', '{"Extra drinks"}',
     '{"Seven tastings","Hidden courtyards"}',
     180, 24, TRUE, NOW()),
    (:'experience_c_id', :'operator_id', :'user_id', 'kayak-cave-adventure',
     'Kayak Cave Adventure', 'Paddle into sea caves only reachable from the water.',
     'A guided paddle along the coast with a stop inside the blue cave. No experience needed; doubles available.',
     FALSE, '{water,adventure}', '{"Kayak & paddle","Dry bag"}', '{"Photos"}',
     '{"The blue cave","Beginner friendly"}',
     120, 6, TRUE, NOW())
ON CONFLICT DO NOTHING;

INSERT INTO experience.experience_translations
    (experience_id, tour_operator_id, locale, name, description, long_description,
     highlights, included, not_included, slug)
VALUES
    (:'experience_a_id', :'operator_id', 'es', 'Paseo en velero al atardecer',
     'Crucero a la hora dorada por la costa con patrón local.', NULL,
     NULL, NULL, NULL, 'paseo-velero-atardecer')
ON CONFLICT DO NOTHING;

-- 7. Slots: two per experience, dated relative to today (operator-local wall
-- clock, no tz), day = 0–6 Sunday-first derived from the date, AVAILABLE.
-- Snapshot name/description mirror the parent (the propagation invariant).
INSERT INTO experience.slots
    (id, experience_id, tour_operator_id, start_at, end_at, day,
     experience_name, experience_description, status, created_at)
VALUES
    (:'slot_a1_id', :'experience_a_id', :'operator_id',
     (CURRENT_DATE + 3) + TIME '18:00', (CURRENT_DATE + 3) + TIME '20:30',
     EXTRACT(DOW FROM CURRENT_DATE + 3)::int,
     'Sunset Sailing Tour', 'Golden-hour cruise along the coast with a local skipper.',
     'AVAILABLE', NOW()),
    (:'slot_a2_id', :'experience_a_id', :'operator_id',
     (CURRENT_DATE + 10) + TIME '18:00', (CURRENT_DATE + 10) + TIME '20:30',
     EXTRACT(DOW FROM CURRENT_DATE + 10)::int,
     'Sunset Sailing Tour', 'Golden-hour cruise along the coast with a local skipper.',
     'AVAILABLE', NOW()),
    (:'slot_b1_id', :'experience_b_id', :'operator_id',
     (CURRENT_DATE + 4) + TIME '10:00', (CURRENT_DATE + 4) + TIME '13:00',
     EXTRACT(DOW FROM CURRENT_DATE + 4)::int,
     'Old Town Food Walk', 'Tastings across the historic quarter with a local guide.',
     'AVAILABLE', NOW()),
    (:'slot_b2_id', :'experience_b_id', :'operator_id',
     (CURRENT_DATE + 11) + TIME '10:00', (CURRENT_DATE + 11) + TIME '13:00',
     EXTRACT(DOW FROM CURRENT_DATE + 11)::int,
     'Old Town Food Walk', 'Tastings across the historic quarter with a local guide.',
     'AVAILABLE', NOW()),
    (:'slot_c1_id', :'experience_c_id', :'operator_id',
     (CURRENT_DATE + 5) + TIME '09:00', (CURRENT_DATE + 5) + TIME '11:00',
     EXTRACT(DOW FROM CURRENT_DATE + 5)::int,
     'Kayak Cave Adventure', 'Paddle into sea caves only reachable from the water.',
     'AVAILABLE', NOW()),
    (:'slot_c2_id', :'experience_c_id', :'operator_id',
     (CURRENT_DATE + 12) + TIME '09:00', (CURRENT_DATE + 12) + TIME '11:00',
     EXTRACT(DOW FROM CURRENT_DATE + 12)::int,
     'Kayak Cave Adventure', 'Paddle into sea caves only reachable from the water.',
     'AVAILABLE', NOW())
ON CONFLICT DO NOTHING;

-- Per-(slot, audience) pricing snapshots. One tier (a1 Adult) carries booked
-- seats so the capacity-floor 422 and the booked/capacity summaries are
-- exercisable without a checkout.
INSERT INTO experience.audience_slot
    (id, slot_id, audience_id, audience_name, price, capacity, pax_per_unit, booked_count)
VALUES
    (:'sap_a1_adult_id', :'slot_a1_id', :'audience_adult_id', 'Adult', 65.00, 20, 1, 12),
    (:'sap_a1_child_id', :'slot_a1_id', :'audience_child_id', 'Child', 35.00, 10, 1, 3),
    (:'sap_a2_adult_id', :'slot_a2_id', :'audience_adult_id', 'Adult', 65.00, 20, 1, 0),
    (:'sap_a2_child_id', :'slot_a2_id', :'audience_child_id', 'Child', 35.00, 10, 1, 0),
    (:'sap_b1_adult_id', :'slot_b1_id', :'audience_adult_id', 'Adult', 89.00, 12, 1, 0),
    (:'sap_b1_child_id', :'slot_b1_id', :'audience_child_id', 'Child', 55.00,  6, 1, 0),
    (:'sap_b2_adult_id', :'slot_b2_id', :'audience_adult_id', 'Adult', 89.00, 12, 1, 0),
    (:'sap_b2_child_id', :'slot_b2_id', :'audience_child_id', 'Child', 55.00,  6, 1, 0),
    (:'sap_c1_adult_id', :'slot_c1_id', :'audience_adult_id', 'Adult', 45.00, 16, 1, 0),
    (:'sap_c1_child_id', :'slot_c1_id', :'audience_child_id', 'Child', 30.00,  8, 1, 0),
    (:'sap_c2_adult_id', :'slot_c2_id', :'audience_adult_id', 'Adult', 45.00, 16, 1, 0),
    (:'sap_c2_child_id', :'slot_c2_id', :'audience_child_id', 'Child', 30.00,  8, 1, 0)
ON CONFLICT DO NOTHING;

-- 8. CMS pages: two published, one draft, +es overlay (with localized handle)
-- on About.
INSERT INTO page.pages
    (id, tour_operator_id, title, handle, body, seo_title, seo_description,
     status, template_suffix, created_by, created_at, updated_at)
VALUES
    (:'page_about_id', :'operator_id', 'About us', 'about-us',
     '<h1>Who we are</h1>' || E'\n' ||
     '<p>Family-run boat tours on the coast since 1998. Small groups, local skippers, no rush.</p>',
     'About our boat tours', 'Family-run boat tours on the coast since 1998.',
     'PUBLISHED', NULL, :'user_id', NOW(), NOW()),
    (:'page_contact_id', :'operator_id', 'Contact', 'contact',
     '<h1>Get in touch</h1>' || E'\n' ||
     '<p>Email <a href="mailto:hello@acme.test">hello@acme.test</a> or find us at the Old Port kiosk from 9:00.</p>',
     NULL, NULL,
     'PUBLISHED', NULL, :'user_id', NOW(), NOW()),
    (:'page_faq_id', :'operator_id', 'FAQ', 'faq',
     '<h1>Frequently asked questions</h1>' || E'\n' ||
     '<h2>What if it rains?</h2><p>We reschedule or refund — your pick.</p>',
     NULL, NULL,
     'DRAFT', NULL, :'user_id', NOW(), NOW())
ON CONFLICT DO NOTHING;

INSERT INTO page.page_translations
    (page_id, locale, tour_operator_id, slug, title, body, seo_title, seo_description)
VALUES
    (:'page_about_id', 'es', :'operator_id', 'sobre-nosotros', 'Sobre nosotros',
     '<h1>Quiénes somos</h1>' || E'\n' ||
     '<p>Paseos en barco familiares en la costa desde 1998.</p>',
     NULL, NULL)
ON CONFLICT DO NOTHING;

-- 9. Navigation menus. The two defaults every operator gets at creation
-- (seeded here because this operator is inserted raw, bypassing the use
-- case), with a small demo tree: main-menu = Home / Experiences / About
-- (+es overlay on Home), footer = Contact. The FAQ page is deliberately
-- NOT linked — it's DRAFT, which exercises the render side's unresolvable
-- handling later.
INSERT INTO touroperator.menus
    (id, tour_operator_id, handle, title, created_by, created_at, updated_at)
VALUES
    (:'menu_main_id',   :'operator_id', 'main-menu', 'Main menu', :'user_id', NOW(), NOW()),
    (:'menu_footer_id', :'operator_id', 'footer',    'Footer',    :'user_id', NOW(), NOW())
ON CONFLICT DO NOTHING;

-- Items resolve their menu by (operator, handle) instead of assuming the
-- fixed menu ids above landed: on a dev DB whose defaults came from the V5
-- backfill (random ids), the menus insert above no-ops on the handle
-- conflict, and a fixed menu_id here would abort the whole seed on the FK.
INSERT INTO touroperator.menu_items
    (id, menu_id, parent_id, title, link_type, resource_id, url, position, created_at, updated_at)
SELECT v.id, m.id, NULL, v.title, v.link_type, v.resource_id, NULL, v.position, NOW(), NOW()
FROM (VALUES
    (:'mi_home_id'::uuid,        'main-menu', 'Home',        'HOME',            NULL::uuid,             0),
    (:'mi_experiences_id'::uuid, 'main-menu', 'Experiences', 'EXPERIENCE_LIST', NULL::uuid,             1),
    (:'mi_about_id'::uuid,       'main-menu', 'About us',    'PAGE',            :'page_about_id'::uuid, 2),
    (:'mi_contact_id'::uuid,     'footer',    'Contact',     'PAGE',            :'page_contact_id'::uuid, 0)
) AS v(id, menu_handle, title, link_type, resource_id, position)
JOIN touroperator.menus m
    ON m.tour_operator_id = :'operator_id' AND m.handle = v.menu_handle
ON CONFLICT DO NOTHING;

INSERT INTO touroperator.menu_item_translations (menu_item_id, locale, title)
VALUES
    (:'mi_home_id', 'es', 'Inicio')
ON CONFLICT DO NOTHING;

-- 10. Contact-inbox messages (the admin Inbox's rows — the storefront intake
-- endpoint that will write these arrives with the storefront arc). Two
-- unread, one read.
INSERT INTO contact.contact_messages
    (id, tour_operator_id, name, email, summary, content, read_at, created_at)
VALUES
    (:'cm_sizes_id', :'operator_id', 'Laura Pérez', 'laura@example.com',
     'Do you have child seats on the sunset tour?',
     'Hi! We are a family of four (kids are 4 and 7). Do you provide child-size life vests and seats on the Sunset Sailing Tour? Thanks!',
     NULL, NOW() - INTERVAL '2 hours'),
    (:'cm_group_id', :'operator_id', 'Tom Baker', 'tom@example.org',
     'Group booking for 15 people',
     'Hello, I am organising a company outing in September for about 15 people. Can we book a private departure, and is there a group rate?',
     NULL, NOW() - INTERVAL '1 day'),
    (:'cm_gift_id', :'operator_id', NULL, 'ana@example.net',
     'Gift voucher?',
     'Do you sell gift vouchers for the kayak trip? I would like to give one to my sister for her birthday.',
     NOW() - INTERVAL '2 days', NOW() - INTERVAL '3 days')
ON CONFLICT DO NOTHING;
