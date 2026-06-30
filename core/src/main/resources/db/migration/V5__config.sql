-- Runtime configuration management.
--
-- core.config holds the values of the core settings the admin changes live from the management UI.
-- Rows are written only when a setting is changed; until then CoreConfigService serves each setting's
-- default, which is declared by the feature that owns it (so this migration enumerates no keys).
CREATE TABLE core.config
(
    key   TEXT PRIMARY KEY,
    value TEXT NOT NULL
);

-- core.module_config holds each module's config values, one row per (module, key). The set of keys,
-- their types and defaults are declared in the module's manifest; this table only stores values.
-- Like module_provisioning.db_password, secret values are stored in plaintext (a demo shortcut).
CREATE TABLE core.module_config
(
    module_id TEXT NOT NULL,
    key       TEXT NOT NULL,
    value     TEXT NOT NULL,
    PRIMARY KEY (module_id, key)
);
