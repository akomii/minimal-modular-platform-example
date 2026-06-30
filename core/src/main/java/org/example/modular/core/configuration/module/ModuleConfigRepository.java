package org.example.modular.core.configuration.module;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Reads and writes the {@code core.module_config} key/value store holding each module's config values.
 */
@Repository
public class ModuleConfigRepository {

  private final JdbcTemplate jdbc;

  public ModuleConfigRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public Map<String, String> findValues(String moduleId) {
    Map<String, String> values = new LinkedHashMap<>();
    jdbc.query("SELECT key, value FROM core.module_config WHERE module_id = ?", rs -> {
      values.put(rs.getString("key"), rs.getString("value"));
    }, moduleId);
    return values;
  }

  public void upsert(String moduleId, String key, String value) {
    jdbc.update("""
        INSERT INTO core.module_config (module_id, key, value) VALUES (?, ?, ?)
        ON CONFLICT (module_id, key) DO UPDATE SET value = EXCLUDED.value
        """, moduleId, key, value);
  }

  public void deleteAll(String moduleId) {
    jdbc.update("DELETE FROM core.module_config WHERE module_id = ?", moduleId);
  }
}
