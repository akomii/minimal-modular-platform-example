package org.example.modular.core.event;

import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * Postgres-backed {@link EventStore}: the {@code core.events} append-only log. Appends are serialized so the sequence is assigned and committed in order, keeping the log gap-free.
 */
@Repository
@ConditionalOnProperty(name = "events.backend", havingValue = "postgres", matchIfMissing = true)
public class PostgresEventStore implements EventStore {

  private static final RowMapper<EventRecord> EVENT = (rs, n) -> new EventRecord(rs.getLong("seq"), rs.getString("topic"), rs.getString("payload"));

  private final JdbcTemplate jdbc;

  public PostgresEventStore(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public synchronized long append(String topic, String payload, String publisher) {
    return jdbc.queryForObject("INSERT INTO core.events (topic, payload, publisher) VALUES (?, ?::jsonb, ?) RETURNING seq", Long.class, topic, payload, publisher);
  }

  @Override
  public List<EventRecord> replay(String topic, long sinceSeq) {
    return jdbc.query("SELECT seq, topic, payload FROM core.events WHERE topic = ? AND seq > ? ORDER BY seq", EVENT, topic, sinceSeq);
  }

  // Live-dispatch helpers for PostgresEventListener; not part of the EventStore seam.
  public List<EventRecord> readSince(long seq) {
    return jdbc.query("SELECT seq, topic, payload FROM core.events WHERE seq > ? ORDER BY seq", EVENT, seq);
  }

  public long maxSeq() {
    return jdbc.queryForObject("SELECT coalesce(max(seq), 0) FROM core.events", Long.class);
  }
}
