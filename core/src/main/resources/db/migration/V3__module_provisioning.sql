CREATE TABLE core.module_provisioning (
    module_id    TEXT        PRIMARY KEY,
    schema_name  TEXT,                            -- null when the module brings no own schema
    core_access  TEXT        NOT NULL,            -- none | read | write
    authorized   BOOLEAN     NOT NULL DEFAULT false,
    installed_at TIMESTAMPTZ,
    db_password  TEXT                             -- module db user login, plaintext for the demo
);
