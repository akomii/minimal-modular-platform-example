package org.example.modular.core.runtime;

/**
 * Callback for {@link ModuleRuntime#streamLogs} that receives a container's log output line by line, with terminal {@code complete}/{@code error} signals.
 */
public interface LogSink {

  void line(String text);

  void complete();

  void error(Throwable t);
}
