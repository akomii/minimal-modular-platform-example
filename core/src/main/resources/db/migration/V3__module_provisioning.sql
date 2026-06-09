CREATE TABLE core.module_provisioning (
    module_id    TEXT        PRIMARY KEY,
    schema_name  TEXT        NOT NULL,
    core_access  TEXT        NOT NULL,            -- none | read | write
    authorized   BOOLEAN     NOT NULL DEFAULT false,
    installed_at TIMESTAMPTZ
);
