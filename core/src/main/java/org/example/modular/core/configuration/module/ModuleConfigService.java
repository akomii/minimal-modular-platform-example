package org.example.modular.core.configuration.module;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.example.modular.core.configuration.setting.Setting;
import org.example.modular.core.configuration.setting.SettingDTO;
import org.example.modular.core.module.ModuleCatalog;
import org.example.modular.core.module.ModuleDefinition;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages each module's configuration: the values delivered to its container as env vars, seeding manifest defaults at install, the API view of its fields, validated updates, and cleanup on purge.
 */
@Service
public class ModuleConfigService {

  private final ModuleCatalog catalog;
  private final ModuleConfigRepository repository;

  public ModuleConfigService(ModuleCatalog catalog, ModuleConfigRepository repository) {
    this.catalog = catalog;
    this.repository = repository;
  }

  /**
   * The module's config as container env vars: each declared field's stored value, or its manifest default; fields with neither are omitted.
   */
  public Map<String, String> configEnv(ModuleDefinition module) {
    Map<String, String> stored = repository.findValues(module.getId());
    Map<String, String> env = new LinkedHashMap<>();
    module.getConfig().forEach(field -> {
      String value = stored.getOrDefault(field.key(), field.defaultValue());
      if (value != null) {
        env.put(field.key(), value);
      }
    });
    return env;
  }

  /**
   * Persists each declared field's manifest default that has no stored value yet (additive; run at install/upgrade).
   */
  @Transactional
  public void seedDefaults(ModuleDefinition module) {
    Map<String, String> stored = repository.findValues(module.getId());
    module.getConfig().forEach(field -> {
      if (field.defaultValue() != null && !stored.containsKey(field.key())) {
        repository.upsert(module.getId(), field.key(), field.defaultValue());
      }
    });
  }

  /**
   * The module's declared fields each paired with its current value (stored value, or the manifest default), for the API.
   */
  public List<SettingDTO> list(String moduleId) {
    ModuleDefinition module = catalog.byId(moduleId);
    Map<String, String> stored = repository.findValues(moduleId);
    return module.getConfig().stream()
        .map(field -> SettingDTO.of(field, stored))
        .toList();
  }

  /**
   * Validates the submitted values against the module's manifest schema (declared key, type, and required not blank) and persists them. Blank values for optional fields are ignored. Does not
   * redeploy.
   */
  @Transactional
  public void update(String moduleId, Map<String, String> values) {
    ModuleDefinition module = catalog.byId(moduleId);
    Map<String, Setting<?>> declared = module.getConfig().stream()
        .collect(Collectors.toMap(Setting::key, Function.identity()));
    // validate everything before persisting anything, so one bad field can't leave a partial save
    values.forEach((key, value) -> {
      Setting<?> field = declared.get(key);
      if (field == null) {
        throw new IllegalArgumentException("Unknown config key for module " + moduleId + ": " + key);
      }
      if (value == null || value.isBlank()) {
        if (field.required()) {
          throw new IllegalArgumentException(key + " is required");
        }
      } else {
        field.type().validate(key, value);
      }
    });
    // blank values for optional fields are left unchanged
    values.forEach((key, value) -> {
      if (value != null && !value.isBlank()) {
        repository.upsert(moduleId, key, value);
      }
    });
  }

  /**
   * Removes all of the module's stored config values.
   */
  public void purge(String moduleId) {
    repository.deleteAll(moduleId);
  }

  /**
   * Restores the module's config to its manifest defaults by dropping all stored overrides; does not redeploy, so the container keeps its current env until applied.
   */
  public void reset(String moduleId) {
    // restoring defaults and purging on removal both drop every stored override
    purge(moduleId);
  }
}
