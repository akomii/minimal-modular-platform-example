package org.example.modular.core.user;

import java.util.List;

/**
 * API view of a platform user for the user-management UI: the IdP id, username, optional email, and the ids of the assignable roles currently held.
 */
public record UserDTO(String id, String username, String email, List<String> roles) {

}
