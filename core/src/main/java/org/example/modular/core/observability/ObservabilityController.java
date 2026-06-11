package org.example.modular.core.observability;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api")
public class ObservabilityController {

  private final RequestLogStore requestLog;
  private final ServerLogStore serverLog;

  public ObservabilityController(RequestLogStore requestLog, ServerLogStore serverLog) {
    this.requestLog = requestLog;
    this.serverLog = serverLog;
  }

  @GetMapping(value = "/requests/stream", produces = "text/event-stream")
  public SseEmitter requests() {
    return requestLog.subscribe();
  }

  @GetMapping(value = "/server/logs/stream", produces = "text/event-stream")
  public SseEmitter serverLogs() {
    return serverLog.subscribe();
  }
}
