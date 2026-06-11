package org.example.modular.core.module;

import java.util.List;
import org.example.modular.core.provisioning.ModuleProvisioner;
import org.example.modular.core.runtime.ModuleRuntime;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/modules")
public class ModuleController {

  private final ModuleCatalog catalog;
  private final ModuleRuntime runtime;
  private final ModuleProvisioner provisioner;

  public ModuleController(ModuleCatalog catalog, ModuleRuntime runtime, ModuleProvisioner provisioner) {
    this.catalog = catalog;
    this.runtime = runtime;
    this.provisioner = provisioner;
  }

  @GetMapping
  public List<ModuleDTO> list() {
    return catalog.getModules().stream().map(module -> ModuleDTO.from(module, runtime.status(module), provisioner.isAuthorized(module))).toList();
  }

  @PostMapping("/{id}/install")
  public ModuleDTO install(@PathVariable String id) {
    ModuleDefinition module = catalog.byId(id);
    provisioner.provision(module);
    runtime.install(module);
    return ModuleDTO.from(module, runtime.status(module), provisioner.isAuthorized(module));
  }

  @PostMapping("/{id}/authorize")
  public ModuleDTO authorize(@PathVariable String id) {
    ModuleDefinition module = catalog.byId(id);
    provisioner.authorize(module);
    return ModuleDTO.from(module, runtime.status(module), provisioner.isAuthorized(module));
  }

  @PostMapping("/{id}/start")
  public ModuleDTO start(@PathVariable String id) {
    ModuleDefinition module = catalog.byId(id);
    runtime.start(module);
    return ModuleDTO.from(module, runtime.status(module), provisioner.isAuthorized(module));
  }

  @PostMapping("/{id}/stop")
  public ModuleDTO stop(@PathVariable String id) {
    ModuleDefinition module = catalog.byId(id);
    runtime.stop(module);
    return ModuleDTO.from(module, runtime.status(module), provisioner.isAuthorized(module));
  }

  @DeleteMapping("/{id}")
  public ModuleDTO remove(@PathVariable String id, @RequestParam(defaultValue = "false") boolean purge) {
    ModuleDefinition module = catalog.byId(id);
    if (purge) {
      provisioner.purge(module);
    }
    runtime.remove(module);
    return ModuleDTO.from(module, runtime.status(module), provisioner.isAuthorized(module));
  }

  @GetMapping(value = "/{id}/logs", produces = "text/plain")
  public String getLogs(@PathVariable String id) {
    ModuleDefinition module = catalog.byId(id);
    return runtime.getLogs(module);
  }
}
