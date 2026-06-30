package org.example.modular.core.configuration.setting;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.example.modular.core.configuration.setting.type.BoolSetting;
import org.example.modular.core.configuration.setting.type.IntSetting;
import org.example.modular.core.configuration.setting.type.SecretSetting;
import org.example.modular.core.configuration.setting.type.StringSetting;

/**
 * A typed configuration setting shared by the core live-config registry and module manifests: its key, UI label, default, value {@link ConfigType}, whether a value is required, and how to parse its
 * stored string into the typed value. Subclasses ({@link IntSetting}, {@link BoolSetting}, {@link StringSetting}, {@link SecretSetting}) fix the type, so a consumer reads a value with
 * {@code config.get(setting)} and gets the right Java type back. Core features declare settings as typed constants; module manifest entries are deserialized into the matching subclass by
 * {@link #fromManifest}.
 */
public abstract class Setting<T> {

  private final String key;
  private final String label;
  private final T defaultValue;
  private final boolean required;

  protected Setting(String key, String label, T defaultValue, boolean required) {
    this.key = key;
    this.label = label;
    this.defaultValue = defaultValue;
    this.required = required;
  }

  public String key() {
    return key;
  }

  public String label() {
    return label;
  }

  /**
   * The default value in its stored (string) form, or {@code null} when the setting declares none; values are always stored as text.
   */
  public String defaultValue() {
    return defaultValue == null ? null : String.valueOf(defaultValue);
  }

  /**
   * Whether a value must be supplied. Only meaningful for module fields; core settings always carry a default and so are never required.
   */
  public boolean required() {
    return required;
  }

  /**
   * The value type, for the UI and for validating updates.
   */
  public abstract ConfigType type();

  /**
   * Parses a stored string into this setting's typed value.
   */
  public abstract T convert(String raw);

  /**
   * Builds a setting from a module manifest entry, choosing the subclass by declared {@code type} (defaulting to {@link ConfigType#STRING}); the typed default is parsed from the manifest string.
   */
  @JsonCreator
  static Setting<?> fromManifest(
      @JsonProperty("key") String key,
      @JsonProperty("label") String label,
      @JsonProperty("type") ConfigType type,
      @JsonProperty("default") String defaultValue,
      @JsonProperty("required") boolean required) {
    ConfigType resolved = type == null ? ConfigType.STRING : type;
    return switch (resolved) {
      case NUMBER -> new IntSetting(key, label, defaultValue == null ? null : Integer.valueOf(defaultValue), required);
      case BOOLEAN -> new BoolSetting(key, label, defaultValue == null ? null : Boolean.valueOf(defaultValue), required);
      case SECRET -> new SecretSetting(key, label, defaultValue, required);
      case STRING -> new StringSetting(key, label, defaultValue, required);
    };
  }
}
