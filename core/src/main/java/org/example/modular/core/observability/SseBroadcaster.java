package org.example.modular.core.observability;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Reusable building block behind each observability stream: keeps a bounded backlog of recent items and fans new items out to all connected SSE subscribers.
 */
public class SseBroadcaster {

  private final int capacity;
  private final Deque<Object> buffer = new ArrayDeque<>();
  private final Set<SseEmitter> emitters = new CopyOnWriteArraySet<>();

  public SseBroadcaster(int capacity) {
    this.capacity = capacity;
  }

  /**
   * Appends an item to the bounded backlog (evicting the oldest past capacity) and sends it to every subscriber, dropping any whose send fails.
   */
  public void publish(Object item) {
    synchronized (buffer) {
      buffer.addLast(item);
      while (buffer.size() > capacity) {
        buffer.removeFirst();
      }
    }
    for (SseEmitter emitter : emitters) {
      try {
        emitter.send(item);
      } catch (Exception ex) {
        emitters.remove(emitter);
      }
    }
  }

  /**
   * Creates a non-expiring SSE emitter, replays the current backlog to it, and registers it for future items (self-unregistering on completion, timeout or error).
   */
  public SseEmitter subscribe() {
    SseEmitter emitter = new SseEmitter(0L);
    emitter.onCompletion(() -> emitters.remove(emitter));
    emitter.onTimeout(() -> emitters.remove(emitter));
    emitter.onError(ex -> emitters.remove(emitter));
    Object[] backlog;
    synchronized (buffer) {
      backlog = buffer.toArray();
    }
    try {
      for (Object item : backlog) {
        emitter.send(item);
      }
    } catch (Exception ex) {
      emitter.completeWithError(ex);
      return emitter;
    }
    emitters.add(emitter);
    return emitter;
  }
}
