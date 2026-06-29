package org.example.modular.core.configuration.setting.type;

import org.example.modular.core.configuration.setting.ConfigType;
import org.example.modular.core.configuration.setting.Setting;

/**
 * A secret string setting: stored and converted like a {@link StringSetting}, but typed {@link ConfigType#SECRET} so the UI renders it masked.
 */
public final class SecretSetting extends Setting<String> {

  public SecretSetting(String key, String label, String defaultValue) {
    this(key, label, defaultValue, false);
  }

  public SecretSetting(String key, String label, String defaultValue, boolean required) {
    super(key, label, defaultValue, required);
  }

  @Override
  public ConfigType type() {
    return ConfigType.SECRET;
  }

  @Override
  public String convert(String raw) {
    return raw;
  }
}
