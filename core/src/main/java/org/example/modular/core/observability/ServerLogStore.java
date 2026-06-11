package org.example.modular.core.observability;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class ServerLogStore {

  private final SseBroadcaster broadcaster;

  public ServerLogStore(@Value("${observability.server-log-capacity}") int capacity) {
    this.broadcaster = new SseBroadcaster(capacity);
  }

  public void publish(String line) {
    broadcaster.publish(line);
  }

  public SseEmitter subscribe() {
    return broadcaster.subscribe();
  }
}
