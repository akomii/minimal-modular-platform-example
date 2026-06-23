package org.example.modular.core.dbviewer;

import java.util.List;

/**
 * A non-system schema and the names of its base tables, as shown in the demo database viewer's tree.
 */
public record SchemaView(String name, List<String> tables) {

}
