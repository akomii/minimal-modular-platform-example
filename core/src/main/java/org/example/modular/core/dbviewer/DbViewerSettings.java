package org.example.modular.core.dbviewer;

import java.util.List;
import org.example.modular.core.configuration.setting.CoreSettingsContributor;
import org.example.modular.core.configuration.setting.Setting;
import org.example.modular.core.configuration.setting.type.BoolSetting;
import org.springframework.stereotype.Component;

/**
 * Declares the database viewer's own live-config setting (its enabled flag) next to the code that uses it, and contributes it to the core config registry.
 */
@Component
public class DbViewerSettings implements CoreSettingsContributor {

  public static final BoolSetting DBVIEWER_ENABLED = new BoolSetting("dbviewer.enabled", "Database viewer enabled", true);

  @Override
  public List<Setting<?>> coreSettings() {
    return List.of(DBVIEWER_ENABLED);
  }
}
