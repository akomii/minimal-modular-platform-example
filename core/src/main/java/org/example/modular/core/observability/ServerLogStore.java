package org.example.modular.core.observability;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * In-memory, bounded store of recent formatted server log lines that fans them out to subscribed management-UI clients.
 */
@Component
public class ServerLogStore {

  private final SseBroadcaster broadcaster;

  public ServerLogStore(@Value("${observability.server-log-capacity}") int capacity) {
    this.broadcaster = new SseBroadcaster(capacity);
  }

  /**
   * Buffers a formatted server log line and pushes it to all current subscribers.
   */
  public void publish(String line) {
    broadcaster.publish(line);
  }

  /**
   * Opens an SSE stream that first replays the buffered backlog, then receives subsequent server log lines.
   */
  public SseEmitter subscribe() {
    return broadcaster.subscribe();
  }
}
