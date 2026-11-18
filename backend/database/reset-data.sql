-- Resets test data while keeping every registered user (students and admin).
--
-- Deletes all claims (pending, approved, rejected, handed over) and all item
-- reports, then reloads the 15 default reports from seed-items.sql and hands
-- them to the first 4 students (by id). If fewer than 4 students exist, the
-- remaining sets go to the next students who register.
--
-- Run from the project root, as often as you like:
--   psql -U <db_user> -d campus_lost_found -f backend/database/reset-data.sql
--
-- Uploaded photos stay in backend/uploads/; empty that folder for a full clean.

\set ON_ERROR_STOP on
BEGIN;
TRUNCATE claims, items RESTART IDENTITY;
\ir seed-items.sql
COMMIT;

SELECT (SELECT count(*) FROM users)  AS users_kept,
       (SELECT count(*) FROM items)  AS seed_items,
       (SELECT count(*) FROM claims) AS claims;
