package org.example.modular.core.dependency;

import org.example.modular.core.module.ModuleCatalog;
import org.example.modular.core.module.ModuleDefinition;
import org.example.modular.core.provisioning.ModuleProvisioner;
import org.springframework.stereotype.Component;

/**
 * Enforces module prerequisites (the manifest {@code dependsOn} list): a module installs only if each prerequisite is installed at a satisfying version, and a module can't be removed while an
 * installed module still depends on it. Gating only — it never installs or removes anything itself.
 */
@Component
public class ModuleDependencyChecker {

  private final ModuleCatalog catalog;
  private final ModuleProvisioner provisioner;

  public ModuleDependencyChecker(ModuleCatalog catalog, ModuleProvisioner provisioner) {
    this.catalog = catalog;
    this.provisioner = provisioner;
  }

  /**
   * Throws if any prerequisite is unknown to the catalog, not installed, or installed at a version that doesn't satisfy the declared constraint.
   */
  public void requireDependenciesInstalled(ModuleDefinition module) {
    for (ModuleDefinition.Dependency dep : module.getDependsOn()) {
      boolean known = catalog.getModules().stream().anyMatch(candidate -> candidate.getId().equals(dep.getId()));
      if (!known) {
        throw new ModuleDependencyException(module.getId() + " declares an unknown dependency: " + dep.getId());
      }
      String installed = provisioner.installedVersion(dep.getId());
      if (installed == null) {
        throw new ModuleDependencyException(module.getId() + " requires " + dep.getId() + " " + dep.getVersion() + ", but it is not installed");
      }
      if (!new VersionConstraint(dep.getVersion()).satisfies(installed)) {
        throw new ModuleDependencyException(module.getId() + " requires " + dep.getId() + " " + dep.getVersion() + ", but installed " + installed + " does not satisfy it");
      }
    }
  }

  /**
   * Throws if any installed module declares this module as a prerequisite.
   */
  public void requireNoInstalledDependents(ModuleDefinition module) {
    for (ModuleDefinition other : catalog.getModules()) {
      boolean dependsOnThis = other.getDependsOn().stream().anyMatch(dep -> dep.getId().equals(module.getId()));
      if (dependsOnThis && provisioner.installedVersion(other.getId()) != null) {
        throw new ModuleDependencyException("cannot remove " + module.getId() + ": " + other.getId() + " depends on it");
      }
    }
  }
}
