-- Runs as role mod_weather (still a core_owner member) on purge.
-- Reverts the core change; the module's own schema is dropped by core
-- (DROP OWNED BY mod_weather CASCADE).
ALTER TABLE core.patients DROP COLUMN IF EXISTS mod_weather_opt_in;
