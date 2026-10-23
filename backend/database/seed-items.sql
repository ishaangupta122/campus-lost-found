-- Sample data: 10 lost/found reports for demos.
--
-- Before running:
--   1. start the backend once, so the tables exist;
--   2. register at least 3 students (the admin account is not used).
--
-- Run from the project root:
--   psql -U <db_user> -d campus_lost_found -f backend/database/seed-items.sql
--
-- Notes:
--   - Reports are spread over the first 3 students (by id).
--   - Every report starts OPEN and has no photo (add one by editing the report).
--   - Safe to run again: nothing is inserted if the items table already has rows.

WITH students AS (
    SELECT id, row_number() OVER (ORDER BY id) AS n
    FROM users
    WHERE role = 'STUDENT'
    ORDER BY id
    LIMIT 3
),
seed (n, item_name, description, category, type, location, days_ago) AS (
    VALUES
    (1, 'Black Jansport backpack', 'Black backpack with a red keychain on the zip. Contains notebooks and a calculator.', 'Bags',          'LOST',  'Central Library, 2nd floor', 1),
    (2, 'Blue steel water bottle', 'Blue insulated bottle with a dent near the bottom and a "Go Green" sticker.',       'Water bottles', 'FOUND', 'Main Canteen',               2),
    (3, 'Student ID card',         'ID card found near the notice board. Name is partly faded; photo is visible.',      'ID cards',      'FOUND', 'Admin Block entrance',       2),
    (1, 'Samsung earbuds case',    'White earbuds case with a small crack on the lid. Left earbud is missing.',          'Electronics',   'LOST',  'Computer Lab 3',             3),
    (2, 'Hostel room keys',        'Bunch of three keys on a green lanyard with a Hostel B tag.',                        'Keys',          'FOUND', 'Hostel B common room',       4),
    (3, 'Engineering Mathematics textbook', 'Thick blue textbook with handwritten name on the first page.',              'Books',         'LOST',  'Lecture Hall 204',           5),
    (1, 'Grey hoodie',             'Grey zip-up hoodie, size M, with a university logo on the back.',                    'Clothing',      'FOUND', 'Sports Ground bleachers',    6),
    (2, 'Scientific calculator',   'Casio fx-991EX with initials "A.K." scratched on the back.',                         'Stationery',    'LOST',  'Exam Hall A',                7),
    (3, 'Laptop charger',          '65W USB-C charger with a white cable, wrapped with black tape near the plug.',       'Electronics',   'FOUND', 'Cafeteria, corner table',    8),
    (1, 'Prescription glasses',    'Black rectangular frame glasses in a brown hard case.',                              'Other',         'LOST',  'Auditorium, row F',          10)
)
INSERT INTO items (item_name, description, category, type, location, report_date, status, image_path, reporter_id, created_at)
SELECT s.item_name, s.description, s.category, s.type, s.location,
       CURRENT_DATE - s.days_ago, 'OPEN', NULL, st.id,
       now() - make_interval(days => s.days_ago)
FROM seed s
JOIN students st ON st.n = s.n
WHERE NOT EXISTS (SELECT 1 FROM items);
