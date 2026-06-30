package org.example.modular.core.provisioning;

import java.util.HashSet;
import java.util.Set;
import org.example.modular.core.module.ModuleDefinition;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * Reads and writes the {@code core.module_provisioning} ledger and records DDL entries in the {@code core.module_core_audit} log of module changes to core.
 */
@Repository
public class ModuleProvisioningRepository {

  private final JdbcTemplate jdbc;

  public ModuleProvisioningRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public void markAuthorized(ModuleDefinition module) {
    jdbc.update("""
        INSERT INTO core.module_provisioning (module_id, core_access, authorized)
        VALUES (?, ?, true)
        ON CONFLICT (module_id) DO UPDATE
        SET authorized = true, core_access = EXCLUDED.core_access
        """, module.getId(), module.getDb().getCoreAccess().toString());
  }

  public void markInstalled(ModuleDefinition module, String dbPassword, String version, int appliedMigrations) {
    jdbc.update("""
        INSERT INTO core.module_provisioning (module_id, core_access, installed_at, installed_version, applied_migrations, db_password)
        VALUES (?, ?, now(), ?, ?, ?)
        ON CONFLICT (module_id) DO UPDATE
        SET installed_at = now(), core_access = EXCLUDED.core_access,
            installed_version = EXCLUDED.installed_version, applied_migrations = EXCLUDED.applied_migrations,
            db_password = EXCLUDED.db_password
        """, module.getId(), module.getDb().getCoreAccess().toString(), version, appliedMigrations, dbPassword);
  }

  public void markUpgraded(String moduleId, String version, int appliedMigrations) {
    jdbc.update("""
        UPDATE core.module_provisioning SET installed_version = ?, applied_migrations = ?
        WHERE module_id = ?
        """, version, appliedMigrations, moduleId);
  }

  /**
   * Records (or refreshes) a module's installed version, creating a minimal ledger row for modules that need no db provisioning. This is what dependency checks read installed versions from.
   */
  public void recordInstalled(String moduleId, String version) {
    jdbc.update("""
        INSERT INTO core.module_provisioning (module_id, core_access, installed_at, installed_version)
        VALUES (?, 'none', now(), ?)
        ON CONFLICT (module_id) DO UPDATE
        SET installed_at = now(), installed_version = EXCLUDED.installed_version
        """, moduleId, version);
  }

  /**
   * Returns the module's installed version, or null if it has no install record.
   */
  public String installedVersion(String moduleId) {
    return queryColumn("installed_version", (rs, n) -> rs.getString(1), moduleId, null);
  }

  public boolean isAuthorized(String moduleId) {
    return queryFlag("authorized", moduleId);
  }

  public boolean isInstalled(String moduleId) {
    return queryFlag("installed_at IS NOT NULL", moduleId);
  }

  /**
   * Returns how many of the module's ordered migrations have already run (0 if not installed).
   */
  public int appliedMigrations(String moduleId) {
    return queryColumn("applied_migrations", (rs, n) -> rs.getInt(1), moduleId, 0);
  }

  public void delete(String moduleId) {
    jdbc.update("DELETE FROM core.module_provisioning WHERE module_id = ?", moduleId);
  }

  public String findPassword(String moduleId) {
    return queryColumn("db_password", (rs, n) -> rs.getString(1), moduleId, "");
  }

  /**
   * Snapshots the tables, columns and indexes currently in the core schema; the provisioner diffs two snapshots to learn what a module's migrations added.
   */
  public Set<CoreObject> coreObjects() {
    return new HashSet<>(jdbc.query("""
        SELECT 'table' AS kind, table_name AS tbl, NULL AS name
          FROM information_schema.tables  WHERE table_schema = 'core' AND table_type = 'BASE TABLE'
        UNION ALL
        SELECT 'column', table_name, column_name
          FROM information_schema.columns WHERE table_schema = 'core'
        UNION ALL
        SELECT 'index', tablename, indexname
          FROM pg_indexes              WHERE schemaname = 'core'
        """, (rs, n) -> new CoreObject(rs.getString("kind"), rs.getString("tbl"), rs.getString("name"))));
  }

  /**
   * Records a DDL change a module made to core (a created table, added column, or created index) in the audit log, so purge can reverse it.
   */
  public void recordCoreDdl(String moduleId, String op, String targetTable, String targetName) {
    jdbc.update("""
        INSERT INTO core.module_core_audit (module_id, category, op, target_table, target_name)
        VALUES (?, 'ddl', ?, ?, ?)
        """, moduleId, op, targetTable, targetName);
  }

  public void deleteAudit(String moduleId) {
    jdbc.update("DELETE FROM core.module_core_audit WHERE module_id = ?", moduleId);
  }

  private boolean queryFlag(String selectExpr, String moduleId) {
    return queryColumn(selectExpr, (rs, n) -> rs.getBoolean(1), moduleId, false);
  }

  /**
   * Reads a single column for one module from the ledger, returning {@code fallback} when the module has no row.
   */
  private <T> T queryColumn(String selectExpr, RowMapper<T> mapper, String moduleId, T fallback) {
    return jdbc.query("SELECT " + selectExpr + " FROM core.module_provisioning WHERE module_id = ?", mapper, moduleId)
        .stream().findFirst().orElse(fallback);
  }
}
