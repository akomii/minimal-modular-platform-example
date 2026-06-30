package org.example.modular.core.ui;

/**
 * A module UI page the current user may see, rendered as a tab in the SPA: the owning module, a display label, and the browser-reachable URL to embed.
 */
public record ModuleUiDTO(String moduleId, String label, String url) {

}
