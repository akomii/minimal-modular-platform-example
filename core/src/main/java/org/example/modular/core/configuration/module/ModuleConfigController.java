package org.example.modular.core.configuration.module;

import java.util.List;
import java.util.Map;
import org.example.modular.core.configuration.setting.SettingDTO;
import org.example.modular.core.module.ModuleDTO;
import org.example.modular.core.module.ModuleService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin-only REST API for a module's configuration: read and persist its declared fields, and apply the saved values by recreating the container. Lives under {@code /api/modules/**}, which is already
 * admin-gated. Reading and saving go through {@link ModuleConfigService}; applying is a container-lifecycle action, so it delegates to {@link ModuleService}.
 */
@RestController
@RequestMapping("/api/modules/{id}/config")
public class ModuleConfigController {

  private final ModuleConfigService configService;
  private final ModuleService moduleService;

  public ModuleConfigController(ModuleConfigService configService, ModuleService moduleService) {
    this.configService = configService;
    this.moduleService = moduleService;
  }

  /**
   * The module's declared config fields with their current values.
   */
  @GetMapping
  public List<SettingDTO> list(@PathVariable String id) {
    return configService.list(id);
  }

  /**
   * Persists submitted config values (validated against the manifest); does not restart the module.
   */
  @PutMapping
  public List<SettingDTO> update(@PathVariable String id, @RequestBody Map<String, String> values) {
    configService.update(id, values);
    return configService.list(id);
  }

  /**
   * Restores the module's config to its manifest defaults (persisted only; the module keeps running until Apply) and returns the refreshed list.
   */
  @DeleteMapping
  public List<SettingDTO> reset(@PathVariable String id) {
    configService.reset(id);
    return configService.list(id);
  }

  /**
   * Applies the persisted config by recreating the container with refreshed env.
   */
  @PostMapping("/apply")
  public ModuleDTO apply(@PathVariable String id) {
    return moduleService.applyConfig(id);
  }
}
