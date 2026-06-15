package org.example.modular.core.module;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;

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

  @Data
  public static class Mount {

    private String source;
    private String target;
  }

  /**
   * Identity resources the module needs: an OAuth client (named after the module), realm roles and optionally users.
   */
  @Data
  public static class Idp {

    private List<String> redirectUris = new ArrayList<>();
    private List<String> roles = new ArrayList<>();
    private List<IdpUser> users = new ArrayList<>();
  }

  /**
   * A user the module ships (with password) or an existing user it grants roles to (without password).
   */
  @Data
  public static class IdpUser {

    private String username;
    private String password;
    private List<String> roles = new ArrayList<>();
  }

  @Data
  public static class Db {

    private String schema;
    private CoreAccess coreAccess = CoreAccess.NONE;
    private String up;
    private String down;
  }
}
