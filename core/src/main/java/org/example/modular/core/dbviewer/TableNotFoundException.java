package org.example.modular.core.dbviewer;

/**
 * Thrown when the viewer is asked for a schema/table pair that the live catalog does not contain.
 */
public class TableNotFoundException extends RuntimeException {

  public TableNotFoundException(String schema, String table) {
    super("No such table: " + schema + "." + table);
  }
}
