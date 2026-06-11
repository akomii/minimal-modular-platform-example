-- Login password of the module's database user (mod_<id>), generated at provisioning.
-- Stored in plaintext for the demo so it can be handed to module containers later.
ALTER TABLE core.module_provisioning ADD COLUMN db_password TEXT;
