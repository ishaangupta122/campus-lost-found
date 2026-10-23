-- Creates the (empty) database used by the backend.
-- Run once as a PostgreSQL superuser, from the backend/ folder:
--   psql -U postgres -f database/create-database.sql
--
-- The tables (users, items, claims) are NOT created here: the backend creates
-- them automatically on first start (spring.jpa.hibernate.ddl-auto=update).

CREATE DATABASE campus_lost_found;
