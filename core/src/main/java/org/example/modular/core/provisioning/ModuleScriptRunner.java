package org.example.modular.core.provisioning;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(propagation = Propagation.MANDATORY)
public class ModuleScriptRunner {

  private final JdbcTemplate jdbc;

  public ModuleScriptRunner(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public void createModuleRole(String role, String password) {
    Integer count = jdbc.queryForObject("SELECT count(*) FROM pg_roles WHERE rolname = ?", Integer.class, role);
    if (count == null || count == 0) {
      jdbc.execute("CREATE ROLE " + quoteIdent(role) + " LOGIN PASSWORD " + quoteLiteral(password));
    } else {
      jdbc.execute("ALTER ROLE " + quoteIdent(role) + " WITH LOGIN PASSWORD " + quoteLiteral(password));
    }
  }

  public void createModuleSchema(String schema, String role) {
    jdbc.execute("CREATE SCHEMA " + quoteIdent(schema) + " AUTHORIZATION " + quoteIdent(role));
  }

  public void grantCoreRead(String role) {
    jdbc.execute("GRANT USAGE ON SCHEMA core TO " + quoteIdent(role));
    jdbc.execute("GRANT SELECT ON ALL TABLES IN SCHEMA core TO " + quoteIdent(role));
  }

  public void grantCoreWrite(String role) {
    jdbc.execute("GRANT core_owner TO " + quoteIdent(role));
  }

  public void runScriptAs(String role, String schema, String sql) {
    jdbc.execute("SET LOCAL ROLE " + quoteIdent(role));
    String searchPath = schema == null || schema.isBlank() ? "core" : quoteIdent(schema) + ", core";
    jdbc.execute("SET LOCAL search_path TO " + searchPath);
    jdbc.execute(sql);
    // restore the core role; SET LOCAL would otherwise keep later statements in the same
    // transaction running as the module role
    jdbc.execute("RESET ROLE");
  }

  /**
   * Replays the module's audit log to undo its core writes, running as the module role (still a core_owner member) so it owns the changes it reverts.
   */
  public void undoCoreWritesAs(String role, String moduleId) {
    jdbc.execute("SET LOCAL ROLE " + quoteIdent(role));
    jdbc.queryForObject("SELECT core.undo_module_writes(?)", (rs, n) -> null, moduleId);
    jdbc.execute("RESET ROLE");
  }

  public void dropModule(String role) {
    jdbc.execute("RESET ROLE");
    jdbc.execute("DROP OWNED BY " + quoteIdent(role) + " CASCADE");
    jdbc.execute("DROP ROLE IF EXISTS " + quoteIdent(role));
  }

  private static String quoteIdent(String ident) {
    return "\"" + ident.replace("\"", "\"\"") + "\"";
  }

  private static String quoteLiteral(String value) {
    return "'" + value.replace("'", "''") + "'";
  }
}
