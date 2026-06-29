package org.example.modular.core.configuration.core;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Reads and writes the {@code core.config} key/value store backing the core live-config settings.
 */
@Repository
public class CoreConfigRepository {

  private final JdbcTemplate jdbc;

  public CoreConfigRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public Map<String, String> findAll() {
    Map<String, String> values = new LinkedHashMap<>();
    jdbc.query("SELECT key, value FROM core.config", rs -> {
      values.put(rs.getString("key"), rs.getString("value"));
    });
    return values;
  }

  public void upsert(String key, String value) {
    jdbc.update("""
        INSERT INTO core.config (key, value) VALUES (?, ?)
        ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value
        """, key, value);
  }
}
