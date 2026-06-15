CREATE TABLE core.module_provisioning (
    module_id          TEXT        PRIMARY KEY,
    core_access        TEXT        NOT NULL,            -- none | read | write
    authorized         BOOLEAN     NOT NULL DEFAULT false,
    installed_at       TIMESTAMPTZ,
    installed_version  TEXT,                            -- manifest version at last (re)provision
    applied_migrations INTEGER     NOT NULL DEFAULT 0,  -- how many of db.migrations have run
    db_password        TEXT                             -- module db login, plaintext for the demo
);
