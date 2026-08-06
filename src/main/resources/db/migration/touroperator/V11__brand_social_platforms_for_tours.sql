-- V10 took Shopify's nine platforms verbatim. That list is theirs, and it is an
-- e-commerce list: it has Tumblr, Snapchat and Vimeo, and it does not have the
-- two a tour operator actually needs.
--
-- TRIPADVISOR is where a tour is reviewed, and reviews are most of what converts
-- a booking. WHATSAPP is the contact channel operators run on across most of
-- Latin America, southern Europe and southeast Asia — for many of them it is the
-- primary one, ahead of email.
--
-- Dropped rather than kept-and-extended: an operator picking from a list that
-- includes Tumblr is being asked a question about someone else's business. The
-- list stays closed (a theme renders one icon per platform and cannot render an
-- icon for a name it has never heard of), so it has to be the right closed list.
--
-- No data migration: nothing writes these yet and the only rows are the dev
-- seed's, which use FACEBOOK, INSTAGRAM and YOUTUBE. If that stops being true,
-- this needs a DELETE for the dropped values before the constraint is replaced.
ALTER TABLE touroperator.tour_operator_brand_social_links
    DROP CONSTRAINT tour_operator_brand_social_links_platform_check;

ALTER TABLE touroperator.tour_operator_brand_social_links
    ADD CONSTRAINT tour_operator_brand_social_links_platform_check
        CHECK (platform IN (
            'FACEBOOK','INSTAGRAM','TIKTOK','YOUTUBE','TWITTER',
            'PINTEREST','TRIPADVISOR','WHATSAPP'));
