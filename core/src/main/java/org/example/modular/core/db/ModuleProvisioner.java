package org.example.modular.core.db;

import lombok.extern.slf4j.Slf4j;
import org.example.modular.core.module.ModuleDefinition;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class ModuleProvisioner {

  private final ModuleScriptRunner scripts;
  private final ModuleProvisioningRepository repository;

  public ModuleProvisioner(ModuleScriptRunner scripts, ModuleProvisioningRepository repository) {
    this.scripts = scripts;
    this.repository = repository;
  }

  @Transactional
  public void authorize(ModuleDefinition module) {
    ModuleDefinition.Db db = module.getDb();
    if (db == null || !"write".equals(db.getCoreAccess())) {
      throw new IllegalArgumentException("Module does not request core write access: " + module.getId());
    }
    log.info("Authorizing core write access for module {}", module.getId());
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
    String role = roleName(module);
    String access = db.getCoreAccess();
    log.info("Provisioning module {} (schema={}, coreAccess={})", module.getId(), db.getSchema(), access);
    scripts.createModuleRole(role, db.getSchema());
    if ("read".equals(access) || "write".equals(access)) {
      scripts.grantCoreRead(role);
    }
    if ("write".equals(access) && repository.isAuthorized(module.getId())) {
      scripts.grantCoreWrite(role);
    }
    repository.markInstalled(module);
    scripts.runScriptAs(role, db.getSchema(), db.getUp());
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
    scripts.runScriptAs(role, db.getSchema(), db.getDown());
    scripts.dropModule(role);
    repository.delete(module.getId());
  }

  private static String roleName(ModuleDefinition module) {
    return "mod_" + module.getId();
  }
}
