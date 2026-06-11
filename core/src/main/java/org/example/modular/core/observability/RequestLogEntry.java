package org.example.modular.core.observability;

public record RequestLogEntry(String time, String method, String path, int status) {
}
