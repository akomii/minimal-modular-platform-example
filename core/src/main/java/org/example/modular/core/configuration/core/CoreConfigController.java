package org.example.modular.core.configuration.core;

import java.util.List;
import java.util.Map;
import org.example.modular.core.configuration.setting.SettingDTO;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin-only REST API for the curated core live-config settings: list them and change any of them, which takes effect immediately.
 */
@RestController
@RequestMapping("/api/config")
public class CoreConfigController {

  private final CoreConfigService config;

  public CoreConfigController(CoreConfigService config) {
    this.config = config;
  }

  /**
   * Every core setting with its current value.
   */
  @GetMapping
  public List<SettingDTO> list() {
    return config.list();
  }

  /**
   * Validates and persists the submitted values; each takes effect immediately.
   */
  @PutMapping
  public List<SettingDTO> update(@RequestBody Map<String, String> values) {
    config.update(values);
    return config.list();
  }

  /**
   * Restores all core settings to their defaults and returns the refreshed list; takes effect immediately.
   */
  @DeleteMapping
  public List<SettingDTO> reset() {
    config.reset();
    return config.list();
  }
}
