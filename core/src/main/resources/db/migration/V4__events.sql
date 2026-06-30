-- Durable append-only event log backing the platform's pub/sub bus. Core is the only writer
-- (modules publish and subscribe over HTTP), and publishes are serialized so seq is gap-free
-- and a subscriber can replay from any seq. Owned by the app user; modules never touch it
-- directly, so it needs no module grants and is not audited.
CREATE TABLE core.events
(
    seq          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    topic        TEXT        NOT NULL,
    payload      JSONB       NOT NULL,
    publisher    TEXT        NOT NULL, -- the module's oauth client id, or 'core'
    published_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX events_topic_seq ON core.events (topic, seq);

-- Signals the in-core listener that a new event landed. The payload is just the topic name
-- (NOTIFY payloads are size-limited), so the listener re-reads the row from the table.
CREATE FUNCTION core.notify_event() RETURNS trigger
    LANGUAGE plpgsql AS $$
BEGIN
  PERFORM
pg_notify('core_events', NEW.topic);
RETURN NULL;
END $$;

CREATE TRIGGER notify_event
    AFTER INSERT
    ON core.events
    FOR EACH ROW EXECUTE FUNCTION core.notify_event();
