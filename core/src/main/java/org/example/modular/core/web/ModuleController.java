package org.example.modular.core.web;

import java.util.List;
import org.example.modular.core.module.ModuleCatalog;
import org.example.modular.core.module.ModuleDefinition;
import org.example.modular.core.runtime.ModuleRuntime;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/modules")
public class ModuleController {

  private final ModuleCatalog catalog;
  private final ModuleRuntime runtime;

  public ModuleController(ModuleCatalog catalog, ModuleRuntime runtime) {
    this.catalog = catalog;
    this.runtime = runtime;
  }

  @GetMapping
  public List<ModuleDTO> list() {
    return catalog.getModules().stream()
        .map(module -> ModuleDTO.from(module, runtime))
        .toList();
  }

  @PostMapping("/{id}/install")
  public ModuleDTO install(@PathVariable String id) {
    ModuleDefinition module = catalog.byId(id);
    runtime.install(module);
    return ModuleDTO.from(module, runtime);
  }

  @PostMapping("/{id}/start")
  public ModuleDTO start(@PathVariable String id) {
    ModuleDefinition module = catalog.byId(id);
    runtime.start(module);
    return ModuleDTO.from(module, runtime);
  }

  @PostMapping("/{id}/stop")
  public ModuleDTO stop(@PathVariable String id) {
    ModuleDefinition module = catalog.byId(id);
    runtime.stop(module);
    return ModuleDTO.from(module, runtime);
  }

  @DeleteMapping("/{id}")
  public ModuleDTO remove(@PathVariable String id) {
    ModuleDefinition module = catalog.byId(id);
    runtime.remove(module);
    return ModuleDTO.from(module, runtime);
  }
}
