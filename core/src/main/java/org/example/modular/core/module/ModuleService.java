package org.example.modular.core.module;

import java.io.Closeable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.example.modular.core.dependency.ModuleDependencyChecker;
import org.example.modular.core.idp.IdpProvisioner;
import org.example.modular.core.provisioning.ModuleProvisioner;
import org.example.modular.core.runtime.LogSink;
import org.example.modular.core.runtime.ModuleRuntime;
import org.springframework.stereotype.Service;

/**
 * Orchestrates the module lifecycle for the API, tying together the catalog, container runtime, and the database and identity provisioners.
 */
@Service
public class ModuleService {

  private final ModuleCatalog catalog;
  private final ModuleRuntime runtime;
  private final ModuleProvisioner provisioner;
  private final IdpProvisioner idpProvisioner;
  private final ModuleDependencyChecker dependencyChecker;

  public ModuleService(ModuleCatalog catalog, ModuleRuntime runtime, ModuleProvisioner provisioner, IdpProvisioner idpProvisioner,
      ModuleDependencyChecker dependencyChecker) {
    this.catalog = catalog;
    this.runtime = runtime;
    this.provisioner = provisioner;
    this.idpProvisioner = idpProvisioner;
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
    Map<String, String> env = new LinkedHashMap<>(provisioner.dbEnv(module));
    env.putAll(idpProvisioner.provision(module));
    runtime.install(module, env);
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
    Map<String, String> env = new LinkedHashMap<>(provisioner.dbEnv(module));
    env.putAll(idpProvisioner.provision(module));
    runtime.remove(module);
    runtime.install(module, env);
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
      provisioner.removeInstalled(module.getId());
    }
    runtime.remove(module);
    return state(module);
  }

  public Closeable streamLogs(String id, LogSink sink) {
    return runtime.streamLogs(catalog.byId(id), sink);
  }

  private ModuleDTO state(ModuleDefinition module) {
    return ModuleDTO.from(module, runtime.status(module), provisioner.isAuthorized(module));
  }
}
