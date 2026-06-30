package org.example.modular.core.module;

import java.util.List;

/**
 * Source of module definitions and their referenced SQL scripts. Implementations decide where they come from (filesystem tree, registry, database, ...).
 */
public interface ModuleCatalog {

  List<ModuleDefinition> getModules();

  /**
   * Returns the module with the given id, or throws if none matches.
   */
  ModuleDefinition byId(String id);

  /**
   * Reads the text of a module-referenced script (e.g. a SQL migration) by its path relative to the modules directory.
   */
  String readScript(String relativePath);
}
