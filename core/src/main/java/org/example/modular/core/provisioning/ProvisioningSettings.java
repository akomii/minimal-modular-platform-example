package org.example.modular.core.provisioning;

import java.util.List;
import org.example.modular.core.configuration.setting.CoreSettingsContributor;
import org.example.modular.core.configuration.setting.Setting;
import org.example.modular.core.configuration.setting.type.IntSetting;
import org.example.modular.core.configuration.setting.type.StringSetting;
import org.springframework.stereotype.Component;

/**
 * Declares provisioning's own live-config settings (the database connection handed to module containers as MODULE_DB_* env vars) next to the code that uses them, and contributes them to the core config
 * registry, so a changed value is picked up by the next module provisioned without a restart.
 */
@Component
public class ProvisioningSettings implements CoreSettingsContributor {

  public static final StringSetting DB_HOST = new StringSetting("modules.db-host", "Module DB host", "host.docker.internal");
  public static final IntSetting DB_PORT = new IntSetting("modules.db-port", "Module DB port", 5432);
  public static final StringSetting DB_NAME = new StringSetting("modules.db-name", "Module DB name", "dwh");

  @Override
  public List<Setting<?>> coreSettings() {
    return List.of(DB_HOST, DB_PORT, DB_NAME);
  }
}
