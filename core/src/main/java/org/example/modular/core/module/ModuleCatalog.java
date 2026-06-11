package org.example.modular.core.module;

import java.util.List;

/**
 * Source of module definitions and their referenced SQL scripts. Implementations decide where they come from (filesystem tree, registry, database, ...).
 */
public interface ModuleCatalog {

  List<ModuleDefinition> getModules();

  ModuleDefinition byId(String id);

  String readScript(String relativePath);
}
