package org.example.modular.core.configuration.setting.type;

import org.example.modular.core.configuration.setting.ConfigType;
import org.example.modular.core.configuration.setting.Setting;

public final class IntSetting extends Setting<Integer> {

  public IntSetting(String key, String label, Integer defaultValue) {
    this(key, label, defaultValue, false);
  }

  public IntSetting(String key, String label, Integer defaultValue, boolean required) {
    super(key, label, defaultValue, required);
  }

  @Override
  public ConfigType type() {
    return ConfigType.NUMBER;
  }

  @Override
  public Integer convert(String raw) {
    return Integer.parseInt(raw);
  }
}
