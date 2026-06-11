package org.example.modular.core.observability;

import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class RequestLogStore {

  private final SseBroadcaster broadcaster;

  public RequestLogStore(@Value("${observability.request-log-capacity}") int capacity) {
    this.broadcaster = new SseBroadcaster(capacity);
  }

  public void record(String method, String path, int status) {
    broadcaster.publish(new RequestLogEntry(Instant.now().toString(), method, path, status));
  }

  public SseEmitter subscribe() {
    return broadcaster.subscribe();
  }
}
