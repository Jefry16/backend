-- Dev-only seed data. Applied by the docker-compose `seed` service after the
-- app's Flyway migrations finish. NOT a Flyway migration; never runs in
-- production. The method is the archive's: pure SQL (no cross-context Java),
-- idempotent, fail-loud.
--
-- Idempotent: every insert is ON CONFLICT DO NOTHING, so the service re-runs
-- on every `docker compose up` without duplicating rows. **DO NOTHING does not
-- converge**: change a value in this file and an existing database keeps the old
-- one, silently, while the seed still reports success. Rows whose values are
-- expected to be edited use ON CONFLICT ... DO UPDATE instead — see the tour
-- operator. Otherwise `docker compose down -v` is the only way to pick up a
-- change. Fixed UUIDs keep the
-- cross-row foreign keys stable across re-runs. ON_ERROR_STOP is set by
-- run.sh: schema drift (a renamed/dropped column) aborts the WHOLE seed
-- loudly instead of half-applying — if this file fails after a migration,
-- update it in the same PR (renames-must-update-dev-seed rule).
--
-- WHAT THIS IS FOR. Every admin screen has to be reachable with data in it. A
-- table with zero rows renders the same as a broken query, so a thin fixture
-- makes whole surfaces untestable — you cannot tell an empty inbox from an
-- inbox that failed to load. The rule for this file: every table the admin UI
-- reads gets rows, and every *state* a screen can show gets at least one row —
-- published and draft, read and unread, sold out and cancelled, past and
-- future, translated and not.
--
-- Seeds, per context:
--   identity      5 users (4 verified, 1 unverified), password `password`
--   touroperator  operator `acme` (primary locale `es`, + `en` `fr`), brand
--                 with palette/socials/images, all four legal policies,
--                 5 members across OWNER/ADMIN/STAFF, 5 invitations covering
--                 every status, 2 menus with a nested tree
--   media         14 image rows, each with a real MinIO object (see below)
--   audience      5 audiences incl. one multi-pax "Family pack"
--   pickup        4 pickup locations
--   experience    5 experiences (4 published, 1 draft), 16 slots spanning
--                 past/today/future and AVAILABLE/SOLD_OUT/CANCELLED, with
--                 per-audience pricing generated per slot
--   page          5 CMS pages (3 published, 2 draft, one with a template suffix)
--   metafield     8 metafield definitions, a `boat` metaobject with 4 fields
--                 and 3 entries, and values attached to experiences and pages
--   contact       12 inbox messages (9 unread, 3 read)
--   audit         27 activity entries across 13 entity types
--
-- MEDIA IS SEEDED, and the objects come with it. A media row whose MinIO object
-- is missing renders as a broken image on every screen that references it, so
-- for a long time this file seeded none. The fix is not to avoid media, it is to
-- ship the objects: `docker/dev-seed/media/` holds a placeholder PNG per row,
-- named for the exact key the upload use case would have produced
-- (`{mediaId}-{name}.png`), and the `minio-init` compose service uploads them
-- under `tour-operators/{operatorId}/` before the app starts. Row and object
-- agree by construction — if you add a media row here, add its file there.
--
-- AUDIT IS SEEDED, which is a change of position. The trail records real
-- actions, so seeded history is invented history — but the Activity screen is
-- unreviewable without it, and inventing a plausible trail is the lesser cost.
-- Treat these rows as fixture, never as evidence of anything.
--
-- The operator's primary locale is `es`, and the canonical rows are deliberately
-- still ENGLISH with `es` overlays on only SOME of them. That is not an
-- oversight: partial translation is the realistic case and the one where
-- fallback bugs hide. A fully-translated fixture would render identically
-- whether the overlay chain worked or not. So `es` exercises overlay-hit and
-- overlay-miss in the same page, `en` exercises the canonical fallback, and
-- `fr` is a supported locale with almost no overlay at all.
--
-- Login: admin@vointika.test / password (same password for every seeded user).

\set user_id            '01900000-0000-7000-8000-000000000001'
\set operator_id        '01900000-0000-7000-8000-000000000002'
\set member_id          '01900000-0000-7000-8000-000000000003'

\set audience_adult_id  '01900000-0000-7000-8000-000000000010'
\set audience_child_id  '01900000-0000-7000-8000-000000000011'
\set audience_infant_id '01900000-0000-7000-8000-000000000012'
\set audience_senior_id '01900000-0000-7000-8000-000000000013'
\set audience_family_id '01900000-0000-7000-8000-000000000014'

\set pickup_port_id     '01900000-0000-7000-8000-000000000015'
\set pickup_marina_id   '01900000-0000-7000-8000-000000000016'
\set pickup_hotel_id    '01900000-0000-7000-8000-000000000017'
\set pickup_station_id  '01900000-0000-7000-8000-000000000018'

\set experience_a_id    '01900000-0000-7000-8000-000000000020'
\set experience_b_id    '01900000-0000-7000-8000-000000000021'
\set experience_c_id    '01900000-0000-7000-8000-000000000022'
\set experience_d_id    '01900000-0000-7000-8000-000000000023'
\set experience_e_id    '01900000-0000-7000-8000-000000000024'

\set slot_a1_id         '01900000-0000-7000-8000-000000000030'
\set slot_a2_id         '01900000-0000-7000-8000-000000000031'
\set slot_b1_id         '01900000-0000-7000-8000-000000000032'
\set slot_b2_id         '01900000-0000-7000-8000-000000000033'
\set slot_c1_id         '01900000-0000-7000-8000-000000000034'
\set slot_c2_id         '01900000-0000-7000-8000-000000000035'
\set slot_a3_id         '01900000-0000-7000-8000-000000000036'
\set slot_a4_id         '01900000-0000-7000-8000-000000000037'
\set slot_a5_id         '01900000-0000-7000-8000-000000000038'
\set slot_a6_id         '01900000-0000-7000-8000-000000000039'
\set slot_a7_id         '01900000-0000-7000-8000-00000000003a'
\set slot_b3_id         '01900000-0000-7000-8000-00000000003b'
\set slot_b4_id         '01900000-0000-7000-8000-00000000003c'
\set slot_c3_id         '01900000-0000-7000-8000-00000000003d'
\set slot_c4_id         '01900000-0000-7000-8000-00000000003e'
\set slot_d1_id         '01900000-0000-7000-8000-00000000003f'

\set policy_cancel_id   '01900000-0000-7000-8000-000000000040'
\set policy_privacy_id  '01900000-0000-7000-8000-000000000041'
\set policy_terms_id    '01900000-0000-7000-8000-000000000042'
\set policy_legal_id    '01900000-0000-7000-8000-000000000043'

\set page_about_id      '01900000-0000-7000-8000-000000000050'
\set page_contact_id    '01900000-0000-7000-8000-000000000051'
\set page_faq_id        '01900000-0000-7000-8000-000000000052'
\set page_boats_id      '01900000-0000-7000-8000-000000000053'
\set page_press_id      '01900000-0000-7000-8000-000000000054'

\set menu_main_id       '01900000-0000-7000-8000-000000000060'
\set menu_footer_id     '01900000-0000-7000-8000-000000000061'
\set mi_home_id         '01900000-0000-7000-8000-000000000062'
\set mi_experiences_id  '01900000-0000-7000-8000-000000000063'
\set mi_about_id        '01900000-0000-7000-8000-000000000064'
\set mi_contact_id      '01900000-0000-7000-8000-000000000065'
\set mi_sunset_id       '01900000-0000-7000-8000-000000000066'
\set mi_kayak_id        '01900000-0000-7000-8000-000000000067'
\set mi_diving_id       '01900000-0000-7000-8000-000000000068'
\set mi_boats_id        '01900000-0000-7000-8000-000000000069'
\set mi_instagram_id    '01900000-0000-7000-8000-00000000006a'

\set cm_sizes_id        '01900000-0000-7000-8000-000000000070'
\set cm_group_id        '01900000-0000-7000-8000-000000000071'
\set cm_gift_id         '01900000-0000-7000-8000-000000000072'
\set cm_wheelchair_id   '01900000-0000-7000-8000-000000000073'
\set cm_lost_id         '01900000-0000-7000-8000-000000000074'
\set cm_weather_id      '01900000-0000-7000-8000-000000000075'
\set cm_invoice_id      '01900000-0000-7000-8000-000000000076'
\set cm_press_id        '01900000-0000-7000-8000-000000000077'
\set cm_dietary_id      '01900000-0000-7000-8000-000000000078'
\set cm_parking_id      '01900000-0000-7000-8000-000000000079'
\set cm_private_id      '01900000-0000-7000-8000-00000000007a'
\set cm_partner_id      '01900000-0000-7000-8000-00000000007b'
-- Never inserted: the audit trail's contact_message.deleted entry points here.
\set cm_deleted_id      '01900000-0000-7000-8000-00000000007c'

-- Media. The hex suffix here is also the filename prefix in
-- docker/dev-seed/media/ — they must not drift apart.
\set media_logo_id      '01900000-0000-7000-8000-000000000080'
\set media_logo_sq_id   '01900000-0000-7000-8000-000000000081'
\set media_favicon_id   '01900000-0000-7000-8000-000000000082'
\set media_cover_id     '01900000-0000-7000-8000-000000000083'
\set media_sunset1_id   '01900000-0000-7000-8000-000000000084'
\set media_sunset2_id   '01900000-0000-7000-8000-000000000085'
\set media_sunsetp_id   '01900000-0000-7000-8000-000000000086'
\set media_food1_id     '01900000-0000-7000-8000-000000000087'
\set media_food2_id     '01900000-0000-7000-8000-000000000088'
\set media_kayak1_id    '01900000-0000-7000-8000-000000000089'
\set media_kayak2_id    '01900000-0000-7000-8000-00000000008a'
\set media_diving_id    '01900000-0000-7000-8000-00000000008b'
\set media_crew_id      '01900000-0000-7000-8000-00000000008c'
\set media_og_id        '01900000-0000-7000-8000-00000000008d'

\set user_maria_id      '01900000-0000-7000-8000-000000000090'
\set user_diego_id      '01900000-0000-7000-8000-000000000091'
\set user_sofia_id      '01900000-0000-7000-8000-000000000092'
\set user_noa_id        '01900000-0000-7000-8000-000000000093'
\set member_maria_id    '01900000-0000-7000-8000-000000000094'
\set member_diego_id    '01900000-0000-7000-8000-000000000095'
\set member_sofia_id    '01900000-0000-7000-8000-000000000096'
\set member_noa_id      '01900000-0000-7000-8000-000000000097'

\set inv_pending_id     '01900000-0000-7000-8000-0000000000a0'
\set inv_expiring_id    '01900000-0000-7000-8000-0000000000a1'
\set inv_expired_id     '01900000-0000-7000-8000-0000000000a2'
\set inv_revoked_id     '01900000-0000-7000-8000-0000000000a3'
\set inv_accepted_id    '01900000-0000-7000-8000-0000000000a4'

\set mfd_difficulty_id  '01900000-0000-7000-8000-0000000000b0'
\set mfd_min_age_id     '01900000-0000-7000-8000-0000000000b1'
\set mfd_wetsuit_id     '01900000-0000-7000-8000-0000000000b2'
\set mfd_meetpoint_id   '01900000-0000-7000-8000-0000000000b3'
\set mfd_season_id      '01900000-0000-7000-8000-0000000000b4'
\set mfd_boat_id        '01900000-0000-7000-8000-0000000000b5'
\set mfd_subtitle_id    '01900000-0000-7000-8000-0000000000b6'
\set mfd_footer_id      '01900000-0000-7000-8000-0000000000b7'

\set mod_boat_id        '01900000-0000-7000-8000-0000000000d0'
\set mofd_name_id       '01900000-0000-7000-8000-0000000000d1'
\set mofd_capacity_id   '01900000-0000-7000-8000-0000000000d2'
\set mofd_year_id       '01900000-0000-7000-8000-0000000000d3'
\set mofd_notes_id      '01900000-0000-7000-8000-0000000000d4'
\set moe_swallow_id     '01900000-0000-7000-8000-0000000000d5'
\set moe_marlin_id      '01900000-0000-7000-8000-0000000000d6'
\set moe_gaffer_id      '01900000-0000-7000-8000-0000000000d7'

-- 1. Users. Password is the literal string "password" for all of them — the
-- hash is a fixed, precomputed BCrypt ($2a$, cost 10) digest accepted by
-- BCryptPasswordEncoder.matches(...).
--
-- Heads-up if you regenerate: do NOT reuse $2a$10$N9qo8uLOickgx2ZMRZoMy... —
-- that string is the SENTINEL_HASH inside the login use case (constant-time
-- guard when no user is found) and is NOT the hash of "password". Planting it
-- here would make every login attempt 401.
--
-- Noa is UNVERIFIED on purpose and is still a STAFF member: the roster has to
-- show a teammate who was invited, accepted, and never confirmed their address.
-- `language` varies so the admin-UI language column is not uniform.
INSERT INTO identity.users
    (id, email, name, hashed_password, status, language, created_at, updated_at)
VALUES
    (:'user_id', 'admin@vointika.test', 'Dev Admin',
     '$2a$10$g/AqLa2GMVbNRPyYas8h1e/.Gtfk8YsvHq8g0QTJrCZaRhmnRYr3m',
     'VERIFIED', 'en', NOW() - INTERVAL '400 days', NOW() - INTERVAL '400 days'),
    (:'user_maria_id', 'maria@acme.test', 'María Robles',
     '$2a$10$g/AqLa2GMVbNRPyYas8h1e/.Gtfk8YsvHq8g0QTJrCZaRhmnRYr3m',
     'VERIFIED', 'es', NOW() - INTERVAL '300 days', NOW() - INTERVAL '300 days'),
    (:'user_diego_id', 'diego@acme.test', 'Diego Santos',
     '$2a$10$g/AqLa2GMVbNRPyYas8h1e/.Gtfk8YsvHq8g0QTJrCZaRhmnRYr3m',
     'VERIFIED', 'es', NOW() - INTERVAL '120 days', NOW() - INTERVAL '120 days'),
    (:'user_sofia_id', 'sofia@acme.test', 'Sofía Marín',
     '$2a$10$g/AqLa2GMVbNRPyYas8h1e/.Gtfk8YsvHq8g0QTJrCZaRhmnRYr3m',
     'VERIFIED', 'en', NOW() - INTERVAL '40 days', NOW() - INTERVAL '40 days'),
    (:'user_noa_id', 'noa@acme.test', 'Noa Lindqvist',
     '$2a$10$g/AqLa2GMVbNRPyYas8h1e/.Gtfk8YsvHq8g0QTJrCZaRhmnRYr3m',
     'UNVERIFIED', 'en', NOW() - INTERVAL '6 days', NOW() - INTERVAL '6 days')
ON CONFLICT DO NOTHING;

-- 2. Tour operator. Timezone/currency resolve by name/code with a fallback to
-- the first reference row, so a changed reference set still seeds.
INSERT INTO touroperator.tour_operators
    (id, name, handle, timezone_id, currency_id, address, phone, email, primary_locale,
     seo_title, seo_description, og_image_media_id, created_by, created_at, updated_at)
VALUES
    (:'operator_id', 'Acme Tours', 'acme',
     COALESCE(
         (SELECT id FROM reference.timezones WHERE name = 'Europe/Madrid'),
         (SELECT id FROM reference.timezones ORDER BY name LIMIT 1)),
     COALESCE(
         (SELECT id FROM reference.currencies WHERE code = 'EUR'),
         (SELECT id FROM reference.currencies ORDER BY code LIMIT 1)),
     'Calle Mayor 1, 28013 Madrid',
     '+34 910 000 000', 'hola@acme.test', 'es',
     -- Canonical SEO is English like every other canonical row here; the `es`
     -- overlay below is what a default visit resolves to.
     'Acme Tours — Sailing and diving in Madrid',
     'Small-group sailing, diving and coastal day trips run out of Madrid since 2011.',
     :'media_og_id',
     :'user_id', NOW() - INTERVAL '400 days', NOW() - INTERVAL '9 days')
-- DO UPDATE, not DO NOTHING: this row's values get edited (the primary locale
-- has already changed once), and DO NOTHING would skip an existing operator
-- entirely — the seed would print "done" having applied nothing. Converge the
-- columns the seed owns; created_by/created_at stay as first written.
-- og_image_media_id is NOT converged, for the same reason the brand's image
-- columns are not: the admin writes it, and re-seeding must not undo that.
ON CONFLICT (id) DO UPDATE SET
    name           = EXCLUDED.name,
    handle           = EXCLUDED.handle,
    timezone_id    = EXCLUDED.timezone_id,
    currency_id    = EXCLUDED.currency_id,
    address         = EXCLUDED.address,
    phone           = EXCLUDED.phone,
    email           = EXCLUDED.email,
    primary_locale  = EXCLUDED.primary_locale,
    seo_title       = EXCLUDED.seo_title,
    seo_description = EXCLUDED.seo_description,
    updated_at     = NOW();

INSERT INTO touroperator.tour_operator_locales (tour_operator_id, locale)
VALUES
    (:'operator_id', 'es'),
    (:'operator_id', 'en'),
    (:'operator_id', 'fr')
ON CONFLICT DO NOTHING;

-- 2b. Operator translations. The `es` overlay is what a default visit resolves
-- to (primary locale is es), so this is the row that proves the shop-level
-- overlay is actually read rather than the canonical always winning.
-- password_message is left NULL: the gate is off, so a value here renders
-- nowhere and would be untestable fiction.
-- slogan is translated and short_description deliberately is NOT: one column per
-- page proves the overlay, the other proves the canonical fallback, in the same
-- request. `fr` carries a single column for the same reason at the other
-- extreme — an almost-empty overlay row is its own case.
INSERT INTO touroperator.tour_operator_translations
    (tour_operator_id, locale, seo_title, seo_description, password_message,
     slogan, short_description)
VALUES
    (:'operator_id', 'es',
     'Acme Tours — Vela y buceo en Madrid',
     'Salidas en velero, buceo y excursiones de un día en grupos pequeños desde Madrid, desde 2011.',
     NULL,
     'Navega la costa, no las multitudes.', NULL),
    (:'operator_id', 'fr',
     NULL, NULL, NULL,
     'Naviguez la côte, pas la foule.', NULL)
ON CONFLICT (tour_operator_id, locale) DO UPDATE SET
    seo_title         = EXCLUDED.seo_title,
    seo_description   = EXCLUDED.seo_description,
    password_message  = EXCLUDED.password_message,
    slogan            = EXCLUDED.slogan,
    short_description = EXCLUDED.short_description;

-- 3. Media. Inserted before everything that references a media id. There are no
-- foreign keys into this table — media is its own context and the owners hold
-- bare ids (PATTERNS §5) — so ordering here is for readability, not integrity.
--
-- size_bytes matches the real file on disk. width/height are the real pixel
-- dimensions, so the library's dimension column and any aspect-ratio handling
-- have honest values, including one portrait image among the landscapes.
-- `alt` is set on most rows and left NULL on two: the "missing alt" state is a
-- thing the media UI is supposed to surface.
INSERT INTO media.media
    (id, tour_operator_id, storage_key, content_type, size_bytes, original_name,
     alt, width, height, created_by, created_by_name, created_at)
VALUES
    (:'media_logo_id', :'operator_id',
     'tour-operators/' || :'operator_id' || '/' || :'media_logo_id' || '-acme-logo.png',
     'image/png', 3105, 'acme-logo.png', 'Acme Tours wordmark', 480, 160,
     :'user_id', 'Dev Admin', NOW() - INTERVAL '390 days'),
    (:'media_logo_sq_id', :'operator_id',
     'tour-operators/' || :'operator_id' || '/' || :'media_logo_sq_id' || '-acme-logo-square.png',
     'image/png', 6355, 'acme-logo-square.png', 'Acme Tours square mark', 512, 512,
     :'user_id', 'Dev Admin', NOW() - INTERVAL '390 days'),
    (:'media_favicon_id', :'operator_id',
     'tour-operators/' || :'operator_id' || '/' || :'media_favicon_id' || '-acme-favicon.png',
     'image/png', 676, 'acme-favicon.png', NULL, 64, 64,
     :'user_id', 'Dev Admin', NOW() - INTERVAL '390 days'),
    (:'media_cover_id', :'operator_id',
     'tour-operators/' || :'operator_id' || '/' || :'media_cover_id' || '-acme-cover.png',
     'image/png', 20849, 'acme-cover.png', 'Boats moored at the old port at dawn', 1600, 900,
     :'user_id', 'Dev Admin', NOW() - INTERVAL '388 days'),
    (:'media_sunset1_id', :'operator_id',
     'tour-operators/' || :'operator_id' || '/' || :'media_sunset1_id' || '-sunset-sail-1.png',
     'image/png', 21660, 'sunset-sail-1.png', 'Sailing boat against the setting sun', 1600, 1067,
     :'user_maria_id', 'María Robles', NOW() - INTERVAL '210 days'),
    (:'media_sunset2_id', :'operator_id',
     'tour-operators/' || :'operator_id' || '/' || :'media_sunset2_id' || '-sunset-sail-2.png',
     'image/png', 22472, 'sunset-sail-2.png', 'Guests on deck during the golden hour', 1600, 1067,
     :'user_maria_id', 'María Robles', NOW() - INTERVAL '210 days'),
    (:'media_sunsetp_id', :'operator_id',
     'tour-operators/' || :'operator_id' || '/' || :'media_sunsetp_id' || '-sunset-sail-portrait.png',
     'image/png', 21450, 'sunset-sail-portrait.png', 'The mainsail from below', 900, 1350,
     :'user_maria_id', 'María Robles', NOW() - INTERVAL '209 days'),
    (:'media_food1_id', :'operator_id',
     'tour-operators/' || :'operator_id' || '/' || :'media_food1_id' || '-food-walk-1.png',
     'image/png', 21358, 'food-walk-1.png', 'Market stall with cured meats', 1600, 1067,
     :'user_diego_id', 'Diego Santos', NOW() - INTERVAL '95 days'),
    (:'media_food2_id', :'operator_id',
     'tour-operators/' || :'operator_id' || '/' || :'media_food2_id' || '-food-walk-2.png',
     'image/png', 21724, 'food-walk-2.png', NULL, 1600, 1067,
     :'user_diego_id', 'Diego Santos', NOW() - INTERVAL '95 days'),
    (:'media_kayak1_id', :'operator_id',
     'tour-operators/' || :'operator_id' || '/' || :'media_kayak1_id' || '-kayak-cave-1.png',
     'image/png', 21855, 'kayak-cave-1.png', 'Kayaks at the mouth of the blue cave', 1600, 1067,
     :'user_diego_id', 'Diego Santos', NOW() - INTERVAL '80 days'),
    (:'media_kayak2_id', :'operator_id',
     'tour-operators/' || :'operator_id' || '/' || :'media_kayak2_id' || '-kayak-cave-2.png',
     'image/png', 22340, 'kayak-cave-2.png', 'Paddling along the cliffs', 1600, 1067,
     :'user_diego_id', 'Diego Santos', NOW() - INTERVAL '80 days'),
    (:'media_diving_id', :'operator_id',
     'tour-operators/' || :'operator_id' || '/' || :'media_diving_id' || '-diving-1.png',
     'image/png', 23795, 'diving-1.png', 'Diver over a seagrass meadow', 1600, 1067,
     :'user_sofia_id', 'Sofía Marín', NOW() - INTERVAL '30 days'),
    (:'media_crew_id', :'operator_id',
     'tour-operators/' || :'operator_id' || '/' || :'media_crew_id' || '-team-crew.png',
     'image/png', 14703, 'team-crew.png', 'The Acme crew on the quay', 1200, 800,
     :'user_sofia_id', 'Sofía Marín', NOW() - INTERVAL '22 days'),
    (:'media_og_id', :'operator_id',
     'tour-operators/' || :'operator_id' || '/' || :'media_og_id' || '-og-share-card.png',
     'image/png', 14294, 'og-share-card.png', 'Acme Tours share card', 1200, 630,
     :'user_id', 'Dev Admin', NOW() - INTERVAL '9 days')
ON CONFLICT DO NOTHING;

-- 4. Brand. Without it the whole brand leg is unverifiable against a running
-- stack — the lesson #88 and #92 both had to report.
--
-- The image columns are set on INSERT so a fresh database has a rendered logo,
-- and are deliberately left OUT of the DO UPDATE: the admin PUT .../logo writes
-- them, and re-seeding must not undo an operator's logo.
INSERT INTO touroperator.tour_operator_brand
    (tour_operator_id, slogan, short_description,
     logo_media_id, square_logo_media_id, favicon_media_id, cover_image_media_id,
     created_at, updated_at)
VALUES
    (:'operator_id', 'Sail the coast, not the crowds.',
     'Small-group sailing, diving and coastal day trips out of Madrid since 2011.',
     :'media_logo_id', :'media_logo_sq_id', :'media_favicon_id', :'media_cover_id',
     NOW() - INTERVAL '400 days', NOW() - INTERVAL '9 days')
ON CONFLICT (tour_operator_id) DO UPDATE SET
    slogan            = EXCLUDED.slogan,
    short_description = EXCLUDED.short_description,
    updated_at        = NOW();

-- Two primaries and one secondary. PRIMARY position 1 is inserted BEFORE
-- position 0 on purpose: Postgres returns unordered rows in roughly heap order,
-- so a palette read without ORDER BY position comes back visibly wrong here
-- rather than accidentally right.
INSERT INTO touroperator.tour_operator_brand_colors
    (tour_operator_id, role, position, background, foreground)
VALUES
    (:'operator_id', 'PRIMARY',   1, '#1c7ba8', '#ffffff'),
    (:'operator_id', 'PRIMARY',   0, '#0b3d5c', '#ffffff'),
    (:'operator_id', 'SECONDARY', 1, '#f6d9a8', '#3a2c12'),
    (:'operator_id', 'SECONDARY', 0, '#f2a541', '#1a1a1a')
ON CONFLICT (tour_operator_id, role, position) DO UPDATE SET
    background = EXCLUDED.background,
    foreground = EXCLUDED.foreground;

INSERT INTO touroperator.tour_operator_brand_social_links
    (tour_operator_id, platform, url)
VALUES
    (:'operator_id', 'INSTAGRAM',   'https://instagram.com/acmetours'),
    (:'operator_id', 'FACEBOOK',    'https://facebook.com/acmetours'),
    (:'operator_id', 'YOUTUBE',     'https://youtube.com/@acmetours'),
    (:'operator_id', 'TRIPADVISOR', 'https://tripadvisor.com/acmetours'),
    (:'operator_id', 'WHATSAPP',    'https://wa.me/34910000000')
ON CONFLICT (tour_operator_id, platform) DO UPDATE SET
    url = EXCLUDED.url;

-- 5. Policies. All four types, so the footer lists a full set and the admin
-- list has one row per type. TERMS is deliberately the thin one — an operator
-- who pasted two sentences and moved on is the ordinary case.
--
-- The bodies are real HTML — headings, a list, a link — because that is the
-- whole point of the rendering decision: the storefront renders these unescaped,
-- and a plain-text fixture could not tell a working page from an escaped one.
-- `id` is required since V13, which gave policies a surrogate primary key so the
-- admin list could go through the shared cursor framework. (tour_operator_id,
-- type) is still UNIQUE, so the ON CONFLICT below is unchanged.
INSERT INTO touroperator.tour_operator_policies
    (id, tour_operator_id, type, title, body, created_at, updated_at)
VALUES
    (:'policy_cancel_id', :'operator_id', 'CANCELLATION', 'Cancellation policy',
     '<h2>Free cancellation</h2>' ||
     '<p>Cancel up to 48 hours before departure for a full refund.</p>' ||
     '<ul><li>48 hours or more: full refund.</li>' ||
     '<li>24 to 48 hours: 50% refund.</li>' ||
     '<li>Less than 24 hours: no refund.</li></ul>' ||
     '<p>Weather cancellations are always refunded in full. Write to ' ||
     '<a href="mailto:hola@acme.test">hola@acme.test</a> and we will sort it out.</p>',
     NOW() - INTERVAL '380 days', NOW() - INTERVAL '30 days'),
    (:'policy_privacy_id', :'operator_id', 'PRIVACY', 'Privacy policy',
     '<h2>What we collect</h2>' ||
     '<p>Your name, email and phone number, so that we can run your booking.</p>' ||
     '<h2>What we do with it</h2>' ||
     '<ul><li>Confirm and change your departure.</li>' ||
     '<li>Reach you if the weather turns.</li></ul>' ||
     '<p>We never sell it. See the <a href="/policies/cancellation">cancellation policy</a> ' ||
     'for how refunds work.</p>',
     NOW() - INTERVAL '380 days', NOW() - INTERVAL '380 days'),
    (:'policy_terms_id', :'operator_id', 'TERMS', 'Terms and conditions',
     '<p>Booking a departure means you accept these terms and the ' ||
     '<a href="/policies/cancellation">cancellation policy</a>.</p>' ||
     '<p>Minimum age and swimming ability vary by experience; check the ' ||
     'experience page before you book.</p>',
     NOW() - INTERVAL '200 days', NOW() - INTERVAL '200 days'),
    (:'policy_legal_id', :'operator_id', 'LEGAL_NOTICE', 'Legal notice',
     '<h2>Acme Tours S.L.</h2>' ||
     '<p>Calle Mayor 1, 28013 Madrid, Spain.<br>VAT ESB00000000.</p>' ||
     '<p>Registered in the Registro Mercantil de Madrid, tomo 1, folio 1.</p>' ||
     '<p>Contact: <a href="mailto:hola@acme.test">hola@acme.test</a> · +34 910 000 000</p>',
     NOW() - INTERVAL '200 days', NOW() - INTERVAL '200 days')
ON CONFLICT (tour_operator_id, type) DO UPDATE SET
    title      = EXCLUDED.title,
    body       = EXCLUDED.body,
    updated_at = NOW();

-- Two of the four carry an `es` overlay, so the policy list shows both the
-- translated and the untranslated state side by side.
INSERT INTO touroperator.tour_operator_policy_translations
    (tour_operator_id, type, locale, title, body)
VALUES
    (:'operator_id', 'CANCELLATION', 'es', 'Política de cancelación',
     '<h2>Cancelación gratuita</h2>' ||
     '<p>Cancela hasta 48 horas antes de la salida y te devolvemos el importe íntegro.</p>' ||
     '<ul><li>48 horas o más: reembolso completo.</li>' ||
     '<li>Entre 24 y 48 horas: 50%.</li>' ||
     '<li>Menos de 24 horas: sin reembolso.</li></ul>' ||
     '<p>Si cancelamos por mal tiempo, el reembolso es siempre completo. Escríbenos a ' ||
     '<a href="mailto:hola@acme.test">hola@acme.test</a>.</p>'),
    (:'operator_id', 'PRIVACY', 'es', 'Política de privacidad',
     '<h2>Qué recogemos</h2>' ||
     '<p>Tu nombre, correo y teléfono, para poder gestionar tu reserva.</p>' ||
     '<h2>Qué hacemos con ello</h2>' ||
     '<ul><li>Confirmar y modificar tu salida.</li>' ||
     '<li>Avisarte si cambia el tiempo.</li></ul>' ||
     '<p>Nunca lo vendemos.</p>')
ON CONFLICT (tour_operator_id, type, locale) DO UPDATE SET
    title = EXCLUDED.title,
    body  = EXCLUDED.body;

-- 6. Members (name/email denormalized per the roster model). One of each role,
-- plus the unverified teammate, so the roster's role column, the role-gate
-- behaviour and the "invited but never confirmed" state all have a row.
INSERT INTO touroperator.tour_operator_members
    (id, tour_operator_id, user_id, role, is_default, joined_at, name, email)
VALUES
    (:'member_id',       :'operator_id', :'user_id',       'OWNER', TRUE,
     NOW() - INTERVAL '400 days', 'Dev Admin',      'admin@vointika.test'),
    (:'member_maria_id', :'operator_id', :'user_maria_id', 'ADMIN', TRUE,
     NOW() - INTERVAL '300 days', 'María Robles',   'maria@acme.test'),
    (:'member_diego_id', :'operator_id', :'user_diego_id', 'STAFF', TRUE,
     NOW() - INTERVAL '120 days', 'Diego Santos',   'diego@acme.test'),
    (:'member_sofia_id', :'operator_id', :'user_sofia_id', 'STAFF', TRUE,
     NOW() - INTERVAL '40 days',  'Sofía Marín',    'sofia@acme.test'),
    (:'member_noa_id',   :'operator_id', :'user_noa_id',   'STAFF', TRUE,
     NOW() - INTERVAL '6 days',   'Noa Lindqvist',  'noa@acme.test')
ON CONFLICT DO NOTHING;

-- 7. Invitations, one per status. The token hashes are arbitrary fixed strings:
-- they are unique (a UNIQUE constraint) and never verified against a real token
-- here, so nothing can be *accepted* from this fixture — these exist so the
-- invitations list renders every state, including the two that only differ by
-- date (PENDING that is fine vs PENDING that expires tomorrow).
INSERT INTO touroperator.tour_operator_invitations
    (id, tour_operator_id, email, name, role, token_hash, status,
     invited_by_user_id, invited_by_name, created_at, expires_at, accepted_at)
VALUES
    (:'inv_pending_id', :'operator_id', 'pilar@example.com', 'Pilar Nieto', 'STAFF',
     'seed-hash-pending-0000000000000000000000000000', 'PENDING',
     :'user_id', 'Dev Admin', NOW() - INTERVAL '2 days', NOW() + INTERVAL '5 days', NULL),
    (:'inv_expiring_id', :'operator_id', 'kwame@example.com', 'Kwame Mensah', 'ADMIN',
     'seed-hash-expiring-000000000000000000000000000', 'PENDING',
     :'user_maria_id', 'María Robles', NOW() - INTERVAL '6 days', NOW() + INTERVAL '1 day', NULL),
    (:'inv_expired_id', :'operator_id', 'lea@example.com', 'Léa Fontaine', 'STAFF',
     'seed-hash-expired-0000000000000000000000000000', 'EXPIRED',
     :'user_id', 'Dev Admin', NOW() - INTERVAL '20 days', NOW() - INTERVAL '13 days', NULL),
    (:'inv_revoked_id', :'operator_id', 'oskar@example.com', 'Oskar Bauer', 'STAFF',
     'seed-hash-revoked-0000000000000000000000000000', 'REVOKED',
     :'user_maria_id', 'María Robles', NOW() - INTERVAL '15 days', NOW() - INTERVAL '8 days', NULL),
    (:'inv_accepted_id', :'operator_id', 'sofia@acme.test', 'Sofía Marín', 'STAFF',
     'seed-hash-accepted-000000000000000000000000000', 'ACCEPTED',
     :'user_id', 'Dev Admin', NOW() - INTERVAL '43 days', NOW() - INTERVAL '36 days',
     NOW() - INTERVAL '40 days')
ON CONFLICT DO NOTHING;

-- 8. Audiences (+es overlay on the two originals). "Family pack" is the only
-- one with pax_per_unit > 1: without it nothing exercises the difference
-- between units sold and seats consumed.
INSERT INTO audience.audiences
    (id, tour_operator_id, name, pax_per_unit, created_by, created_at)
VALUES
    (:'audience_adult_id',  :'operator_id', 'Adult',       1, :'user_id', NOW() - INTERVAL '390 days'),
    (:'audience_child_id',  :'operator_id', 'Child',       1, :'user_id', NOW() - INTERVAL '390 days'),
    (:'audience_infant_id', :'operator_id', 'Infant',      1, :'user_maria_id', NOW() - INTERVAL '150 days'),
    (:'audience_senior_id', :'operator_id', 'Senior',      1, :'user_maria_id', NOW() - INTERVAL '150 days'),
    (:'audience_family_id', :'operator_id', 'Family pack', 4, :'user_maria_id', NOW() - INTERVAL '60 days')
ON CONFLICT DO NOTHING;

INSERT INTO audience.audience_translations
    (audience_id, tour_operator_id, locale, name)
VALUES
    (:'audience_adult_id',  :'operator_id', 'es', 'Adulto'),
    (:'audience_child_id',  :'operator_id', 'es', 'Niño'),
    (:'audience_senior_id', :'operator_id', 'es', 'Sénior'),
    (:'audience_family_id', :'operator_id', 'es', 'Pack familiar')
ON CONFLICT DO NOTHING;

-- 9. Pickup locations (standalone catalog — no slot relationship by design).
INSERT INTO pickup.pickup_locations
    (id, tour_operator_id, created_by, name, "time", created_at)
VALUES
    (:'pickup_port_id',    :'operator_id', :'user_id',       'Old Port',        TIME '09:30', NOW() - INTERVAL '380 days'),
    (:'pickup_marina_id',  :'operator_id', :'user_id',       'Marina Gate B',   TIME '08:45', NOW() - INTERVAL '380 days'),
    (:'pickup_hotel_id',   :'operator_id', :'user_maria_id', 'Hotel Sol lobby', TIME '07:50', NOW() - INTERVAL '140 days'),
    (:'pickup_station_id', :'operator_id', :'user_diego_id', 'Atocha station',  TIME '07:15', NOW() - INTERVAL '70 days')
ON CONFLICT DO NOTHING;

-- 10. Experiences. Four published and one draft, so the list has both states and
-- the storefront has something to hide. Galleries and thumbnails point at the
-- seeded media; the draft has none, which is what a half-written experience
-- looks like. starting_price is set explicitly rather than left at its 0
-- default — a price of 0 reads as free, not as unset.
INSERT INTO experience.experiences
    (id, tour_operator_id, created_by, handle, name, description, long_description,
     featured, tags, included, not_included, highlights,
     media_ids, thumbnail_media_id, duration_minutes, booking_cutoff_hours,
     published, starting_price, seo_title, seo_description, created_at)
VALUES
    (:'experience_a_id', :'operator_id', :'user_id', 'sunset-sailing-tour',
     'Sunset Sailing Tour', 'Golden-hour cruise along the coast with a local skipper.',
     'Board at the old port and glide past the cliffs as the sun drops. Includes a drink on board and a stop for a swim when the sea allows.',
     TRUE, '{boat,sunset,sailing}', '{"Skipper","Drink on board","Swim stop"}', '{"Hotel pickup","Dinner"}',
     '{"Golden-hour light","Small groups","Swim stop when the sea allows"}',
     ARRAY[:'media_sunset1_id', :'media_sunset2_id', :'media_sunsetp_id']::uuid[],
     :'media_sunset1_id', 150, 12, TRUE, 35.00,
     'Sunset sailing in Madrid — small groups',
     'A two-and-a-half hour golden-hour sail with a local skipper. Drinks on board, swim stop when the sea allows.',
     NOW() - INTERVAL '380 days'),
    (:'experience_b_id', :'operator_id', :'user_id', 'old-town-food-walk',
     'Old Town Food Walk', 'Tastings across the historic quarter with a local guide.',
     'Five stops, seven tastings: market stalls, a century-old bakery, and the bar the guides go to after work.',
     FALSE, '{food,walking}', '{"All tastings","Local guide"}', '{"Extra drinks"}',
     '{"Seven tastings","Hidden courtyards"}',
     ARRAY[:'media_food1_id', :'media_food2_id']::uuid[],
     :'media_food1_id', 180, 24, TRUE, 55.00,
     NULL, NULL,
     NOW() - INTERVAL '300 days'),
    (:'experience_c_id', :'operator_id', :'user_id', 'kayak-cave-adventure',
     'Kayak Cave Adventure', 'Paddle into sea caves only reachable from the water.',
     'A guided paddle along the coast with a stop inside the blue cave. No experience needed; doubles available.',
     TRUE, '{water,adventure,kayak}', '{"Kayak & paddle","Dry bag","Guide"}', '{"Photos","Wetsuit"}',
     '{"The blue cave","Beginner friendly"}',
     ARRAY[:'media_kayak1_id', :'media_kayak2_id']::uuid[],
     :'media_kayak1_id', 120, 6, TRUE, 0.00,
     'Kayak the blue cave — no experience needed',
     'A guided two-hour paddle to sea caves reachable only from the water. Doubles available.',
     -- Same created_at as the sunset sail, to the microsecond, and both are
     -- featured. The storefront listing orders featured DESC, created_at, id —
     -- so these two are separated by the id tie-break and nothing else. Give
     -- them different dates and the fixture stops exercising it, and a listing
     -- that reorders between requests would render correctly here anyway.
     NOW() - INTERVAL '380 days'),
    (:'experience_d_id', :'operator_id', :'user_sofia_id', 'blue-cave-diving',
     'Blue Cave Diving', 'A guided two-tank dive on the cave wall.',
     'For certified divers. Two tanks, a wall dive and a swim-through, with a surface interval on the boat.',
     FALSE, '{diving,water,certified}', '{"Two tanks","Guide","Weights"}', '{"Certification course","Wetsuit rental"}',
     '{"Wall dive and swim-through","Certified divers only"}',
     ARRAY[:'media_diving_id']::uuid[],
     :'media_diving_id', 240, 48, TRUE, 110.00,
     NULL, NULL,
     NOW() - INTERVAL '30 days'),
    (:'experience_e_id', :'operator_id', :'user_sofia_id', 'winter-whale-watching',
     'Winter Whale Watching', 'Half-day offshore trip in the migration season.',
     'Still being written — sailing dates and pricing are not final.',
     FALSE, '{boat,wildlife,seasonal}', '{}', '{}', '{}',
     '{}'::uuid[], NULL, 300, 48, FALSE, 0.00,
     NULL, NULL,
     NOW() - INTERVAL '4 days')
ON CONFLICT DO NOTHING;

-- Two of five carry an `es` overlay, and only one of those localizes its handle
-- — so the storefront resolves a translated slug on one experience and falls
-- back to the canonical slug on the other, in the same list.
INSERT INTO experience.experience_translations
    (experience_id, tour_operator_id, locale, name, description, long_description,
     highlights, included, not_included, handle, seo_title, seo_description)
VALUES
    (:'experience_a_id', :'operator_id', 'es', 'Paseo en velero al atardecer',
     'Crucero a la hora dorada por la costa con patrón local.',
     'Embarca en el puerto viejo y navega junto a los acantilados mientras cae el sol. Incluye una bebida a bordo y una parada para nadar cuando el mar lo permite.',
     '{"Luz de la hora dorada","Grupos pequeños"}', '{"Patrón","Bebida a bordo"}', NULL,
     'paseo-velero-atardecer',
     'Paseo en velero al atardecer en Madrid', NULL),
    (:'experience_c_id', :'operator_id', 'es', 'Aventura en kayak por las cuevas',
     'Rema hasta cuevas marinas a las que solo se llega por el agua.', NULL,
     NULL, NULL, NULL, NULL, NULL, NULL)
ON CONFLICT DO NOTHING;

-- 11. Slots: sixteen, dated relative to today (operator-local wall clock, no
-- tz), day = 0–6 Sunday-first derived from the date. Snapshot
-- name/description mirror the parent (the propagation invariant).
--
-- The spread is the point. Past, today and future all appear, and so do all
-- three statuses — a calendar that only ever holds future AVAILABLE slots
-- cannot show you what a sold-out or cancelled departure looks like. Two
-- experiences have a second departure at a different time of day so the day
-- view has more than one row. The draft experience has no slots at all.
INSERT INTO experience.slots
    (id, experience_id, tour_operator_id, start_at, end_at, day,
     experience_name, experience_description, status, created_at)
SELECT v.id, v.experience_id, :'operator_id',
       (CURRENT_DATE + v.day_offset) + v.start_time,
       (CURRENT_DATE + v.day_offset) + v.end_time,
       EXTRACT(DOW FROM CURRENT_DATE + v.day_offset)::int,
       e.name, e.description, v.status, NOW() - INTERVAL '20 days'
FROM (VALUES
    -- Sunset Sailing
    (:'slot_a3_id'::uuid, :'experience_a_id'::uuid, -7, TIME '18:00', TIME '20:30', 'AVAILABLE'),
    (:'slot_a4_id'::uuid, :'experience_a_id'::uuid, -2, TIME '18:00', TIME '20:30', 'AVAILABLE'),
    (:'slot_a5_id'::uuid, :'experience_a_id'::uuid,  0, TIME '18:00', TIME '20:30', 'AVAILABLE'),
    (:'slot_a1_id'::uuid, :'experience_a_id'::uuid,  3, TIME '18:00', TIME '20:30', 'AVAILABLE'),
    (:'slot_a2_id'::uuid, :'experience_a_id'::uuid, 10, TIME '18:00', TIME '20:30', 'AVAILABLE'),
    (:'slot_a6_id'::uuid, :'experience_a_id'::uuid, 17, TIME '18:00', TIME '20:30', 'SOLD_OUT'),
    (:'slot_a7_id'::uuid, :'experience_a_id'::uuid, 24, TIME '18:00', TIME '20:30', 'CANCELLED'),
    -- Old Town Food Walk
    (:'slot_b3_id'::uuid, :'experience_b_id'::uuid, -5, TIME '10:00', TIME '13:00', 'AVAILABLE'),
    (:'slot_b1_id'::uuid, :'experience_b_id'::uuid,  4, TIME '10:00', TIME '13:00', 'AVAILABLE'),
    (:'slot_b2_id'::uuid, :'experience_b_id'::uuid, 11, TIME '10:00', TIME '13:00', 'AVAILABLE'),
    (:'slot_b4_id'::uuid, :'experience_b_id'::uuid, 18, TIME '10:00', TIME '13:00', 'AVAILABLE'),
    -- Kayak Cave Adventure (two departures in the same week)
    (:'slot_c1_id'::uuid, :'experience_c_id'::uuid,  5, TIME '09:00', TIME '11:00', 'AVAILABLE'),
    (:'slot_c3_id'::uuid, :'experience_c_id'::uuid,  6, TIME '14:30', TIME '16:30', 'AVAILABLE'),
    (:'slot_c2_id'::uuid, :'experience_c_id'::uuid, 12, TIME '09:00', TIME '11:00', 'AVAILABLE'),
    (:'slot_c4_id'::uuid, :'experience_c_id'::uuid, 19, TIME '09:00', TIME '11:00', 'SOLD_OUT'),
    -- Blue Cave Diving
    (:'slot_d1_id'::uuid, :'experience_d_id'::uuid,  7, TIME '08:00', TIME '12:00', 'AVAILABLE')
) AS v(id, experience_id, day_offset, start_time, end_time, status)
JOIN experience.experiences e ON e.id = v.experience_id
ON CONFLICT DO NOTHING;

-- Per-(slot, audience) pricing snapshots, generated rather than listed: sixteen
-- slots times two-to-three audience tiers is fifty-odd rows, and hand-numbering
-- them buys nothing — nothing references an audience_slot id, and the table is
-- keyed by (slot_id, audience_id) anyway. The id is derived with md5 so it is
-- still deterministic across re-runs, which is the only property the fixed-UUID
-- convention was protecting.
--
-- Tiers differ per experience (diving has no child tier; only the sunset sail
-- sells the multi-pax family pack), and `fill` sets how full each departure is:
-- 1.0 on the SOLD_OUT and past departures, 0 on the far-future ones, partial in
-- between. That is what makes the capacity floor, the booked/capacity summaries
-- and the sold-out badge all exercisable without a checkout.
WITH tier(experience_id, audience_id, audience_name, price, capacity, pax_per_unit) AS (
    VALUES
        (:'experience_a_id'::uuid, :'audience_adult_id'::uuid,  'Adult',        65.00, 20, 1),
        (:'experience_a_id'::uuid, :'audience_child_id'::uuid,  'Child',        35.00, 10, 1),
        (:'experience_a_id'::uuid, :'audience_family_id'::uuid, 'Family pack', 220.00,  5, 4),
        (:'experience_b_id'::uuid, :'audience_adult_id'::uuid,  'Adult',        89.00, 12, 1),
        (:'experience_b_id'::uuid, :'audience_child_id'::uuid,  'Child',        55.00,  6, 1),
        (:'experience_b_id'::uuid, :'audience_senior_id'::uuid, 'Senior',       75.00,  6, 1),
        (:'experience_c_id'::uuid, :'audience_adult_id'::uuid,  'Adult',        45.00, 16, 1),
        (:'experience_c_id'::uuid, :'audience_child_id'::uuid,  'Child',        30.00,  8, 1),
        (:'experience_c_id'::uuid, :'audience_infant_id'::uuid, 'Infant',        0.00,  4, 1),
        (:'experience_d_id'::uuid, :'audience_adult_id'::uuid,  'Adult',       120.00,  8, 1),
        (:'experience_d_id'::uuid, :'audience_senior_id'::uuid, 'Senior',      110.00,  4, 1)
), fill(slot_id, factor) AS (
    VALUES
        (:'slot_a3_id'::uuid, 1.00), (:'slot_a4_id'::uuid, 0.85), (:'slot_a5_id'::uuid, 0.90),
        (:'slot_a1_id'::uuid, 0.60), (:'slot_a2_id'::uuid, 0.00), (:'slot_a6_id'::uuid, 1.00),
        (:'slot_a7_id'::uuid, 0.30),
        (:'slot_b3_id'::uuid, 1.00), (:'slot_b1_id'::uuid, 0.25), (:'slot_b2_id'::uuid, 0.00),
        (:'slot_b4_id'::uuid, 0.50),
        (:'slot_c1_id'::uuid, 0.10), (:'slot_c3_id'::uuid, 0.75), (:'slot_c2_id'::uuid, 0.00),
        (:'slot_c4_id'::uuid, 1.00),
        (:'slot_d1_id'::uuid, 0.40)
)
INSERT INTO experience.audience_slot
    (id, slot_id, audience_id, audience_name, price, capacity, pax_per_unit, booked_count)
SELECT md5('audience_slot:' || s.id || ':' || t.audience_id)::uuid,
       s.id, t.audience_id, t.audience_name, t.price, t.capacity, t.pax_per_unit,
       LEAST(t.capacity, FLOOR(t.capacity * f.factor)::int)
FROM experience.slots s
JOIN tier t ON t.experience_id = s.experience_id
JOIN fill f ON f.slot_id = s.id
ON CONFLICT (slot_id, audience_id) DO NOTHING;

-- 12. CMS pages: three published, two draft, +es overlay (with localized
-- handle) on About. "Our boats" carries a template_suffix so the alternate-
-- template path has a row; every other page leaves it NULL.
INSERT INTO page.pages
    (id, tour_operator_id, title, handle, body, seo_title, seo_description,
     status, template_suffix, created_by, created_at, updated_at)
VALUES
    (:'page_about_id', :'operator_id', 'About us', 'about-us',
     '<h1>Who we are</h1>' || E'\n' ||
     '<p>Family-run boat tours on the coast since 1998. Small groups, local skippers, no rush.</p>',
     'About our boat tours', 'Family-run boat tours on the coast since 1998.',
     'PUBLISHED', NULL, :'user_id', NOW() - INTERVAL '370 days', NOW() - INTERVAL '60 days'),
    (:'page_contact_id', :'operator_id', 'Contact', 'contact',
     '<h1>Get in touch</h1>' || E'\n' ||
     '<p>Email <a href="mailto:hello@acme.test">hello@acme.test</a> or find us at the Old Port kiosk from 9:00.</p>',
     NULL, NULL,
     'PUBLISHED', NULL, :'user_id', NOW() - INTERVAL '370 days', NOW() - INTERVAL '370 days'),
    (:'page_faq_id', :'operator_id', 'FAQ', 'faq',
     '<h1>Frequently asked questions</h1>' || E'\n' ||
     '<h2>What if it rains?</h2><p>We reschedule or refund — your pick.</p>',
     NULL, NULL,
     'DRAFT', NULL, :'user_id', NOW() - INTERVAL '340 days', NOW() - INTERVAL '340 days'),
    (:'page_boats_id', :'operator_id', 'Our boats', 'our-boats',
     '<h1>The fleet</h1>' || E'\n' ||
     '<p>Two sailing boats and a RIB, all under twelve passengers.</p>' || E'\n' ||
     '<ul><li><strong>Sea Swallow</strong> — 11 m sloop, twelve guests.</li>' ||
     '<li><strong>Blue Marlin</strong> — 9 m RIB, eight guests.</li></ul>',
     'Our boats', 'Two sailing boats and a RIB, all under twelve passengers.',
     'PUBLISHED', 'wide', :'user_maria_id', NOW() - INTERVAL '150 days', NOW() - INTERVAL '20 days'),
    (:'page_press_id', :'operator_id', 'Press', 'press',
     '<h1>Press</h1>' || E'\n' || '<p>Kit and photos on request.</p>',
     NULL, NULL,
     'DRAFT', NULL, :'user_sofia_id', NOW() - INTERVAL '11 days', NOW() - INTERVAL '11 days')
ON CONFLICT DO NOTHING;

INSERT INTO page.page_translations
    (page_id, locale, tour_operator_id, handle, title, body, seo_title, seo_description)
VALUES
    (:'page_about_id', 'es', :'operator_id', 'sobre-nosotros', 'Sobre nosotros',
     '<h1>Quiénes somos</h1>' || E'\n' ||
     '<p>Paseos en barco familiares en la costa desde 1998.</p>',
     NULL, NULL),
    (:'page_boats_id', 'es', :'operator_id', NULL, 'Nuestros barcos',
     '<h1>La flota</h1>' || E'\n' ||
     '<p>Dos veleros y una neumática, todos para menos de doce pasajeros.</p>',
     NULL, NULL)
ON CONFLICT DO NOTHING;

-- 13. Navigation menus. The two defaults every operator gets at creation
-- (seeded here because this operator is inserted raw, bypassing the use
-- case), with a demo tree deep enough to exercise nesting: main-menu =
-- Home / Experiences (three children) / About, footer = Contact / Our boats /
-- an external link. The FAQ and Press pages are deliberately NOT linked —
-- they are DRAFT, which exercises the render side's unresolvable handling.
INSERT INTO touroperator.menus
    (id, tour_operator_id, handle, title, created_by, created_at, updated_at)
VALUES
    (:'menu_main_id',   :'operator_id', 'main-menu', 'Main menu', :'user_id',
     NOW() - INTERVAL '400 days', NOW() - INTERVAL '20 days'),
    (:'menu_footer_id', :'operator_id', 'footer',    'Footer',    :'user_id',
     NOW() - INTERVAL '400 days', NOW() - INTERVAL '20 days')
ON CONFLICT DO NOTHING;

-- Items resolve their menu by (operator, handle) instead of assuming the
-- fixed menu ids above landed: on a dev DB whose defaults came from the V5
-- backfill (random ids), the menus insert above no-ops on the handle
-- conflict, and a fixed menu_id here would abort the whole seed on the FK.
--
-- The three children reference their parent inside the same statement, which
-- is only legal because menu_items.parent_id is DEFERRABLE INITIALLY DEFERRED
-- — the constraint is checked at commit, by which time the parent row exists.
--
-- **These fixed ids do not survive the admin API, and the seed cannot tell.**
-- Menu items are not edited individually: PUT .../menus/{id}/items rewrites the
-- whole tree with FRESH ids. So one save from the admin (or one curl) replaces
-- these rows with randomly-keyed ones, the ids below no longer exist, and the
-- next `docker compose up` re-inserts all of them — the menu silently doubles.
-- Every other table here is safe from this because nothing else rewrites its
-- primary keys on update.
--
-- If you have edited a menu and want a clean re-seed, delete that menu's items
-- first (`DELETE FROM touroperator.menu_items ...`) or `docker compose down -v`.
INSERT INTO touroperator.menu_items
    (id, menu_id, parent_id, title, link_type, resource_id, url, position, created_at, updated_at)
SELECT v.id, m.id, v.parent_id, v.title, v.link_type, v.resource_id, v.url, v.position,
       NOW() - INTERVAL '20 days', NOW() - INTERVAL '20 days'
FROM (VALUES
    (:'mi_home_id'::uuid,        'main-menu', NULL::uuid,               'Home',              'HOME',            NULL::uuid,               NULL::varchar, 0),
    (:'mi_experiences_id'::uuid, 'main-menu', NULL::uuid,               'Experiences',       'EXPERIENCE_LIST', NULL::uuid,               NULL::varchar, 1),
    (:'mi_sunset_id'::uuid,      'main-menu', :'mi_experiences_id'::uuid, 'Sunset Sailing',  'EXPERIENCE',      :'experience_a_id'::uuid, NULL::varchar, 0),
    (:'mi_kayak_id'::uuid,       'main-menu', :'mi_experiences_id'::uuid, 'Kayak Cave',      'EXPERIENCE',      :'experience_c_id'::uuid, NULL::varchar, 1),
    (:'mi_diving_id'::uuid,      'main-menu', :'mi_experiences_id'::uuid, 'Blue Cave Diving','EXPERIENCE',      :'experience_d_id'::uuid, NULL::varchar, 2),
    (:'mi_about_id'::uuid,       'main-menu', NULL::uuid,               'About us',          'PAGE',            :'page_about_id'::uuid,   NULL::varchar, 2),
    (:'mi_contact_id'::uuid,     'footer',    NULL::uuid,               'Contact',           'PAGE',            :'page_contact_id'::uuid, NULL::varchar, 0),
    (:'mi_boats_id'::uuid,       'footer',    NULL::uuid,               'Our boats',         'PAGE',            :'page_boats_id'::uuid,   NULL::varchar, 1),
    (:'mi_instagram_id'::uuid,   'footer',    NULL::uuid,               'Instagram',         'EXTERNAL_URL',    NULL::uuid,               'https://instagram.com/acmetours', 2)
) AS v(id, menu_handle, parent_id, title, link_type, resource_id, url, position)
JOIN touroperator.menus m
    ON m.tour_operator_id = :'operator_id' AND m.handle = v.menu_handle
ON CONFLICT DO NOTHING;

INSERT INTO touroperator.menu_item_translations (menu_item_id, locale, title)
VALUES
    (:'mi_home_id',        'es', 'Inicio'),
    (:'mi_experiences_id', 'es', 'Experiencias'),
    (:'mi_about_id',       'es', 'Sobre nosotros')
ON CONFLICT DO NOTHING;

-- 14. Custom data. A metaobject definition with four fields and three entries
-- (two published, one not), plus eight metafield definitions across both owner
-- types and a value for most of them. The definitions cover every scalar type
-- the validator accepts plus one metaobject_reference, because the Custom Data
-- screens render per type and a fixture with only text fields tests one branch.
--
-- Values are stored in the canonical form the validator normalizes to —
-- "true"/"false" for boolean, a plain integer string, a canonical UUID for a
-- reference. Writing them any other way here would seed rows the API itself
-- could never have produced.
INSERT INTO metafield.metaobject_definitions
    (id, tour_operator_id, type, name, description, created_by, created_at, updated_at)
VALUES
    (:'mod_boat_id', :'operator_id', 'boat', 'Boat',
     'A vessel in the fleet. Referenced from experiences.',
     :'user_maria_id', NOW() - INTERVAL '160 days', NOW() - INTERVAL '160 days')
ON CONFLICT DO NOTHING;

INSERT INTO metafield.metaobject_field_definitions
    (id, definition_id, key, type, name, position, created_at, updated_at)
VALUES
    (:'mofd_name_id',     :'mod_boat_id', 'name',       'SINGLE_LINE_TEXT', 'Name',       0, NOW() - INTERVAL '160 days', NOW() - INTERVAL '160 days'),
    (:'mofd_capacity_id', :'mod_boat_id', 'capacity',   'NUMBER_INTEGER',   'Capacity',   1, NOW() - INTERVAL '160 days', NOW() - INTERVAL '160 days'),
    (:'mofd_year_id',     :'mod_boat_id', 'year_built', 'NUMBER_INTEGER',   'Year built', 2, NOW() - INTERVAL '160 days', NOW() - INTERVAL '160 days'),
    (:'mofd_notes_id',    :'mod_boat_id', 'notes',      'MULTI_LINE_TEXT',  'Notes',      3, NOW() - INTERVAL '155 days', NOW() - INTERVAL '155 days')
ON CONFLICT DO NOTHING;

INSERT INTO metafield.metaobject_entries
    (id, tour_operator_id, definition_id, handle, name, published, created_by, created_at, updated_at)
VALUES
    (:'moe_swallow_id', :'operator_id', :'mod_boat_id', 'sea-swallow', 'Sea Swallow', TRUE,
     :'user_maria_id', NOW() - INTERVAL '159 days', NOW() - INTERVAL '40 days'),
    (:'moe_marlin_id',  :'operator_id', :'mod_boat_id', 'blue-marlin', 'Blue Marlin', TRUE,
     :'user_maria_id', NOW() - INTERVAL '159 days', NOW() - INTERVAL '40 days'),
    (:'moe_gaffer_id',  :'operator_id', :'mod_boat_id', 'old-gaffer',  'Old Gaffer',  FALSE,
     :'user_diego_id', NOW() - INTERVAL '25 days', NOW() - INTERVAL '25 days')
ON CONFLICT DO NOTHING;

-- Entry values, generated from a (handle, key, value) list so adding a boat is
-- one row rather than four ids. Old Gaffer deliberately fills only two of the
-- four fields — a half-filled unpublished draft is the state worth seeing.
INSERT INTO metafield.metaobject_entry_values
    (id, entry_id, field_definition_id, value, created_by, created_at, updated_at)
SELECT md5('metaobject_entry_value:' || e.id || ':' || fd.id)::uuid,
       e.id, fd.id, v.value, e.created_by, e.created_at, e.updated_at
FROM (VALUES
    ('sea-swallow', 'name',       'Sea Swallow'),
    ('sea-swallow', 'capacity',   '12'),
    ('sea-swallow', 'year_built', '2014'),
    ('sea-swallow', 'notes',      E'Sloop, 11 m.\nRefit in 2022: new sails and a bigger bimini.'),
    ('blue-marlin', 'name',       'Blue Marlin'),
    ('blue-marlin', 'capacity',   '8'),
    ('blue-marlin', 'year_built', '2019'),
    ('blue-marlin', 'notes',      'RIB, 9 m. Used for the diving trips and rough-water days.'),
    ('old-gaffer',  'name',       'Old Gaffer'),
    ('old-gaffer',  'capacity',   '6')
) AS v(entry_handle, field_key, value)
JOIN metafield.metaobject_entries e
    ON e.tour_operator_id = :'operator_id'
   AND e.definition_id = :'mod_boat_id'
   AND e.handle = v.entry_handle
JOIN metafield.metaobject_field_definitions fd
    ON fd.definition_id = :'mod_boat_id' AND fd.key = v.field_key
ON CONFLICT DO NOTHING;

INSERT INTO metafield.metafield_definitions
    (id, tour_operator_id, owner_type, namespace, key, type, name, description,
     metaobject_definition_id, created_by, created_at, updated_at)
VALUES
    (:'mfd_difficulty_id', :'operator_id', 'EXPERIENCE', 'custom', 'difficulty',
     'SINGLE_LINE_TEXT', 'Difficulty', 'Easy, moderate or hard.',
     NULL, :'user_maria_id', NOW() - INTERVAL '170 days', NOW() - INTERVAL '170 days'),
    (:'mfd_min_age_id', :'operator_id', 'EXPERIENCE', 'custom', 'min_age',
     'NUMBER_INTEGER', 'Minimum age', 'Youngest guest accepted, in years.',
     NULL, :'user_maria_id', NOW() - INTERVAL '170 days', NOW() - INTERVAL '170 days'),
    (:'mfd_wetsuit_id', :'operator_id', 'EXPERIENCE', 'custom', 'wetsuit_included',
     'BOOLEAN', 'Wetsuit included', NULL,
     NULL, :'user_maria_id', NOW() - INTERVAL '168 days', NOW() - INTERVAL '168 days'),
    (:'mfd_meetpoint_id', :'operator_id', 'EXPERIENCE', 'custom', 'meeting_point_url',
     'URL', 'Meeting point map', 'A link to the pin on a map.',
     NULL, :'user_diego_id', NOW() - INTERVAL '90 days', NOW() - INTERVAL '90 days'),
    (:'mfd_season_id', :'operator_id', 'EXPERIENCE', 'custom', 'season_notes',
     'MULTI_LINE_TEXT', 'Season notes', 'Anything guests should know about the time of year.',
     NULL, :'user_diego_id', NOW() - INTERVAL '90 days', NOW() - INTERVAL '90 days'),
    (:'mfd_boat_id', :'operator_id', 'EXPERIENCE', 'custom', 'boat',
     'METAOBJECT_REFERENCE', 'Boat', 'Which vessel runs this experience.',
     :'mod_boat_id', :'user_maria_id', NOW() - INTERVAL '158 days', NOW() - INTERVAL '158 days'),
    (:'mfd_subtitle_id', :'operator_id', 'PAGE', 'custom', 'hero_subtitle',
     'SINGLE_LINE_TEXT', 'Hero subtitle', 'Shown under the page title.',
     NULL, :'user_sofia_id', NOW() - INTERVAL '35 days', NOW() - INTERVAL '35 days'),
    (:'mfd_footer_id', :'operator_id', 'PAGE', 'custom', 'show_in_footer',
     'BOOLEAN', 'Show in footer', NULL,
     NULL, :'user_sofia_id', NOW() - INTERVAL '35 days', NOW() - INTERVAL '35 days')
ON CONFLICT DO NOTHING;

-- Values, keyed by (definition, owner). Coverage is deliberately uneven — the
-- sunset sail fills every field, the food walk fills two, the draft experience
-- fills none. A fixture where every owner has every value cannot show you what
-- an unset metafield looks like.
INSERT INTO metafield.metafield_values
    (id, definition_id, owner_id, value, created_by, created_at, updated_at)
SELECT md5('metafield_value:' || v.definition_id || ':' || v.owner_id)::uuid,
       v.definition_id, v.owner_id, v.value, :'user_maria_id',
       NOW() - INTERVAL '80 days', NOW() - INTERVAL '15 days'
FROM (VALUES
    (:'mfd_difficulty_id'::uuid, :'experience_a_id'::uuid, 'Easy'),
    (:'mfd_min_age_id'::uuid,    :'experience_a_id'::uuid, '6'),
    (:'mfd_wetsuit_id'::uuid,    :'experience_a_id'::uuid, 'false'),
    (:'mfd_meetpoint_id'::uuid,  :'experience_a_id'::uuid, 'https://maps.example.com/?q=old-port'),
    (:'mfd_season_id'::uuid,     :'experience_a_id'::uuid,
     E'Best from May to September.\nOctober departures sail earlier as the light goes.'),
    (:'mfd_boat_id'::uuid,       :'experience_a_id'::uuid, :'moe_swallow_id'),
    (:'mfd_difficulty_id'::uuid, :'experience_b_id'::uuid, 'Easy'),
    (:'mfd_min_age_id'::uuid,    :'experience_b_id'::uuid, '0'),
    (:'mfd_difficulty_id'::uuid, :'experience_c_id'::uuid, 'Moderate'),
    (:'mfd_min_age_id'::uuid,    :'experience_c_id'::uuid, '10'),
    (:'mfd_wetsuit_id'::uuid,    :'experience_c_id'::uuid, 'true'),
    (:'mfd_boat_id'::uuid,       :'experience_c_id'::uuid, :'moe_marlin_id'),
    (:'mfd_difficulty_id'::uuid, :'experience_d_id'::uuid, 'Hard'),
    (:'mfd_min_age_id'::uuid,    :'experience_d_id'::uuid, '18'),
    (:'mfd_wetsuit_id'::uuid,    :'experience_d_id'::uuid, 'true'),
    (:'mfd_boat_id'::uuid,       :'experience_d_id'::uuid, :'moe_marlin_id'),
    (:'mfd_subtitle_id'::uuid,   :'page_about_id'::uuid,   'Since 1998, off the same quay'),
    (:'mfd_footer_id'::uuid,     :'page_about_id'::uuid,   'true'),
    (:'mfd_subtitle_id'::uuid,   :'page_boats_id'::uuid,   'Two sloops and a RIB'),
    (:'mfd_footer_id'::uuid,     :'page_boats_id'::uuid,   'false')
) AS v(definition_id, owner_id, value)
ON CONFLICT DO NOTHING;

-- 15. Contact-inbox messages. Nine unread, three read, so the unread badge has
-- a number and the read/unread split is visible in one screen. created_at
-- ASCENDS with the fixed ids (070 oldest) so the inbox's -id default order
-- matches recency, like real UUIDv7 intake rows will. One message has a NULL
-- name — the form allows it, so the list has to render it.
INSERT INTO contact.contact_messages
    (id, tour_operator_id, name, email, summary, content, read_at, created_at)
VALUES
    (:'cm_sizes_id', :'operator_id', 'Laura Pérez', 'laura@example.com',
     'Do you have child seats on the sunset tour?',
     'Hi! We are a family of four (kids are 4 and 7). Do you provide child-size life vests and seats on the Sunset Sailing Tour? Thanks!',
     NOW() - INTERVAL '29 days', NOW() - INTERVAL '30 days'),
    (:'cm_group_id', :'operator_id', 'Tom Baker', 'tom@example.org',
     'Group booking for 15 people',
     'Hello, I am organising a company outing in September for about 15 people. Can we book a private departure, and is there a group rate?',
     NOW() - INTERVAL '20 days', NOW() - INTERVAL '21 days'),
    (:'cm_gift_id', :'operator_id', NULL, 'ana@example.net',
     'Gift voucher?',
     'Do you sell gift vouchers for the kayak trip? I would like to give one to my sister for her birthday.',
     NOW() - INTERVAL '13 days', NOW() - INTERVAL '14 days'),
    (:'cm_wheelchair_id', :'operator_id', 'Bea Ortiz', 'bea@example.com',
     'Wheelchair access at the Old Port',
     'My father uses a wheelchair. Is boarding at the Old Port possible, and is there a step onto the boat?',
     NULL, NOW() - INTERVAL '9 days'),
    (:'cm_lost_id', :'operator_id', 'Henrik Sund', 'henrik@example.se',
     'Left a jacket on board',
     'I sailed with you on Saturday evening and left a navy rain jacket under the bench. Did anyone hand it in?',
     NULL, NOW() - INTERVAL '8 days'),
    (:'cm_weather_id', :'operator_id', 'Marta Gil', 'marta@example.com',
     'What happens if the forecast is bad?',
     'We are booked for the 18:00 sail next week and the forecast looks windy. When do you decide whether to sail?',
     NULL, NOW() - INTERVAL '6 days'),
    (:'cm_invoice_id', :'operator_id', 'Julien Roy', 'julien@example.fr',
     'Invoice with company VAT number',
     'Bonjour, could you issue an invoice with our company VAT number for the food walk we did in June? I can send the details.',
     NULL, NOW() - INTERVAL '5 days'),
    (:'cm_press_id', :'operator_id', 'Ada Okonkwo', 'ada@example.co.uk',
     'Press enquiry — travel feature',
     'I am writing a piece on small-group coastal operators for a UK travel title. Would you have twenty minutes for a call this month?',
     NULL, NOW() - INTERVAL '4 days'),
    (:'cm_dietary_id', :'operator_id', 'Sam Fischer', 'sam@example.de',
     'Vegetarian options on the food walk',
     'Two of us are vegetarian. Are the seven tastings adaptable, or should we look at a different tour?',
     NULL, NOW() - INTERVAL '3 days'),
    (:'cm_parking_id', :'operator_id', 'Nuria Blanco', 'nuria@example.es',
     'Where do we park?',
     '¿Hay aparcamiento cerca del puerto viejo? Llegamos en coche desde Toledo.',
     NULL, NOW() - INTERVAL '2 days'),
    (:'cm_private_id', :'operator_id', 'Otto Lehtinen', 'otto@example.fi',
     'Private charter for a proposal',
     'I would like to book the whole boat for a sunset sail in three weeks. It is a proposal, so I would want it just us. Possible?',
     NULL, NOW() - INTERVAL '1 day'),
    (:'cm_partner_id', :'operator_id', 'Hotel Sol', 'reservas@hotelsol.example',
     'Partnership — concierge referrals',
     'Buenos días. Somos un hotel a diez minutos del puerto y nos gustaría ofrecer vuestras salidas a nuestros huéspedes. ¿Con quién hablamos?',
     NULL, NOW() - INTERVAL '3 hours')
ON CONFLICT DO NOTHING;

-- 16. Activity trail. Invented history, as the header says — but the Activity
-- screen filters by entity type, actor and action, and none of that is
-- reviewable against an empty table. Entries ascend with their ids so the
-- newest row is also the highest id, matching what real UUIDv7 writes produce.
--
-- The actions and entity types are the real ones the use cases emit (grep
-- `new NewAuditEntry(` to check), and `changes` uses the FieldChange shape the
-- mapper serializes: [{"field":…,"from":…,"to":…}].
--
-- Every row is a USER actor. The schema also allows SYSTEM (and
-- AuditActor.system() exists), but nothing in the application produces one, so
-- a SYSTEM row here would be a state the UI can never actually receive. Same
-- reason there is no `invitation.expired` entry: EXPIRED is judged on access,
-- not written as an event.
INSERT INTO audit.audit_log
    (id, tour_operator_id, actor_type, actor_id, actor_name,
     entity_type, entity_id, action, details, changes, request_id, created_at)
SELECT
    ('01900000-0000-7000-8000-0000000000' || v.hex)::uuid,
    :'operator_id', v.actor_type,
    CASE v.actor_type WHEN 'SYSTEM' THEN NULL ELSE v.actor_id END,
    v.actor_name, v.entity_type, v.entity_id, v.action,
    v.details::jsonb, v.changes::jsonb,
    'seed-' || v.hex, NOW() - (v.days_ago || ' days')::interval
FROM (VALUES
    ('e0', 'USER', :'user_maria_id'::uuid, 'María Robles', 'EXPERIENCE', :'experience_a_id'::uuid,
     'experience.updated', NULL, '[{"field":"durationMinutes","from":120,"to":150}]', 27),
    ('e1', 'USER', :'user_maria_id'::uuid, 'María Robles', 'AUDIENCE', :'audience_family_id'::uuid,
     'audience.created', '{"name":"Family pack","paxPerUnit":4}', NULL, 26),
    ('e2', 'USER', :'user_id'::uuid, 'Dev Admin', 'TOUR_OPERATOR', :'operator_id'::uuid,
     'tour_operator.policy_updated', '{"type":"CANCELLATION"}', NULL, 25),
    ('e3', 'USER', :'user_diego_id'::uuid, 'Diego Santos', 'PICKUP_LOCATION', :'pickup_station_id'::uuid,
     'pickup_location.created', '{"name":"Atocha station"}', NULL, 24),
    ('e4', 'USER', :'user_maria_id'::uuid, 'María Robles', 'PAGE', :'page_boats_id'::uuid,
     'page.updated', NULL, '[{"field":"templateSuffix","from":null,"to":"wide"}]', 21),
    ('e5', 'USER', :'user_maria_id'::uuid, 'María Robles', 'MENU', :'menu_main_id'::uuid,
     'menu.items_replaced', '{"itemCount":6}', NULL, 20),
    ('e6', 'USER', :'user_id'::uuid, 'Dev Admin', 'INVITATION', :'inv_revoked_id'::uuid,
     'invitation.revoked', '{"email":"oskar@example.com"}', NULL, 15),
    ('e7', 'USER', :'user_diego_id'::uuid, 'Diego Santos', 'METAOBJECT', :'moe_gaffer_id'::uuid,
     'metaobject.created', '{"type":"boat","handle":"old-gaffer"}', NULL, 14),
    ('e8', 'USER', :'user_maria_id'::uuid, 'María Robles', 'EXPERIENCE', :'experience_c_id'::uuid,
     'experience.metafield_updated', '{"namespace":"custom","key":"difficulty"}',
     '[{"field":"value","from":"Easy","to":"Moderate"}]', 12),
    ('e9', 'USER', :'user_sofia_id'::uuid, 'Sofía Marín', 'PAGE', :'page_press_id'::uuid,
     'page.created', '{"handle":"press","status":"DRAFT"}', NULL, 11),
    ('ea', 'USER', :'user_id'::uuid, 'Dev Admin', 'TOUR_OPERATOR', :'operator_id'::uuid,
     'tour_operator.updated', NULL,
     '[{"field":"phone","from":null,"to":"+34 910 000 000"},{"field":"email","from":null,"to":"hola@acme.test"}]', 9),
    ('eb', 'USER', :'user_id'::uuid, 'Dev Admin', 'TOUR_OPERATOR', :'operator_id'::uuid,
     'tour_operator.brand_updated', NULL,
     '[{"field":"slogan","from":"Sail with us","to":"Sail the coast, not the crowds."}]', 9),
    ('ec', 'USER', :'user_sofia_id'::uuid, 'Sofía Marín', 'MEDIA', :'media_og_id'::uuid,
     'media.uploaded', '{"originalName":"og-share-card.png","sizeBytes":14294}', NULL, 9),
    ('ed', 'USER', :'user_id'::uuid, 'Dev Admin', 'MEMBER', :'member_noa_id'::uuid,
     'member.invited', '{"email":"noa@acme.test","role":"STAFF"}', NULL, 6),
    ('ee', 'USER', :'user_noa_id'::uuid, 'Noa Lindqvist', 'INVITATION', :'inv_accepted_id'::uuid,
     'invitation.accepted', '{"email":"noa@acme.test"}', NULL, 6),
    ('ef', 'USER', :'user_sofia_id'::uuid, 'Sofía Marín', 'EXPERIENCE', :'experience_e_id'::uuid,
     'experience.created', '{"handle":"winter-whale-watching","published":false}', NULL, 4),
    ('f0', 'USER', :'user_maria_id'::uuid, 'María Robles', 'SLOT', :'slot_a6_id'::uuid,
     'slot.updated', '{"audiences":["Adult"]}', '[{"field":"capacity","from":20,"to":18}]', 3),
    ('f1', 'USER', :'user_diego_id'::uuid, 'Diego Santos', 'EXPERIENCE', :'experience_b_id'::uuid,
     'experience.slots_created', '{"count":2}', NULL, 3),
    ('f2', 'USER', :'user_maria_id'::uuid, 'María Robles', 'METAFIELD_DEFINITION', :'mfd_season_id'::uuid,
     'metafield_definition.updated', '{"key":"season_notes"}',
     '[{"field":"name","from":"Season","to":"Season notes"}]', 2),
    ('f3', 'USER', :'user_id'::uuid, 'Dev Admin', 'MEMBER', :'member_diego_id'::uuid,
     'member.role_changed', NULL, '[{"field":"role","from":"ADMIN","to":"STAFF"}]', 2),
    ('f4', 'USER', :'user_sofia_id'::uuid, 'Sofía Marín', 'MEDIA', :'media_crew_id'::uuid,
     'media.described', NULL, '[{"field":"alt","from":null,"to":"The Acme crew on the quay"}]', 2),
    ('f5', 'USER', :'user_maria_id'::uuid, 'María Robles', 'EXPERIENCE', :'experience_d_id'::uuid,
     'experience.published', NULL, '[{"field":"published","from":false,"to":true}]', 1),
    ('f6', 'USER', :'user_maria_id'::uuid, 'María Robles', 'SLOT', :'slot_a7_id'::uuid,
     'slot.cancelled', NULL,
     '[{"field":"status","from":"AVAILABLE","to":"CANCELLED"}]', 1),
    ('f7', 'USER', :'user_diego_id'::uuid, 'Diego Santos', 'METAOBJECT', :'moe_marlin_id'::uuid,
     'metaobject.published', '{"handle":"blue-marlin"}', NULL, 1),
    ('f8', 'USER', :'user_id'::uuid, 'Dev Admin', 'INVITATION', :'inv_pending_id'::uuid,
     'invitation.resent', '{"email":"pilar@example.com"}', NULL, 1),
    ('f9', 'USER', :'user_maria_id'::uuid, 'María Robles', 'PAGE', :'page_faq_id'::uuid,
     'page.updated', NULL, '[{"field":"body","from":"(212 chars)","to":"(196 chars)"}]', 1),
    -- Points at a message that is NOT in the inbox above, because that is what
    -- a delete leaves behind: a trail entry whose entity is gone.
    ('fa', 'USER', :'user_sofia_id'::uuid, 'Sofía Marín', 'CONTACT_MESSAGE', :'cm_deleted_id'::uuid,
     'contact_message.deleted', '{"email":"spam@example.com"}', NULL, 1)
) AS v(hex, actor_type, actor_id, actor_name, entity_type, entity_id, action,
       details, changes, days_ago)
ON CONFLICT DO NOTHING;
