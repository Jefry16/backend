-- V5 omitted the house-convention FK on created_by (every other created_by
-- column references identity.users). V5 is already applied, so the fix rides
-- its own migration. All existing values satisfy it: the backfill copied
-- tour_operators.created_by (itself FK'd), the create use case stamps the
-- authenticated caller, and the dev-seed uses the seeded admin user.
ALTER TABLE touroperator.menus
    ADD CONSTRAINT menus_created_by_fkey
        FOREIGN KEY (created_by) REFERENCES identity.users (id);
