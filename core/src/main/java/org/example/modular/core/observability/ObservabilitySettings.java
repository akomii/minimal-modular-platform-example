package org.example.modular.core.observability;

import java.util.List;
import org.example.modular.core.configuration.setting.CoreSettingsContributor;
import org.example.modular.core.configuration.setting.Setting;
import org.example.modular.core.configuration.setting.type.IntSetting;
import org.springframework.stereotype.Component;

/**
 * Declares observability's own live-config settings (the stream buffer capacities) next to the code that uses them, and contributes them to the core config registry.
 */
@Component
public class ObservabilitySettings implements CoreSettingsContributor {

  public static final IntSetting REQUEST_LOG_CAPACITY = new IntSetting("observability.request-log-capacity", "HTTP request log capacity", 500);
  public static final IntSetting SERVER_LOG_CAPACITY = new IntSetting("observability.server-log-capacity", "Server log capacity", 1000);

  @Override
  public List<Setting<?>> coreSettings() {
    return List.of(REQUEST_LOG_CAPACITY, SERVER_LOG_CAPACITY);
  }
}
