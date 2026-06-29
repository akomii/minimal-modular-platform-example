package org.example.modular.core.configuration.setting;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Locale;

/**
 * Type of a config value (core setting or module config field): drives the UI input and how a value is validated. {@code SECRET} is a string the UI renders masked.
 */
public enum ConfigType {
  STRING,
  NUMBER,
  BOOLEAN,
  SECRET;

  /**
   * Parses the manifest's/registry's type value into the enum, failing fast on an unknown value so a typo can't silently pick a wrong input.
   */
  @JsonCreator
  public static ConfigType parse(String value) {
    for (ConfigType type : values()) {
      if (type.name().equalsIgnoreCase(value)) {
        return type;
      }
    }
    throw new IllegalArgumentException("Unknown config type: " + value);
  }

  /**
   * Checks that the value matches this type (numbers parse as integers, booleans are {@code true}/{@code false}); strings and secrets accept any text. Throws {@link IllegalArgumentException} on a
   * mismatch, naming the offending {@code key}.
   */
  public void validate(String key, String value) {
    switch (this) {
      case NUMBER -> {
        try {
          Integer.parseInt(value);
        } catch (NumberFormatException e) {
          throw new IllegalArgumentException(key + " must be a number: " + value);
        }
      }
      case BOOLEAN -> {
        if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
          throw new IllegalArgumentException(key + " must be true or false: " + value);
        }
      }
      case STRING, SECRET -> {
        // any text is acceptable
      }
    }
  }

  /**
   * Lowercase form used in manifests and the API.
   */
  @JsonValue
  @Override
  public String toString() {
    return name().toLowerCase(Locale.ROOT);
  }
}
