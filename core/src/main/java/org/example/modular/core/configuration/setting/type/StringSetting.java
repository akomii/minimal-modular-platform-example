package org.example.modular.core.configuration.setting.type;

import org.example.modular.core.configuration.setting.ConfigType;
import org.example.modular.core.configuration.setting.Setting;

public final class StringSetting extends Setting<String> {

  public StringSetting(String key, String label, String defaultValue) {
    this(key, label, defaultValue, false);
  }

  public StringSetting(String key, String label, String defaultValue, boolean required) {
    super(key, label, defaultValue, required);
  }

  @Override
  public ConfigType type() {
    return ConfigType.STRING;
  }

  @Override
  public String convert(String raw) {
    return raw;
  }
}
