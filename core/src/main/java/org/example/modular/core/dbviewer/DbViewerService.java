package org.example.modular.core.dbviewer;

import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Demo/debug-only read model behind the database viewer: lists schemas and returns capped, validated pages of a table's rows. No business logic — purely for inspecting the running database.
 */
@Service
public class DbViewerService {

  private static final int MAX_LIMIT = 500;

  private final DbCatalogRepository catalog;

  public DbViewerService(DbCatalogRepository catalog) {
    this.catalog = catalog;
  }

  public List<SchemaView> schemas() {
    return catalog.schemas();
  }

  /**
   * Returns a page of the table's contents, rejecting any pair not present in the catalog (so the dynamic query only ever runs against a real table) and clamping the paging window.
   */
  public TableData table(String schema, String table, int limit, int offset) {
    if (!catalog.tableExists(schema, table)) {
      throw new TableNotFoundException(schema, table);
    }
    int safeLimit = Math.max(1, Math.min(limit, MAX_LIMIT));
    int safeOffset = Math.max(0, offset);
    return new TableData(schema, table,
        catalog.columns(schema, table),
        catalog.rows(schema, table, safeLimit, safeOffset),
        catalog.rowCount(schema, table),
        safeLimit, safeOffset);
  }
}
