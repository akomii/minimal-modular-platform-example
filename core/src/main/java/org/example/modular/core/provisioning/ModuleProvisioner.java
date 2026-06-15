package org.example.modular.core.provisioning;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.example.modular.core.module.CoreAccess;
import org.example.modular.core.module.ModuleCatalog;
import org.example.modular.core.module.ModuleDefinition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Provisions, upgrades and tears down a module's database resources: its login role, own schema, granted core access, ordered migrations, and the ledger of core-table columns it adds.
 */
@Slf4j
@Service
public class ModuleProvisioner {

  private final ModuleCatalog catalog;
  private final ModuleScriptRunner scripts;
  private final ModuleProvisioningRepository repository;
  private final String dbHost;
  private final String dbPort;
  private final String dbName;

  public ModuleProvisioner(ModuleCatalog catalog, ModuleScriptRunner scripts, ModuleProvisioningRepository repository,
      @Value("${modules.db-host}") String dbHost, @Value("${modules.db-port}") String dbPort, @Value("${modules.db-name}") String dbName) {
    this.catalog = catalog;
    this.scripts = scripts;
    this.repository = repository;
    this.dbHost = dbHost;
    this.dbPort = dbPort;
    this.dbName = dbName;
  }

  /**
   * Returns the {@code MODULE_DB_*} connection env vars for the module's container, or empty if it declares no database.
   */
  public Map<String, String> dbEnv(ModuleDefinition module) {
    if (module.getDb() == null) {
      return Map.of();
    }
    return Map.of(
        "MODULE_DB_HOST", dbHost,
        "MODULE_DB_PORT", dbPort,
        "MODULE_DB_NAME", dbName,
        "MODULE_DB_USER", roleName(module),
        "MODULE_DB_PASSWORD", repository.findPassword(module.getId()));
  }

  @Transactional
  public void authorize(ModuleDefinition module) {
    ModuleDefinition.Db db = module.getDb();
    if (db == null || db.getCoreAccess() == CoreAccess.NONE) {
      throw new IllegalArgumentException("Module does not request core access: " + module.getId());
    }
    log.info("Authorizing core {} access for module {}", db.getCoreAccess(), module.getId());
    repository.markAuthorized(module);
  }

  public boolean isAuthorized(ModuleDefinition module) {
    return repository.isAuthorized(module.getId());
  }

  /**
   * First install: creates the module's role and schema, grants the requested core access, runs all migrations, then records the installed version and any core changes the migrations made.
   */
  @Transactional
  public void provision(ModuleDefinition module) {
    ModuleDefinition.Db db = module.getDb();
    if (db == null) {
      log.debug("Module {} declares no database needs", module.getId());
      return;
    }
    if (repository.isInstalled(module.getId())) {
      log.info("Module {} is already provisioned; reusing existing schema and data", module.getId());
      return;
    }
    CoreAccess access = db.getCoreAccess();
    if (access != CoreAccess.NONE && !repository.isAuthorized(module.getId())) {
      throw new ModuleNotAuthorizedException("Module not authorized for core " + access + " access: " + module.getId());
    }
    String role = roleName(module);
    log.info("Provisioning module {} (ownSchema={}, coreAccess={})", module.getId(), db.isOwnSchema(), access);
    String password = UUID.randomUUID().toString();
    scripts.createModuleRole(role, password);
    if (db.isOwnSchema()) {
      // the schema shares the module's "mod_<id>" name and is owned by its role
      scripts.createModuleSchema(role, role);
    }
    switch (access) {
      case NONE -> { /* own schema only, no core grants */ }
      case READ -> scripts.grantCoreRead(role);
      case WRITE -> {
        scripts.grantCoreRead(role);
        scripts.grantCoreWrite(role);
      }
    }
    Set<CoreObject> before = repository.coreObjects();
    runMigrations(db, role, db.getMigrations());
    repository.markInstalled(module, password, module.getVersion(), db.getMigrations().size());
    recordCoreChanges(module.getId(), before);
  }

  /**
   * Runs only the migrations added since the last install/upgrade, records any new core changes, and bumps the recorded version. Assumes the migrations list is append-only.
   */
  @Transactional
  public void upgrade(ModuleDefinition module) {
    ModuleDefinition.Db db = module.getDb();
    if (db == null) {
      return;
    }
    if (!repository.isInstalled(module.getId())) {
      throw new IllegalStateException("Module is not installed; cannot upgrade: " + module.getId());
    }
    int applied = repository.appliedMigrations(module.getId());
    List<String> migrations = db.getMigrations();
    List<String> pending = applied >= migrations.size() ? List.of() : migrations.subList(applied, migrations.size());
    if (!pending.isEmpty()) {
      String role = roleName(module);
      log.info("Upgrading module {}: running {} pending migration(s)", module.getId(), pending.size());
      Set<CoreObject> before = repository.coreObjects();
      runMigrations(db, role, pending);
      recordCoreChanges(module.getId(), before);
    }
    repository.markUpgraded(module.getId(), module.getVersion(), migrations.size());
  }

  /**
   * Replays the module's core audit log to undo its core writes (as the module role, which still holds core_owner), then drops the module's schema and role and clears its records.
   */
  @Transactional
  public void purge(ModuleDefinition module) {
    ModuleDefinition.Db db = module.getDb();
    if (db == null) {
      return;
    }
    if (!repository.isInstalled(module.getId())) {
      log.warn("Module {} is not installed; nothing to purge", module.getId());
      return;
    }
    String role = roleName(module);
    log.info("Purging database for module {}", module.getId());
    if (db.getCoreAccess() != CoreAccess.NONE) {
      // only core-access modules can have audit entries (and SELECT on the audit log to replay them)
      scripts.undoCoreWritesAs(role, module.getId());
    }
    repository.deleteAudit(module.getId());
    scripts.dropModule(role);
    repository.delete(module.getId());
  }

  private void runMigrations(ModuleDefinition.Db db, String role, List<String> migrations) {
    String schema = db.isOwnSchema() ? role : null;
    migrations.forEach(path -> scripts.runScriptAs(role, schema, catalog.readScript(path)));
  }

  /**
   * Records the core objects (tables, columns, indexes) that appeared since the {@code before} snapshot as DDL entries in the audit log, so purge can reverse them. Objects on a table the module just
   * created are skipped — dropping the table covers them.
   */
  private void recordCoreChanges(String moduleId, Set<CoreObject> before) {
    Set<CoreObject> added = repository.coreObjects();
    added.removeAll(before);
    Set<String> newTables = added.stream()
        .filter(object -> object.kind().equals("table"))
        .map(CoreObject::table)
        .collect(Collectors.toSet());
    added.stream()
        .filter(object -> object.kind().equals("table") || !newTables.contains(object.table()))
        .forEach(object -> repository.recordCoreDdl(moduleId, ddlOp(object.kind()), object.table(), object.name()));
  }

  private static String ddlOp(String kind) {
    return switch (kind) {
      case "table" -> "create_table";
      case "column" -> "add_column";
      case "index" -> "create_index";
      default -> throw new IllegalStateException("Unknown core object kind: " + kind);
    };
  }

  private static String roleName(ModuleDefinition module) {
    return "mod_" + module.getId();
  }
}
