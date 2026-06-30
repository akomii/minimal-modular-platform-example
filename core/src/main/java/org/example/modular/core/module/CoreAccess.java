package org.example.modular.core.module;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Locale;

/**
 * Access level a module requests on the core schema.
 */
public enum CoreAccess {
  NONE,
  READ,
  WRITE;

  /**
   * Parses the manifest's access value into the enum, failing fast on an unknown value so a manifest typo can't silently become "no access".
   */
  @JsonCreator
  public static CoreAccess parse(String value) {
    for (CoreAccess access : values()) {
      if (access.name().equalsIgnoreCase(value)) {
        return access;
      }
    }
    throw new IllegalArgumentException("Unknown coreAccess value: " + value);
  }

  /**
   * Lowercase form used in manifests, the API and the provisioning table.
   */
  @JsonValue
  @Override
  public String toString() {
    return name().toLowerCase(Locale.ROOT);
  }
}
