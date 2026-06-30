package org.example.modular.core.provisioning;

/**
 * A schema object in the core schema — a {@code table}, a {@code column} (on {@code table}), or an {@code index} (on {@code table}) — used to diff what a module's migrations add to core.
 */
public record CoreObject(String kind, String table, String name) {
}
