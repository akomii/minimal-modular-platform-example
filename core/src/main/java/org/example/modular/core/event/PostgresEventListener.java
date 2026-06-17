package org.example.modular.core.event;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import lombok.extern.slf4j.Slf4j;
import org.postgresql.PGConnection;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Postgres live source for the bus: holds a dedicated connection that LISTENs for the {@code core_events} signal and pushes newly stored events to {@link SseEventBus#deliver}. Replay for reconnecting
 * or catching-up subscribers is handled separately by the bus reading the store.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "events.backend", havingValue = "postgres", matchIfMissing = true)
public class PostgresEventListener {

  private final PostgresEventStore store;
  private final SseEventBus bus;
  private final String url;
  private final String username;
  private final String password;

  private volatile boolean running = true;
  private long lastDispatchedSeq;
  private Thread thread;

  public PostgresEventListener(PostgresEventStore store, SseEventBus bus,
      @Value("${spring.datasource.url}") String url, @Value("${spring.datasource.username}") String username, @Value("${spring.datasource.password}") String password) {
    this.store = store;
    this.bus = bus;
    this.url = url;
    this.username = username;
    this.password = password;
  }

  @PostConstruct
  void start() {
    // Only push events newer than startup to live subscribers; older ones reach a subscriber
    // through its own replay on connect.
    lastDispatchedSeq = store.maxSeq();
    thread = new Thread(this::listen, "event-notification-listener");
    thread.setDaemon(true);
    thread.start();
  }

  @PreDestroy
  void stop() {
    running = false;
    if (thread != null) {
      thread.interrupt();
    }
  }

  private void listen() {
    // A dedicated physical connection (outside the pool) held open for LISTEN/NOTIFY.
    try (Connection connection = DriverManager.getConnection(url, username, password)) {
      try (Statement statement = connection.createStatement()) {
        statement.execute("LISTEN core_events");
      }
      PGConnection pgConnection = connection.unwrap(PGConnection.class);
      while (running) {
        // Wakes promptly on a NOTIFY for low latency; the periodic return also acts as a safety
        // net, and dispatching from the high-water mark self-heals any signal we missed.
        pgConnection.getNotifications(5000);
        dispatch();
      }
    } catch (SQLException ex) {
      if (running) {
        log.error("Event notification listener stopped", ex);
      }
    }
  }

  private synchronized void dispatch() {
    for (EventRecord event : store.readSince(lastDispatchedSeq)) {
      bus.deliver(event);
      lastDispatchedSeq = event.seq();
    }
  }
}
