package org.example.modular.core.provisioning;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ModuleScriptRunner {

  private final JdbcTemplate jdbc;
  private final String moduleDir;

  public ModuleScriptRunner(JdbcTemplate jdbc, @Value("${module.dir}") String moduleDir) {
    this.jdbc = jdbc;
    this.moduleDir = moduleDir;
  }

  public void createModuleRole(String role, String schema) {
    Integer count = jdbc.queryForObject("SELECT count(*) FROM pg_roles WHERE rolname = ?", Integer.class, role);
    if (count == null || count == 0) {
      jdbc.execute("CREATE ROLE " + quoteIdent(role) + " NOLOGIN");
    }
    jdbc.execute("CREATE SCHEMA " + quoteIdent(schema) + " AUTHORIZATION " + quoteIdent(role));
  }

  public void grantCoreRead(String role) {
    jdbc.execute("GRANT USAGE ON SCHEMA core TO " + quoteIdent(role));
    jdbc.execute("GRANT SELECT ON ALL TABLES IN SCHEMA core TO " + quoteIdent(role));
  }

  public void grantCoreWrite(String role) {
    jdbc.execute("GRANT core_owner TO " + quoteIdent(role));
  }

  public void runScriptAs(String role, String schema, String relativePath) {
    if (relativePath == null || relativePath.isBlank()) {
      return;
    }
    Path file = Paths.get(moduleDir, relativePath);
    String sql;
    try {
      sql = Files.readString(file);
    } catch (Exception e) {
      throw new RuntimeException("Failed to read SQL script: " + file, e);
    }
    jdbc.execute("SET LOCAL ROLE " + quoteIdent(role));
    jdbc.execute("SET LOCAL search_path TO " + quoteIdent(schema) + ", core");
    jdbc.execute(sql);
  }

  public void dropModule(String role) {
    jdbc.execute("RESET ROLE");
    jdbc.execute("DROP OWNED BY " + quoteIdent(role) + " CASCADE");
    jdbc.execute("DROP ROLE IF EXISTS " + quoteIdent(role));
  }

  private static String quoteIdent(String ident) {
    return "\"" + ident.replace("\"", "\"\"") + "\"";
  }
}
