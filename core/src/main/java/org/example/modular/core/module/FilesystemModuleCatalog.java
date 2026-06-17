package org.example.modular.core.module;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * {@link ModuleCatalog} backed by the filesystem: at startup it loads every manifest under the configured modules directory and afterwards serves their definitions and referenced scripts.
 */
@Service
public class FilesystemModuleCatalog implements ModuleCatalog {

  private final String modulesDir;
  private final ObjectMapper objectMapper;
  private final List<ModuleDefinition> modules = new ArrayList<>();

  public FilesystemModuleCatalog(@Value("${modules.dir}") String modulesDir, ObjectMapper objectMapper) {
    this.modulesDir = modulesDir;
    this.objectMapper = objectMapper;
  }

  /**
   * Walks the modules directory at startup and parses every {@code manifest.json} into a {@link ModuleDefinition}.
   */
  @PostConstruct
  public void loadModules() {
    Path path = Paths.get(modulesDir);
    if (!Files.exists(path)) {
      throw new IllegalStateException("Module directory not found: " + path.toAbsolutePath());
    }
    try (Stream<Path> paths = Files.walk(path)) {
      paths.filter(Files::isRegularFile)
          // only module manifests — not other JSON a module ships (e.g. Grafana dashboards under provisioning/)
          .filter(p -> p.getFileName().toString().equals("manifest.json"))
          .forEach(this::loadManifest);
    } catch (Exception e) {
      throw new RuntimeException("Failed to load modules from " + modulesDir, e);
    }
  }

  private void loadManifest(Path path) {
    try {
      ModuleDefinition def = objectMapper.readValue(path.toFile(), ModuleDefinition.class);
      modules.add(def);
    } catch (Exception e) {
      throw new RuntimeException("Failed to parse " + path, e);
    }
  }

  @Override
  public List<ModuleDefinition> getModules() {
    return Collections.unmodifiableList(modules);
  }

  @Override
  public String readScript(String relativePath) {
    Path file = Paths.get(modulesDir, relativePath);
    try {
      return Files.readString(file);
    } catch (Exception e) {
      throw new RuntimeException("Failed to read SQL script: " + file, e);
    }
  }

  @Override
  public ModuleDefinition byId(String id) {
    return modules.stream()
        .filter(module -> module.getId().equals(id))
        .findFirst()
        .orElseThrow(() -> new ModuleNotFoundException("Unknown module: " + id));
  }
}
