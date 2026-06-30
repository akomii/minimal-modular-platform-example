package org.example.modular.core.configuration.core;

import jakarta.annotation.PostConstruct;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.example.modular.core.configuration.setting.CoreSettingsContributor;
import org.example.modular.core.configuration.setting.Setting;
import org.example.modular.core.configuration.setting.SettingDTO;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

/**
 * Generic store for the core live-config settings. Each feature exposes its own typed settings via a {@link CoreSettingsContributor} (declared next to the code that uses them), so this service never
 * enumerates which keys exist — it only validates, persists and serves them. Values load from {@code core.config} at startup and are read on each use, so an update takes effect without a restart.
 */
@Service
// the startup load reads core.config, so wait for Flyway to create it first
@DependsOn("flywayInitializer")
public class CoreConfigService {

  private final CoreConfigRepository repository;
  /**
   * Contributed settings keyed by key, in declaration order; the set of keys the UI may change.
   */
  private final Map<String, Setting<?>> registry;
  private final Map<String, String> values = new ConcurrentHashMap<>();

  public CoreConfigService(CoreConfigRepository repository, List<CoreSettingsContributor> contributors) {
    this.repository = repository;
    this.registry = contributors.stream()
        .flatMap(contributor -> contributor.coreSettings().stream())
        .collect(Collectors.toMap(Setting::key, Function.identity(), (a, b) -> a, LinkedHashMap::new));
  }

  /**
   * Loads the stored values from {@code core.config} at startup, so a saved setting survives a restart.
   */
  @PostConstruct
  void load() {
    values.putAll(repository.findAll());
  }

  /**
   * The current value of a setting in its declared Java type (stored value, or the setting's default).
   */
  public <T> T get(Setting<T> setting) {
    return setting.convert(current(setting));
  }

  /**
   * Every contributed setting paired with its current value, for the API.
   */
  public List<SettingDTO> list() {
    return registry.values().stream()
        .map(setting -> new SettingDTO(setting.key(), setting.label(), setting.type(), setting.required(), setting.defaultValue(), current(setting)))
        .toList();
  }

  /**
   * Validates each submitted value against its setting's type and persists them, taking effect immediately. Validates all before persisting any, so one bad value can't leave a partial save.
   */
  public void update(Map<String, String> updates) {
    // validate everything before persisting anything, so one bad value can't leave a partial save
    updates.forEach((key, value) -> {
      Setting<?> setting = registry.get(key);
      if (setting == null) {
        throw new IllegalArgumentException("Unknown core setting: " + key);
      }
      setting.type().validate(key, value);
    });
    updates.forEach((key, value) -> {
      repository.upsert(key, value);
      values.put(key, value);
    });
  }

  /**
   * Restores every core setting to its declared default by dropping all stored overrides; takes effect immediately, like an update.
   */
  public void reset() {
    repository.deleteAll();
    values.clear();
  }

  /**
   * The setting's stored string value, falling back to its default if no row is stored.
   */
  private String current(Setting<?> setting) {
    return values.getOrDefault(setting.key(), setting.defaultValue());
  }
}
