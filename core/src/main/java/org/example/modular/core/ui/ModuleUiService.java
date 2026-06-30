package org.example.modular.core.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.example.modular.core.idp.IdpUserAdmin;
import org.example.modular.core.idp.IdpUserAdmin.RoleRef;
import org.example.modular.core.module.ModuleCatalog;
import org.example.modular.core.module.ModuleDTO;
import org.example.modular.core.module.ModuleDefinition;
import org.example.modular.core.module.ModuleService;
import org.example.modular.core.runtime.ModuleStatus;
import org.springframework.stereotype.Service;

/**
 * Builds the list of module UI pages a given user may see: a page is shown when its module is running, declares a {@code ui} block, and the user holds one of that module's client roles.
 */
@Service
public class ModuleUiService {

  private final ModuleCatalog catalog;
  private final ModuleService moduleService;
  private final IdpUserAdmin userAdmin;

  public ModuleUiService(ModuleCatalog catalog, ModuleService moduleService, IdpUserAdmin userAdmin) {
    this.catalog = catalog;
    this.moduleService = moduleService;
    this.userAdmin = userAdmin;
  }

  public List<ModuleUiDTO> visibleTo(String userId) {
    // modules whose client roles the user holds (a module's OAuth client is named after the module)
    Set<String> permittedModules = userAdmin.rolesOf(userId).stream()
        .map(RoleRef::module)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
    List<ModuleUiDTO> tabs = new ArrayList<>();
    for (ModuleDTO module : moduleService.list()) {
      if (module.status() != ModuleStatus.RUNNING || !permittedModules.contains(module.id()) || module.ports().isEmpty()) {
        continue;
      }
      ModuleDefinition definition = catalog.byId(module.id());
      // the module is reachable from the browser at the host side of its first published port mapping
      String hostPort = module.ports().get(0).split(":")[0];
      definition.getUi().forEach(page ->
          tabs.add(new ModuleUiDTO(module.id(), page.getName(), "http://localhost:" + hostPort + page.getPath())));
    }
    return tabs;
  }
}
