package org.example.modular.core.configuration.setting.type;

import org.example.modular.core.configuration.setting.ConfigType;
import org.example.modular.core.configuration.setting.Setting;

public final class BoolSetting extends Setting<Boolean> {

  public BoolSetting(String key, String label, Boolean defaultValue) {
    this(key, label, defaultValue, false);
  }

  public BoolSetting(String key, String label, Boolean defaultValue, boolean required) {
    super(key, label, defaultValue, required);
  }

  @Override
  public ConfigType type() {
    return ConfigType.BOOLEAN;
  }

  @Override
  public Boolean convert(String raw) {
    return Boolean.parseBoolean(raw);
  }
}
