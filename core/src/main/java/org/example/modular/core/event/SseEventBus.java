package org.example.modular.core.event;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * The event bus: persists and replays through an {@link EventStore} and fans events out to per-topic SSE subscribers. Backend-agnostic — the active backend feeds freshly-stored events back in via
 * {@link #deliver} (for Postgres, that's {@link PostgresEventListener}).
 */
@Service
public class SseEventBus {

  private final EventStore store;
  private final Map<String, Set<SseEmitter>> subscribers = new ConcurrentHashMap<>();

  public SseEventBus(EventStore store) {
    this.store = store;
  }

  public long publish(String topic, String payload, String publisher) {
    return store.append(topic, payload, publisher);
  }

  public SseEmitter subscribe(String topic, long sinceSeq) {
    SseEmitter emitter = new SseEmitter(0L);
    Set<SseEmitter> topicSubscribers = subscribers.computeIfAbsent(topic, t -> new CopyOnWriteArraySet<>());
    emitter.onCompletion(() -> topicSubscribers.remove(emitter));
    emitter.onTimeout(() -> topicSubscribers.remove(emitter));
    emitter.onError(ex -> topicSubscribers.remove(emitter));
    // Register before replaying so an event stored mid-replay can't slip through the gap; the
    // subscriber dedups by the SSE id (seq), so the small replay/live overlap is harmless.
    topicSubscribers.add(emitter);
    try {
      for (EventRecord event : store.replay(topic, sinceSeq)) {
        send(emitter, event);
      }
    } catch (Exception ex) {
      topicSubscribers.remove(emitter);
      emitter.completeWithError(ex);
    }
    return emitter;
  }

  /**
   * Pushes a freshly-stored event to its topic's live subscribers, dropping any whose send fails. Called by the active backend as events land.
   */
  void deliver(EventRecord event) {
    Set<SseEmitter> topicSubscribers = subscribers.get(event.topic());
    if (topicSubscribers == null) {
      return;
    }
    for (SseEmitter emitter : topicSubscribers) {
      try {
        send(emitter, event);
      } catch (Exception ex) {
        topicSubscribers.remove(emitter);
      }
    }
  }

  // Sends to one emitter can come from both the replay and the backend's dispatch thread;
  // serialize them so a message isn't interleaved. The seq is the SSE id, driving client resume.
  private static void send(SseEmitter emitter, EventRecord event) throws IOException {
    synchronized (emitter) {
      emitter.send(SseEmitter.event().id(Long.toString(event.seq())).data(event.payload()));
    }
  }
}
