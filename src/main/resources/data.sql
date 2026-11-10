-- Enforces "a user may hold at most one ACTIVE loan of a book" at the database level.
-- This is the last line of defence behind the pessimistic row lock on book.
--
-- H2 has no partial indexes, so a generated column is used to make the key NULL
-- for returned loans (NULLs do not collide in a unique index).
--
-- PostgreSQL equivalent (preferred on a real deployment, no extra column needed):
--   CREATE UNIQUE INDEX ux_loan_active_book_user
--       ON loan (book_id, user_id) WHERE returned_at IS NULL;
--
-- MySQL equivalent (same generated-column trick as below):
--   ALTER TABLE loan ADD COLUMN active_key VARCHAR(80)
--     GENERATED ALWAYS AS (IF(returned_at IS NULL, CONCAT(book_id, ':', user_id), NULL)) VIRTUAL;
--   CREATE UNIQUE INDEX ux_loan_active_book_user ON loan (active_key);
ALTER TABLE loan ADD COLUMN IF NOT EXISTS active_key VARCHAR(80)
    GENERATED ALWAYS AS (
        CASE WHEN returned_at IS NULL THEN CONCAT(book_id, ':', user_id) END
    );

CREATE UNIQUE INDEX IF NOT EXISTS ux_loan_active_book_user ON loan (active_key);