package org.example.modular.core.observability;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.AppenderBase;
import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Attaches a Logback appender to the root logger at startup so the core app's own log events are formatted and forwarded into the {@link ServerLogStore} for live streaming.
 */
@Component
public class ServerLogAppenderInstaller {

  private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneId.systemDefault());

  private final ServerLogStore store;

  public ServerLogAppenderInstaller(ServerLogStore store) {
    this.store = store;
  }

  /**
   * Builds and registers the SSE-feeding appender on the Logback root logger once the bean is initialized.
   */
  @PostConstruct
  void install() {
    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
    AppenderBase<ILoggingEvent> appender = new AppenderBase<>() {
      @Override
      protected void append(ILoggingEvent event) {
        store.publish(format(event));
      }
    };
    appender.setContext(context);
    appender.setName("sse-server-log");
    appender.start();
    context.getLogger(Logger.ROOT_LOGGER_NAME).addAppender(appender);
  }

  /**
   * Renders a log event into the single-line {@code time level logger - message} form (appending the throwable summary, if any) used by the UI's server-log view.
   */
  private static String format(ILoggingEvent event) {
    StringBuilder line = new StringBuilder()
        .append(TIME.format(Instant.ofEpochMilli(event.getTimeStamp())))
        .append(' ').append(event.getLevel())
        .append(' ').append(shortLogger(event.getLoggerName()))
        .append(" - ").append(event.getFormattedMessage());
    IThrowableProxy throwable = event.getThrowableProxy();
    if (throwable != null) {
      line.append('\n').append(throwable.getClassName()).append(": ").append(throwable.getMessage());
    }
    return line.toString();
  }

  /**
   * Shortens a fully qualified logger name to its simple class name to keep the UI's log lines compact.
   */
  private static String shortLogger(String name) {
    int lastDot = name.lastIndexOf('.');
    return lastDot >= 0 ? name.substring(lastDot + 1) : name;
  }
}
