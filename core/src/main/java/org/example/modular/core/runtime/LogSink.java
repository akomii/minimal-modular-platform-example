package org.example.modular.core.runtime;

public interface LogSink {

  void line(String text);

  void complete();

  void error(Throwable t);
}
