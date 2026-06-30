package org.example.modular.core.dbviewer;

import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Read-only catalog access for the demo database viewer: enumerates non-system schemas/tables/columns from {@code information_schema} and reads capped pages of row data.
 *
 * <p>Table and schema names cannot be bound as JDBC parameters, so callers must validate a pair against {@link #tableExists} before passing it to the row-reading methods; identifiers are then
 * double-quoted before interpolation. The connection runs as the database superuser, so every schema is visible.
 */
@Repository
public class DbCatalogRepository {

  private final JdbcTemplate jdbc;

  public DbCatalogRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * Lists every non-system schema that has at least one base table, each with its table names ordered alphabetically.
   */
  public List<SchemaView> schemas() {
    List<String[]> pairs = jdbc.query("""
        SELECT table_schema, table_name
          FROM information_schema.tables
         WHERE table_type = 'BASE TABLE'
           AND table_schema NOT IN ('pg_catalog', 'information_schema')
           AND table_schema NOT LIKE 'pg_toast%'
           AND table_schema NOT LIKE 'pg_temp%'
         ORDER BY table_schema, table_name
        """, (rs, n) -> new String[]{rs.getString(1), rs.getString(2)});

    List<SchemaView> schemas = new ArrayList<>();
    Map<String, List<String>> tablesBySchema = new LinkedHashMap<>();
    for (String[] pair : pairs) {
      tablesBySchema.computeIfAbsent(pair[0], k -> new ArrayList<>()).add(pair[1]);
    }
    tablesBySchema.forEach((schema, tables) -> schemas.add(new SchemaView(schema, tables)));
    return schemas;
  }

  /**
   * Whether the given pair exists as a base table — the guard that makes the dynamic {@code SELECT} below injection-safe.
   */
  public boolean tableExists(String schema, String table) {
    Integer count = jdbc.queryForObject("""
        SELECT count(*) FROM information_schema.tables
         WHERE table_type = 'BASE TABLE' AND table_schema = ? AND table_name = ?
        """, Integer.class, schema, table);
    return count != null && count > 0;
  }

  /**
   * The table's column names in declaration order.
   */
  public List<String> columns(String schema, String table) {
    return jdbc.query("""
        SELECT column_name FROM information_schema.columns
         WHERE table_schema = ? AND table_name = ?
         ORDER BY ordinal_position
        """, (rs, n) -> rs.getString(1), schema, table);
  }

  public long rowCount(String schema, String table) {
    Long count = jdbc.queryForObject("SELECT count(*) FROM " + qualified(schema, table), Long.class);
    return count == null ? 0 : count;
  }

  /**
   * A page of rows, each a column-keyed, insertion-ordered map of stringified values (so JSONB, timestamps and arrays render predictably). Orders by {@code ctid} for stable-enough paging on a
   * read-only view.
   */
  public List<Map<String, Object>> rows(String schema, String table, int limit, int offset) {
    return jdbc.query("SELECT * FROM " + qualified(schema, table) + " ORDER BY ctid LIMIT ? OFFSET ?", (rs, n) -> {
      ResultSetMetaData meta = rs.getMetaData();
      Map<String, Object> row = new LinkedHashMap<>();
      for (int i = 1; i <= meta.getColumnCount(); i++) {
        Object value = rs.getObject(i);
        row.put(meta.getColumnLabel(i), value == null ? null : value.toString());
      }
      return row;
    }, limit, offset);
  }

  // Double-quotes the (already catalog-validated) identifiers, escaping any embedded quotes, e.g. core.patients -> "core"."patients".
  private static String qualified(String schema, String table) {
    return quote(schema) + "." + quote(table);
  }

  private static String quote(String identifier) {
    return '"' + identifier.replace("\"", "\"\"") + '"';
  }
}
