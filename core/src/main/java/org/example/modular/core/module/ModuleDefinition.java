package org.example.modular.core.module;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;

/**
 * Parsed module manifest: container image, ports, env and mounts plus optional database and identity (idp) provisioning sections.
 */
@Data
public class ModuleDefinition {

  private String id;
  private String version;
  private String type;
  private String deployment;
  private String image;
  private List<String> ports = new ArrayList<>();
  private Map<String, String> env = new LinkedHashMap<>();
  private List<Mount> mounts = new ArrayList<>();
  private Db db;
  private Idp idp;

  /**
   * A read-only host-to-container file mount (manifest {@code source} path resolved against the modules directory, container {@code target} path).
   */
  @Data
  public static class Mount {

    private String source;
    private String target;
  }

  /**
   * Identity resources the module needs: an OAuth client (named after the module), client roles and optionally service accounts.
   */
  @Data
  public static class Idp {

    private List<String> redirectUris = new ArrayList<>();
    private List<String> roles = new ArrayList<>();
    private List<IdpUser> users = new ArrayList<>();
  }

  /**
   * A service account the module ships: created on demand with a generated password and granted the listed roles.
   */
  @Data
  public static class IdpUser {

    private String username;
    private List<String> roles = new ArrayList<>();
  }

  /**
   * Database provisioning the module requests: whether it gets its own schema (named {@code mod_<id>}, like its role), the access level it needs on the core schema, and the ordered migrations to
   * apply (first entry installs, later entries are version deltas).
   */
  @Data
  public static class Db {

    private boolean ownSchema;
    private CoreAccess coreAccess = CoreAccess.NONE;
    private List<String> migrations = new ArrayList<>();
  }
}
