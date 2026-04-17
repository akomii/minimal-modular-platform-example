package org.example.modular.core.module;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ModuleCatalog {

  private final String moduleDir;
  private final ObjectMapper objectMapper;

  @Getter
  private final List<ModuleDefinition> modules = new ArrayList<>();

  public ModuleCatalog(@Value("${module.dir:modules}") String moduleDir, ObjectMapper objectMapper) {
    this.moduleDir = moduleDir;
    this.objectMapper = objectMapper;
  }

  @PostConstruct
  public void loadModules() {
    Path path = Paths.get(moduleDir);
    if (!Files.exists(path)) {
      throw new IllegalStateException("Module directory not found: " + path.toAbsolutePath());
    }
    try (Stream<Path> paths = Files.walk(path)) {
      paths.filter(Files::isRegularFile)
          .filter(p -> p.toString().endsWith(".json"))
          .forEach(this::loadManifest);
    } catch (Exception e) {
      throw new RuntimeException("Failed to load modules from " + moduleDir, e);
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

  public ModuleDefinition byId(String id) {
    return modules.stream()
        .filter(module -> module.getId().equals(id))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unknown module: " + id));
  }
}
