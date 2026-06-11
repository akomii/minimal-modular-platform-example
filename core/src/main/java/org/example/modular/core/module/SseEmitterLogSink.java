package org.example.modular.core.module;

import java.io.IOException;
import org.example.modular.core.runtime.LogSink;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Bridges a runtime {@link LogSink} to a Spring {@link SseEmitter}: every log line becomes one SSE event; completion and errors close the emitter.
 */
final class SseEmitterLogSink implements LogSink {

  private final SseEmitter emitter;

  SseEmitterLogSink(SseEmitter emitter) {
    this.emitter = emitter;
  }

  @Override
  public void line(String text) {
    try {
      emitter.send(text);
    } catch (IOException e) {
      emitter.completeWithError(e);
    }
  }

  @Override
  public void complete() {
    emitter.complete();
  }

  @Override
  public void error(Throwable t) {
    emitter.completeWithError(t);
  }
}
