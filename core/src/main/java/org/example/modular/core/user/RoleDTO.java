package org.example.modular.core.user;

/**
 * An assignable role for the user-management matrix: its id ({@code platform-admin} for the realm role, {@code <moduleId>:<role>} for a module client role), a display label, and the owning module
 * ({@code null} for the realm role).
 */
public record RoleDTO(String id, String label, String module) {

}
