package org.example.modular.core.module;

import java.io.Closeable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.example.modular.core.configuration.module.ModuleConfigService;
import org.example.modular.core.dependency.ModuleDependencyChecker;
import org.example.modular.core.idp.IdpProvisioner;
import org.example.modular.core.provisioning.ModuleProvisioner;
import org.example.modular.core.runtime.InvalidModuleStateException;
import org.example.modular.core.runtime.LogSink;
import org.example.modular.core.runtime.ModuleRuntime;
import org.example.modular.core.runtime.ModuleStatus;
import org.springframework.stereotype.Service;

/**
 * Orchestrates the module lifecycle for the API, tying together the catalog, container runtime, and the database, identity and configuration provisioners.
 */
@Service
public class ModuleService {

  private final ModuleCatalog catalog;
  private final ModuleRuntime runtime;
  private final ModuleProvisioner provisioner;
  private final IdpProvisioner idpProvisioner;
  private final ModuleConfigService moduleConfig;
  private final ModuleDependencyChecker dependencyChecker;

  public ModuleService(ModuleCatalog catalog, ModuleRuntime runtime, ModuleProvisioner provisioner, IdpProvisioner idpProvisioner,
      ModuleConfigService moduleConfig, ModuleDependencyChecker dependencyChecker) {
    this.catalog = catalog;
    this.runtime = runtime;
    this.provisioner = provisioner;
    this.idpProvisioner = idpProvisioner;
    this.moduleConfig = moduleConfig;
    this.dependencyChecker = dependencyChecker;
  }

  public List<ModuleDTO> list() {
    return catalog.getModules().stream().map(this::state).toList();
  }

  /**
   * Provisions the module's database and identity resources, then creates its container with the resulting connection/credential env vars.
   */
  public ModuleDTO install(String id) {
    ModuleDefinition module = catalog.byId(id);
    dependencyChecker.requireDependenciesInstalled(module);
    provisioner.provision(module);
    moduleConfig.seedDefaults(module);
    runtime.install(module, fullEnv(module));
    provisioner.recordInstalled(module);
    return state(module);
  }

  public ModuleDTO authorize(String id) {
    ModuleDefinition module = catalog.byId(id);
    provisioner.authorize(module);
    return state(module);
  }

  /**
   * Brings the module to its catalog version: runs pending DB migrations, additively reconciles its identity resources, and redeploys the container on the new image with refreshed env.
   */
  public ModuleDTO upgrade(String id) {
    ModuleDefinition module = catalog.byId(id);
    provisioner.upgrade(module);
    moduleConfig.seedDefaults(module);
    runtime.remove(module);
    runtime.install(module, fullEnv(module));
    provisioner.recordInstalled(module);
    return state(module);
  }

  public ModuleDTO start(String id) {
    ModuleDefinition module = catalog.byId(id);
    runtime.start(module);
    return state(module);
  }

  public ModuleDTO stop(String id) {
    ModuleDefinition module = catalog.byId(id);
    runtime.stop(module);
    return state(module);
  }

  public ModuleDTO remove(String id, boolean purge) {
    ModuleDefinition module = catalog.byId(id);
    dependencyChecker.requireNoInstalledDependents(module);
    if (purge) {
      provisioner.purge(module);
      idpProvisioner.purge(module);
      moduleConfig.purge(module.getId());
      provisioner.removeInstalled(module.getId());
    }
    runtime.remove(module);
    return state(module);
  }

  /**
   * Applies the module's current config by recreating its container with refreshed env, preserving its run state (a running module is started again; a stopped one stays stopped). Triggered by the
   * configuration API; the config values themselves are managed by the configuration package.
   */
  public ModuleDTO applyConfig(String id) {
    ModuleDefinition module = catalog.byId(id);
    ModuleStatus before = runtime.status(module);
    if (before == ModuleStatus.NOT_CREATED) {
      throw new InvalidModuleStateException("Module is not installed: " + id);
    }
    runtime.reconfigure(module, fullEnv(module));
    if (before == ModuleStatus.RUNNING) {
      runtime.start(module);
    }
    return state(module);
  }

  public Closeable streamLogs(String id, LogSink sink) {
    return runtime.streamLogs(catalog.byId(id), sink);
  }

  /**
   * The full container env for (re)creating a module: its database connection vars, freshly provisioned identity vars, and its configured values.
   */
  private Map<String, String> fullEnv(ModuleDefinition module) {
    Map<String, String> env = new LinkedHashMap<>(provisioner.dbEnv(module));
    env.putAll(idpProvisioner.provision(module));
    env.putAll(moduleConfig.configEnv(module));
    return env;
  }

  private ModuleDTO state(ModuleDefinition module) {
    return ModuleDTO.from(module, runtime.status(module), provisioner.isAuthorized(module));
  }
}
