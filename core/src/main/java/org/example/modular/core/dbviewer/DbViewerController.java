package org.example.modular.core.dbviewer;

import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin-only, read-only endpoints for the demo database viewer. Demonstration/debugging tool — not business logic; disable with {@code dbviewer.enabled=false}.
 */
@RestController
@RequestMapping("/api/db")
@ConditionalOnProperty(name = "dbviewer.enabled", havingValue = "true", matchIfMissing = true)
public class DbViewerController {

  private final DbViewerService service;

  public DbViewerController(DbViewerService service) {
    this.service = service;
  }

  /**
   * Every non-system schema with its base-table names — the viewer's navigation tree.
   */
  @GetMapping("/schemas")
  public List<SchemaView> schemas() {
    return service.schemas();
  }

  /**
   * A capped page of one table's rows plus its columns and total row count.
   */
  @GetMapping("/tables/{schema}/{table}")
  public TableData table(@PathVariable String schema, @PathVariable String table,
      @RequestParam(defaultValue = "100") int limit, @RequestParam(defaultValue = "0") int offset) {
    return service.table(schema, table, limit, offset);
  }
}
