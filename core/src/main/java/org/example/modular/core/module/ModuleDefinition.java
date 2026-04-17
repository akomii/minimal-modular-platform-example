package org.example.modular.core.module;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class ModuleDefinition {

  private String id;
  private String version;
  private String type;
  private String deployment;
  private String image;
  private List<String> ports = new ArrayList<>();
}
