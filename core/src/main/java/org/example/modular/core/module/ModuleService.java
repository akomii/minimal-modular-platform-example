package org.example.modular.core.module;

import java.util.List;
import org.example.modular.core.provisioning.ModuleProvisioner;
import org.example.modular.core.runtime.ModuleRuntime;
import org.springframework.stereotype.Service;

@Service
public class ModuleService {

  private final ModuleCatalog catalog;
  private final ModuleRuntime runtime;
  private final ModuleProvisioner provisioner;

  public ModuleService(ModuleCatalog catalog, ModuleRuntime runtime, ModuleProvisioner provisioner) {
    this.catalog = catalog;
    this.runtime = runtime;
    this.provisioner = provisioner;
  }

  public List<ModuleDTO> list() {
    return catalog.getModules().stream().map(this::state).toList();
  }

  public ModuleDTO install(String id) {
    ModuleDefinition module = catalog.byId(id);
    provisioner.provision(module);
    runtime.install(module);
    return state(module);
  }

  public ModuleDTO authorize(String id) {
    ModuleDefinition module = catalog.byId(id);
    provisioner.authorize(module);
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
    if (purge) {
      provisioner.purge(module);
    }
    runtime.remove(module);
    return state(module);
  }

  public String logs(String id) {
    return runtime.getLogs(catalog.byId(id));
  }

  private ModuleDTO state(ModuleDefinition module) {
    return ModuleDTO.from(module, runtime.status(module), provisioner.isAuthorized(module));
  }
}
