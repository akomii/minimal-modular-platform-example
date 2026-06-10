DO $$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'core_owner') THEN
    CREATE ROLE core_owner NOLOGIN;
  END IF;
END $$;

ALTER TABLE core.patients OWNER TO core_owner;
