/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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

package org.jppf.jmxremote;

import java.util.EventObject;

/**
 * Notification of a connection event on the server side.
 * @author Laurent Cohen
 * @exclude
 */
public class JMXConnectionStatusEvent extends EventObject {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * A {@link Throwable} that caused the connection failure, may be {@code null}.
   */
  private final Throwable throwable;

  /**
   * Initialize this event with the specified connection id and no {@link Throwable}. 
   * @param connectionID the id of the connection source of this event.
   */
  public JMXConnectionStatusEvent(final String connectionID) {
    this(connectionID, null);
  }

  /**
   * Initialize this event with the specified connection id and {@link Throwable}. 
   * @param connectionID the id of the connection source of this event.
   * @param throwable a {@link Throwable} that caused the connection failure, may be {@code null}.
   */
  public JMXConnectionStatusEvent(final String connectionID, final Throwable throwable) {
    super(connectionID);
    this.throwable = throwable;
  }

  /**
   * @return the id of the connection source of this event.
   */
  public String getConnectionID() {
    return (String) getSource();
  }

  /**
   * @return the {@link Throwable} that caused the connection failure, may be {@code null}.
   */
  public Throwable getThrowable() {
    return throwable;
  }

  @Override
  public String toString() {
    return new StringBuilder(getClass().getSimpleName()).append('[')
      .append("connectionID=").append(getConnectionID())
      .append(", throwable=").append(throwable)
      .append(']').toString();
  }
}
