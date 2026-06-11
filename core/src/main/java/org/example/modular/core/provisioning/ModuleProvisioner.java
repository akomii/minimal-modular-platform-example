package org.example.modular.core.provisioning;

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.example.modular.core.module.CoreAccess;
import org.example.modular.core.module.ModuleCatalog;
import org.example.modular.core.module.ModuleDefinition;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class ModuleProvisioner {

  private final ModuleCatalog catalog;
  private final ModuleScriptRunner scripts;
  private final ModuleProvisioningRepository repository;

  public ModuleProvisioner(ModuleCatalog catalog, ModuleScriptRunner scripts, ModuleProvisioningRepository repository) {
    this.catalog = catalog;
    this.scripts = scripts;
    this.repository = repository;
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
    log.info("Provisioning module {} (schema={}, coreAccess={})", module.getId(), db.getSchema(), access);
    String password = UUID.randomUUID().toString();
    scripts.createModuleRole(role, db.getSchema(), password);
    switch (access) {
      case NONE -> { /* own schema only, no core grants */ }
      case READ -> scripts.grantCoreRead(role);
      case WRITE -> {
        scripts.grantCoreRead(role);
        scripts.grantCoreWrite(role);
      }
    }
    runScript(db, role, db.getUp());
    repository.markInstalled(module, password);
  }

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
    runScript(db, role, db.getDown());
    scripts.dropModule(role);
    repository.delete(module.getId());
  }

  private void runScript(ModuleDefinition.Db db, String role, String relativePath) {
    if (relativePath == null || relativePath.isBlank()) {
      return;
    }
    scripts.runScriptAs(role, db.getSchema(), catalog.readScript(relativePath));
  }

  private static String roleName(ModuleDefinition module) {
    return "mod_" + module.getId();
  }
}
