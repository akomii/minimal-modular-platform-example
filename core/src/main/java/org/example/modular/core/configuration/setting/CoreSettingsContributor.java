package org.example.modular.core.configuration.setting;

import java.util.List;

/**
 * Implemented by a feature to contribute its own core live-config settings to the registry. Each feature declares its typed setting constants next to the code that uses them and lists them here;
 * {@code CoreConfigService} collects every contributor, so it never enumerates which settings exist.
 */
public interface CoreSettingsContributor {

  List<Setting<?>> coreSettings();
}
