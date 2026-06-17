package org.example.modular.core.event;

/**
 * One event read back from the log: its sequence number (the replay cursor and SSE id) and its opaque JSON payload.
 */
public record EventRecord(long seq, String topic, String payload) {

}
