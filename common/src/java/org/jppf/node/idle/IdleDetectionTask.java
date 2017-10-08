/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jppf.node.idle;

import static org.jppf.node.idle.IdleState.*;

import java.util.*;

import org.jppf.JPPFError;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Timer tasks that displays a message whenever the computer has received
 * no mouse or keyboard input for at least the timeout time.
 */
class IdleDetectionTask extends TimerTask {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(IdleDetectionTask.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * The time of inactivity after which the system is considered idle, in milliseconds.
   */
  private final long idleTimeout;
  /**
   * Captures the idle state of the system, as specified by the idle timeout.
   */
  private IdleState state = IDLE;
  /**
   * The object used to obtain the system idle time.
   */
  private final IdleTimeDetector detector;
  /**
   * The list of listeners to this object's events.
   */
  private final List<IdleStateListener> listeners = new ArrayList<>();

  /**
   * Initialize this task with the specified idle timeout and listeners.
   * @param factory a factory for creating {@link IdleTimeDetector} instances.
   * @param idleTimeout the time of inactivity after which the system is considered idle, in milliseconds.
   * @param initialListeners a set of listeners to add to this task at construction time.
   */
  public IdleDetectionTask(final IdleTimeDetectorFactory factory, final long idleTimeout, final IdleStateListener...initialListeners) {
    this.idleTimeout = idleTimeout;
    if (initialListeners != null) {
      for (IdleStateListener listener: initialListeners) addIdleStateListener(listener);
    }
    detector = factory.newIdleTimeDetector();
  }

  @Override
  public void run() {
    try {
      if (detector == null) {
        log.error("idle detector is null, cancelling idle mode");
        cancel();
        return;
      }
      long idleTime = detector.getIdleTimeMillis();
      if ((idleTime < idleTimeout) && (state == BUSY)) {
        if (debugEnabled) log.debug("changing to IDLE for idleTime = {}ms", idleTime);
        changeStateTo(IDLE);
      } else if ((idleTime >= idleTimeout) && (state == IDLE)) {
        if (debugEnabled) log.debug("changing to BUSY for idleTime = {}ms", idleTime);
        changeStateTo(BUSY);
      }
    } catch(JPPFError e) {
      System.out.println(ExceptionUtils.getMessage(e) + " - idle mode is disabled");
      log.error(e.getMessage(), e);
      cancel();
    }
  }

  /**
   * Get the idle state of the system, as specified by the idle timeout.
   * @return an {@link IdleState} enum value.
   */
  public IdleState getState() {
    return state;
  }

  /**
   * Set the idle state of the system, as specified by the idle timeout,
   * and fire a corresponding state change event.
   * @param state an {@link IdleState} enum value.
   */
  private void changeStateTo(final IdleState state) {
    log.debug("changing state to {}", state);
    this.state = state;
    fireIdleStateEvent();
  }

  /**
   * Add a listener to the list of listeners.
   * @param listener the listener to add.
   */
  public void addIdleStateListener(final IdleStateListener listener) {
    if (listener == null) return;
    synchronized (listeners) {
      listeners.add(listener);
    }
  }

  /**
   * Remove a listener from the list of listeners.
   * @param listener the listener to remove.
   */
  public void removeIdleStateListener(final IdleStateListener listener) {
    if (listener == null) return;
    synchronized (listeners) {
      listeners.remove(listener);
    }
  }

  /**
   * Notify all listeners that an event has occurred.
   */
  private void fireIdleStateEvent() {
    IdleStateEvent event = new IdleStateEvent(this);
    synchronized (listeners) {
      for (IdleStateListener listener : listeners) listener.idleStateChanged(event);
    }
  }
}
