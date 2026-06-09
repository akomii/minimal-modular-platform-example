-- core's tables are owned by a dedicated non-superuser role so that an authorized
-- module can be granted membership (GRANT core_owner TO mod_x) and thereby alter
-- them. Unauthorized modules lack the membership and are rejected by Postgres.
DO $$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'core_owner') THEN
    CREATE ROLE core_owner NOLOGIN;
  END IF;
END $$;

ALTER TABLE core.patients OWNER TO core_owner;
