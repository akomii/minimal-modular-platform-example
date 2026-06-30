package org.example.modular.core.ui;

import java.util.List;
import org.example.modular.core.config.AuthenticationPrincipals;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tells the SPA which module UI tabs the current user may see. Available to any authenticated user (not just admins), so non-admin users can reach their permitted module UIs.
 */
@RestController
@RequestMapping("/api/ui")
public class ModuleUiController {

  private final ModuleUiService service;

  public ModuleUiController(ModuleUiService service) {
    this.service = service;
  }

  @GetMapping
  public List<ModuleUiDTO> ui(Authentication authentication) {
    return service.visibleTo(AuthenticationPrincipals.userId(authentication));
  }
}
