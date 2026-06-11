CREATE SCHEMA IF NOT EXISTS core;

-- Owns all core tables; modules with write access are granted membership.
DO $$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'core_owner') THEN
    CREATE ROLE core_owner NOLOGIN;
  END IF;
END $$;

CREATE TABLE core.patients (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    mrn         TEXT        NOT NULL UNIQUE,   -- medical record number
    given_name  TEXT        NOT NULL,
    family_name TEXT        NOT NULL,
    birth_date  DATE        NOT NULL,
    sex         TEXT        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
ALTER TABLE core.patients OWNER TO core_owner;
