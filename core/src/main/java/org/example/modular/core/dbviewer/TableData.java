package org.example.modular.core.dbviewer;

import java.util.List;
import java.util.Map;

/**
 * A capped page of one table's contents: its ordered column names, the page of rows (each a column-keyed map of stringified values), and the total row count for paging.
 */
public record TableData(String schema, String table, List<String> columns, List<Map<String, Object>> rows, long totalRows, int limit, int offset) {

}
