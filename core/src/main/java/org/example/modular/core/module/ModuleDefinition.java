package org.example.modular.core.module;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import org.example.modular.core.configuration.setting.Setting;

/**
 * Parsed module manifest: container image, ports, env and mounts plus optional database and identity (idp) provisioning sections, and prerequisite modules.
 */
@Data
public class ModuleDefinition {

  private String id;
  private String version;
  private String image;
  /**
   * Overrides the image's default command when non-empty; lets a module run a one-shot script that exits (the container then stops itself).
   */
  private List<String> command = new ArrayList<>();
  private List<String> ports = new ArrayList<>();
  private Map<String, String> env = new LinkedHashMap<>();
  private List<Mount> mounts = new ArrayList<>();
  private Db db;
  private Idp idp;
  private List<Dependency> dependsOn = new ArrayList<>();
  private List<UiPage> ui = new ArrayList<>();
  private List<Endpoint> endpoints = new ArrayList<>();
  private List<Setting<?>> config = new ArrayList<>();

  /**
   * A read-only host-to-container file mount (manifest {@code source} path resolved against the modules directory, container {@code target} path).
   */
  @Data
  public static class Mount {

    private String source;
    private String target;
  }

  /**
   * Identity resources the module needs: an OAuth client (named after the module), its client roles, optional service accounts the module owns, and role grants to pre-existing platform users.
   */
  @Data
  public static class Idp {

    private List<String> redirectUris = new ArrayList<>();
    private List<String> roles = new ArrayList<>();
    /**
     * Service accounts the module owns: created on demand with a generated password, granted their roles, and deleted when the module is removed.
     */
    private List<IdpUser> users = new ArrayList<>();
    /**
     * Role grants to pre-existing platform users (e.g. {@code admin}): the listed client roles are assigned to the named user, who is never created or deleted by the module.
     */
    private List<IdpUser> grants = new ArrayList<>();
  }

  /**
   * A username paired with the client roles to grant it.
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

  /**
   * A prerequisite module: {@code id} must be installed at a version satisfying the {@code version} constraint (e.g. {@code >=1.0.0}) before this module can be installed.
   */
  @Data
  public static class Dependency {

    private String id;
    private String version;
  }

  /**
   * A web page the module exposes for embedding as a tab: a display {@code name} and a {@code path} relative to the module's published port.
   */
  @Data
  public static class UiPage {

    private String name;
    private String path;
  }

  /**
   * An HTTP endpoint the module exposes, declared for discovery only: a display {@code label}, the HTTP {@code method}, and a {@code path} relative to the module's published port. Core records it but
   * does not call it.
   */
  @Data
  public static class Endpoint {

    private String label;
    private String method;
    private String path;
  }
}
