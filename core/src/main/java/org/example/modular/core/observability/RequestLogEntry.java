package org.example.modular.core.observability;

/**
 * One handled HTTP request as shown in the management UI's live request-log stream (timestamp, method, path and response status).
 */
public record RequestLogEntry(String time, String method, String path, int status) {
}
