CREATE SCHEMA IF NOT EXISTS core;

-- Owns all core tables; modules with write access are granted membership.
DO
$$
BEGIN
  IF
NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'core_owner') THEN
CREATE ROLE core_owner NOLOGIN;
END IF;
END $$;

CREATE TABLE core.patients
(
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    mrn         TEXT        NOT NULL UNIQUE, -- medical record number
    given_name  TEXT        NOT NULL,
    family_name TEXT        NOT NULL,
    birth_date  DATE        NOT NULL,
    sex         TEXT        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Append-only audit/undo log of every change a module makes to core: DDL written by the
-- provisioner (schema diff around migrations) and DML written by the trigger below. Replayed
-- newest-first on purge to leave core as the module never touched it. Owned by the app user
-- (not core_owner), so modules can't tamper with it.
CREATE TABLE core.module_core_audit
(
    seq          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    module_id    TEXT        NOT NULL,
    occurred_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    category     TEXT        NOT NULL, -- 'ddl' | 'dml'
    op           TEXT        NOT NULL, -- create_table | add_column | create_index | insert | update | delete
    target_table TEXT,
    target_name  TEXT,                 -- column/index name for ddl
    old_row      JSONB,                -- row before (dml update/delete)
    new_row      JSONB                 -- row after  (dml insert/update)
);

-- Records a module container's data writes to core. session_user is the module's mod_<id>
-- login (unaffected by SECURITY DEFINER / SET ROLE), so provisioner- and seed-driven writes
-- (session_user = the app user) are not audited.
CREATE FUNCTION core.audit_core_write() RETURNS trigger
    LANGUAGE plpgsql SECURITY DEFINER AS $audit$
BEGIN
  IF
session_user LIKE 'mod\_%' THEN
    INSERT INTO core.module_core_audit (module_id, category, op, target_table, old_row, new_row)
    VALUES (
      substring(session_user FROM 'mod_(.*)'),
      'dml',
      lower(TG_OP),
      TG_TABLE_NAME,
      CASE WHEN TG_OP IN ('UPDATE', 'DELETE') THEN to_jsonb(OLD) END,
      CASE WHEN TG_OP IN ('INSERT', 'UPDATE') THEN to_jsonb(NEW) END);
END IF;
RETURN NULL;
END
$audit$;

-- Replays a module's audit entries newest-first to undo its core writes. SECURITY INVOKER:
-- run as the module role (a core_owner member) on purge so it owns the changes it reverts.
CREATE FUNCTION core.undo_module_writes(p_module_id TEXT) RETURNS void
    LANGUAGE plpgsql AS $undo$
DECLARE
r RECORD;
BEGIN
FOR r IN
SELECT *
FROM core.module_core_audit
WHERE module_id = p_module_id
ORDER BY seq DESC LOOP
    IF r.op = 'insert' THEN
      EXECUTE format('DELETE FROM core.%I t WHERE to_jsonb(t) = $1', r.target_table) USING r.new_row;
ELSIF
r.op = 'delete' THEN
      EXECUTE format('INSERT INTO core.%I OVERRIDING SYSTEM VALUE SELECT * FROM jsonb_populate_record(NULL::core.%I, $1)',
                     r.target_table, r.target_table) USING r.old_row;
    ELSIF
r.op = 'update' THEN
      EXECUTE format('DELETE FROM core.%I t WHERE to_jsonb(t) = $1', r.target_table) USING r.new_row;
EXECUTE format('INSERT INTO core.%I OVERRIDING SYSTEM VALUE SELECT * FROM jsonb_populate_record(NULL::core.%I, $1)',
               r.target_table, r.target_table) USING r.old_row;
ELSIF
r.op = 'add_column' THEN
      EXECUTE format('ALTER TABLE core.%I DROP COLUMN IF EXISTS %I', r.target_table, r.target_name);
    ELSIF
r.op = 'create_index' THEN
      EXECUTE format('DROP INDEX IF EXISTS core.%I', r.target_name);
    ELSIF
r.op = 'create_table' THEN
      EXECUTE format('DROP TABLE IF EXISTS core.%I CASCADE', r.target_table);
END IF;
END LOOP;
END
$undo$;

-- Created while the app user still owns patients (before ownership moves to core_owner), so no
-- superuser is needed; the trigger persists after the ownership change.
CREATE TRIGGER audit_core_write
    AFTER INSERT OR
UPDATE OR
DELETE
ON core.patients
    FOR EACH ROW EXECUTE FUNCTION core.audit_core_write();

ALTER TABLE core.patients OWNER TO core_owner;
