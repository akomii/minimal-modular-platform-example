package org.example.modular.core.configuration.setting;

/**
 * API view of a configuration setting (core setting or module field): its declaration plus its current value (stored value, or the default). Secret values are returned as-is.
 */
public record SettingDTO(String key, String label, ConfigType type, boolean required, String defaultValue, String value) {

}
