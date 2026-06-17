package org.example.modular.core.event;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * The HTTP/SSE contract modules use to reach the event bus: publish an opaque JSON event to a topic, or subscribe to a topic's stream. A topic is a single path segment (use dots, not slashes).
 */
@RestController
@RequestMapping("/api/events")
public class EventController {

  private final SseEventBus eventBus;

  public EventController(SseEventBus eventBus) {
    this.eventBus = eventBus;
  }

  /**
   * Publishes an opaque JSON event to a topic and returns the sequence number it was assigned.
   */
  @PostMapping("/{topic}")
  public PublishResult publish(@PathVariable String topic, @RequestBody JsonNode payload, Authentication caller) {
    return new PublishResult(eventBus.publish(topic, payload.toString(), publisher(caller)));
  }

  /**
   * Streams a topic's events as Server-Sent Events, replaying from the caller's cursor (the {@code Last-Event-ID} header on reconnect, or an explicit {@code since}) then tailing live.
   */
  @GetMapping(value = "/{topic}/stream", produces = "text/event-stream")
  public SseEmitter stream(@PathVariable String topic, @RequestHeader(value = "Last-Event-ID", required = false) Long lastEventId, @RequestParam(value = "since", required = false) Long since) {
    long cursor = lastEventId != null ? lastEventId : since != null ? since : 0L;
    return eventBus.subscribe(topic, cursor);
  }

  // A module authenticates with its client-credentials token, whose azp claim is its oauth client
  // id (= module id); fall back to the principal name for a browser admin.
  private static String publisher(Authentication caller) {
    if (caller.getPrincipal() instanceof Jwt jwt && jwt.getClaimAsString("azp") != null) {
      return jwt.getClaimAsString("azp");
    }
    return caller.getName();
  }

  public record PublishResult(long seq) {

  }
}
