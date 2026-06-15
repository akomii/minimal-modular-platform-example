package org.example.modular.core.observability;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Exposes the management UI's live observability streams (request log, server log) as admin-only Server-Sent Events endpoints.
 */
@RestController
@RequestMapping("/api")
public class ObservabilityController {

  private final RequestLogStore requestLog;
  private final ServerLogStore serverLog;

  public ObservabilityController(RequestLogStore requestLog, ServerLogStore serverLog) {
    this.requestLog = requestLog;
    this.serverLog = serverLog;
  }

  /**
   * Streams handled HTTP requests to a subscribing UI client as Server-Sent Events.
   */
  @GetMapping(value = "/requests/stream", produces = "text/event-stream")
  public SseEmitter requests() {
    return requestLog.subscribe();
  }

  /**
   * Streams formatted server log lines to a subscribing UI client as Server-Sent Events.
   */
  @GetMapping(value = "/server/logs/stream", produces = "text/event-stream")
  public SseEmitter serverLogs() {
    return serverLog.subscribe();
  }
}
