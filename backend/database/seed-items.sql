-- Sample data for demos: 15 lost/found reports, handed out automatically to
-- the first 4 students who register (4 + 4 + 4 + 3 reports).
--
-- Does NOT create any users. Run it before or after students register:
--   - students who already exist get their reports straight away;
--   - every student who registers later is given the next unassigned set,
--     automatically, by a database trigger (no app change needed).
--
-- Run from the project root (start the backend once first, so tables exist):
--   psql -U <db_user> -d campus_lost_found -f backend/database/seed-items.sql
--
-- Notes:
--   - Only STUDENT accounts get reports; the admin never does.
--   - Students 5 and later get nothing (all 4 sets are taken).
--   - Every report starts OPEN and has no photo (add one by editing the report).
--   - Safe to run again. If the items table is empty, the 4 sets are handed out
--     again from scratch; otherwise only sets still waiting for a student are.
--     To start over, use reset-data.sql.
--   - The helper tables seed_reports and seed_slots are not used by the app.

\set ON_ERROR_STOP on
SET client_min_messages = warning;

-- 1. Templates: the 15 reports, split into 4 sets (slots)
CREATE TABLE IF NOT EXISTS seed_reports (
    id          serial PRIMARY KEY,
    slot        int          NOT NULL,
    item_name   varchar(100) NOT NULL,
    description text         NOT NULL,
    category    varchar(50)  NOT NULL,
    type        varchar(10)  NOT NULL,
    location    varchar(100) NOT NULL,
    days_ago    int          NOT NULL
);

-- Which student holds each set (NULL = waiting for the next student)
CREATE TABLE IF NOT EXISTS seed_slots (
    slot       int PRIMARY KEY,
    student_id bigint UNIQUE REFERENCES users(id) ON DELETE SET NULL
);

TRUNCATE seed_reports RESTART IDENTITY;
INSERT INTO seed_reports (slot, item_name, description, category, type, location, days_ago) VALUES
    -- Set 1: first student (4)
    (1, 'Black Jansport backpack', 'Black backpack with a red keychain on the zip. Contains notebooks and a calculator.', 'Bags',          'LOST',  'Central Library, 2nd floor', 1),
    (1, 'Samsung earbuds case',    'White earbuds case with a small crack on the lid. Left earbud is missing.',          'Electronics',   'LOST',  'Computer Lab 3',             3),
    (1, 'Grey hoodie',             'Grey zip-up hoodie, size M, with a university logo on the back.',                    'Clothing',      'FOUND', 'Sports Ground bleachers',    6),
    (1, 'Prescription glasses',    'Black rectangular frame glasses in a brown hard case.',                              'Other',         'LOST',  'Auditorium, row F',          10),
    -- Set 2: second student (4)
    (2, 'Blue steel water bottle', 'Blue insulated bottle with a dent near the bottom and a "Go Green" sticker.',       'Water bottles', 'FOUND', 'Main Canteen',               2),
    (2, 'Hostel room keys',        'Bunch of three keys on a green lanyard with a Hostel B tag.',                        'Keys',          'FOUND', 'Hostel B common room',       4),
    (2, 'Scientific calculator',   'Casio fx-991EX with initials "A.K." scratched on the back.',                         'Stationery',    'LOST',  'Exam Hall A',                7),
    (2, 'Silver wristwatch',       'Silver analog watch with a black leather strap. Strap buckle is slightly bent.',     'Other',         'LOST',  'Gym changing room',          9),
    -- Set 3: third student (4)
    (3, 'Student ID card',         'ID card found near the notice board. Name is partly faded; photo is visible.',      'ID cards',      'FOUND', 'Admin Block entrance',       2),
    (3, 'Engineering Mathematics textbook', 'Thick blue textbook with handwritten name on the first page.',              'Books',         'LOST',  'Lecture Hall 204',           5),
    (3, 'Laptop charger',          '65W USB-C charger with a white cable, wrapped with black tape near the plug.',       'Electronics',   'FOUND', 'Cafeteria, corner table',    8),
    (3, 'Black umbrella',          'Large black foldable umbrella with a wooden handle.',                                'Other',         'FOUND', 'Bus stop near Gate 2',       11),
    -- Set 4: fourth student (3)
    (4, 'Brown leather wallet',    'Brown bifold wallet with a metro card and some cash. No ID inside.',                 'Other',         'LOST',  'Parking lot B',              1),
    (4, 'Pink pencil pouch',       'Pink zip pouch with pens, a highlighter and a small ruler.',                         'Stationery',    'FOUND', 'Seminar Room 1',             3),
    (4, 'Purple water bottle',     'Purple plastic bottle with a flip lid and a name sticker that has peeled off.',      'Water bottles', 'LOST',  'Basketball court',           5);

INSERT INTO seed_slots (slot) VALUES (1), (2), (3), (4) ON CONFLICT (slot) DO NOTHING;

-- 2. Hands each waiting set to the next student (by id) who has no set yet
CREATE OR REPLACE FUNCTION seed_assign_reports() RETURNS void
LANGUAGE plpgsql AS $$
DECLARE
    s   record;
    sid bigint;
BEGIN
    FOR s IN SELECT slot FROM seed_slots WHERE student_id IS NULL ORDER BY slot LOOP
        SELECT u.id INTO sid
        FROM users u
        WHERE u.role = 'STUDENT'
          AND NOT EXISTS (SELECT 1 FROM seed_slots ss WHERE ss.student_id = u.id)
        ORDER BY u.id
        LIMIT 1;

        EXIT WHEN sid IS NULL;  -- no more students to give sets to

        UPDATE seed_slots SET student_id = sid WHERE slot = s.slot;

        INSERT INTO items (item_name, description, category, type, location,
                           report_date, status, image_path, reporter_id, created_at)
        SELECT r.item_name, r.description, r.category, r.type, r.location,
               CURRENT_DATE - r.days_ago, 'OPEN', NULL, sid,
               now() - make_interval(days => r.days_ago)
        FROM seed_reports r
        WHERE r.slot = s.slot
        ORDER BY r.id;
    END LOOP;
END;
$$;

-- 3. Trigger: runs the hand-out after every new user is saved
CREATE OR REPLACE FUNCTION seed_assign_on_register() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    PERFORM seed_assign_reports();
    RETURN NULL;
END;
$$;

DROP TRIGGER IF EXISTS seed_assign_after_user_insert ON users;
CREATE TRIGGER seed_assign_after_user_insert
    AFTER INSERT ON users
    FOR EACH STATEMENT
    EXECUTE FUNCTION seed_assign_on_register();

-- 4. Fresh start when there are no reports; then hand out to existing students
UPDATE seed_slots SET student_id = NULL WHERE NOT EXISTS (SELECT 1 FROM items);
SELECT seed_assign_reports();

SELECT ss.slot, u.email AS student,
       (SELECT count(*) FROM seed_reports r WHERE r.slot = ss.slot) AS reports
FROM seed_slots ss LEFT JOIN users u ON u.id = ss.student_id
ORDER BY ss.slot;
