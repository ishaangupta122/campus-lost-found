-- One-off: moves every STUDENT from @gmail.com to @thapar.edu
-- (asha@gmail.com -> asha@thapar.edu). Admins are not touched.
-- Passwords stay the same: the stored hash does not depend on the email.
--
-- Run from the project root:
--   psql -U <db_user> -d campus_lost_found -f backend/database/rename-student-emails.sql

\set ON_ERROR_STOP on
BEGIN;

-- Students that CANNOT be renamed because the @thapar.edu address is already taken
SELECT u.id, u.email AS gmail_account, t.email AS already_exists
FROM users u
JOIN users t ON t.email = regexp_replace(lower(u.email), '@gmail\.com$', '@thapar.edu')
WHERE u.role = 'STUDENT' AND lower(u.email) LIKE '%@gmail.com';

UPDATE users u
SET email = regexp_replace(lower(u.email), '@gmail\.com$', '@thapar.edu')
WHERE u.role = 'STUDENT'
  AND lower(u.email) LIKE '%@gmail.com'
  AND NOT EXISTS (SELECT 1 FROM users t
                  WHERE t.email = regexp_replace(lower(u.email), '@gmail\.com$', '@thapar.edu'));

SELECT id, name, email, role FROM users ORDER BY role, id;
COMMIT;
