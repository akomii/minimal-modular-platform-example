-- Runs as role mod_weather with search_path = mod_weather, core.
-- Own schema: private objects, allowed via schema ownership.
CREATE TABLE forecast (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    patient_mrn TEXT        NOT NULL,
    summary     TEXT        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Core: requires core_owner membership, granted only when authorized.
ALTER TABLE core.patients ADD COLUMN mod_weather_opt_in BOOLEAN NOT NULL DEFAULT false;
