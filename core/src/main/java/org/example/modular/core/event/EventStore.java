package org.example.modular.core.event;

import java.util.List;

/**
 * Storage seam for the event log, behind the event bus. A backend implements the append and reads; swapping it leaves the bus, controller and modules untouched.
 */
public interface EventStore {

  /**
   * Appends an event and returns its assigned, gap-free sequence number.
   */
  long append(String topic, String payload, String publisher);

  /**
   * The topic's events after {@code sinceSeq}, in order, for a subscriber catching up.
   */
  List<EventRecord> replay(String topic, long sinceSeq);
}
