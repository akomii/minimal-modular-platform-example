package org.example.modular.core.db;

import java.util.List;
import org.example.modular.core.module.ModuleDefinition;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ModuleProvisioningRepository {

  private final JdbcTemplate jdbc;

  public ModuleProvisioningRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public void markAuthorized(ModuleDefinition module) {
    jdbc.update(
        "INSERT INTO core.module_provisioning (module_id, schema_name, core_access, authorized) VALUES (?, ?, ?, true) ON CONFLICT (module_id) DO UPDATE SET authorized = true, "
            + "schema_name = EXCLUDED.schema_name, core_access = EXCLUDED.core_access", module.getId(), module.getDb().getSchema(), module.getDb().getCoreAccess());
  }

  public void markInstalled(ModuleDefinition module) {
    jdbc.update(
        "INSERT INTO core.module_provisioning (module_id, schema_name, core_access, installed_at) VALUES (?, ?, ?, now()) ON CONFLICT (module_id) DO UPDATE SET installed_at = now(), "
            + "schema_name = EXCLUDED.schema_name, core_access = EXCLUDED.core_access", module.getId(), module.getDb().getSchema(), module.getDb().getCoreAccess());
  }

  public boolean isAuthorized(String moduleId) {
    return queryFlag("authorized", moduleId);
  }

  public boolean isInstalled(String moduleId) {
    return queryFlag("installed_at IS NOT NULL", moduleId);
  }

  public void delete(String moduleId) {
    jdbc.update("DELETE FROM core.module_provisioning WHERE module_id = ?", moduleId);
  }

  private boolean queryFlag(String selectExpr, String moduleId) {
    List<Boolean> rows = jdbc.query("SELECT " + selectExpr + " FROM core.module_provisioning WHERE module_id = ?", (rs, n) -> rs.getBoolean(1), moduleId);
    return rows.stream().findFirst().orElse(false);
  }
}
