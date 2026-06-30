package org.example.modular.core.dbviewer;

import java.util.List;
import org.example.modular.core.configuration.core.CoreConfigService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Admin-only, read-only endpoints for the demo database viewer. Demonstration/debugging tool — not business logic; disable live with the {@code dbviewer.enabled} core setting.
 */
@RestController
@RequestMapping("/api/db")
public class DbViewerController {

  private final DbViewerService service;
  private final CoreConfigService config;

  public DbViewerController(DbViewerService service, CoreConfigService config) {
    this.service = service;
    this.config = config;
  }

  /**
   * Every non-system schema with its base-table names — the viewer's navigation tree.
   */
  @GetMapping("/schemas")
  public List<SchemaView> schemas() {
    requireEnabled();
    return service.schemas();
  }

  /**
   * A capped page of one table's rows plus its columns and total row count.
   */
  @GetMapping("/tables/{schema}/{table}")
  public TableData table(@PathVariable String schema, @PathVariable String table,
      @RequestParam(defaultValue = "100") int limit, @RequestParam(defaultValue = "0") int offset) {
    requireEnabled();
    return service.table(schema, table, limit, offset);
  }

  /**
   * Hides the viewer (404) when the {@code dbviewer.enabled} core setting is off, so toggling it takes effect without a restart.
   */
  private void requireEnabled() {
    if (!config.get(DbViewerSettings.DBVIEWER_ENABLED)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Database viewer is disabled");
    }
  }
}
