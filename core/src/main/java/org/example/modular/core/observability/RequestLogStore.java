package org.example.modular.core.observability;

import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * In-memory, bounded store of recent HTTP request-log entries that fans them out to subscribed management-UI clients.
 */
@Component
public class RequestLogStore {

  private final SseBroadcaster broadcaster;

  public RequestLogStore(@Value("${observability.request-log-capacity}") int capacity) {
    this.broadcaster = new SseBroadcaster(capacity);
  }

  /**
   * Buffers a new request-log entry (stamped with the current time) and pushes it to all current subscribers.
   */
  public void record(String method, String path, int status) {
    broadcaster.publish(new RequestLogEntry(Instant.now().toString(), method, path, status));
  }

  /**
   * Opens an SSE stream that first replays the buffered backlog, then receives subsequent request-log entries.
   */
  public SseEmitter subscribe() {
    return broadcaster.subscribe();
  }
}
