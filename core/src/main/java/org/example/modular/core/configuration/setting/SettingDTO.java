package org.example.modular.core.configuration.setting;

import java.util.Map;

/**
 * API view of a configuration setting (core setting or module field): its declaration plus its current value (stored value, or the default). Secret values are returned as-is.
 */
public record SettingDTO(String key, String label, ConfigType type, boolean required, String defaultValue, String value) {

  /**
   * Builds the API view of a setting from its declaration and the stored values, taking the current value from {@code stored} or falling back to the setting's default when none is stored.
   */
  public static SettingDTO of(Setting<?> setting, Map<String, String> stored) {
    return new SettingDTO(setting.key(), setting.label(), setting.type(), setting.required(),
        setting.defaultValue(), stored.getOrDefault(setting.key(), setting.defaultValue()));
  }
}
