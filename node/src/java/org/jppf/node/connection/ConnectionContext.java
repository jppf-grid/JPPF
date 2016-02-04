/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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

package org.jppf.node.connection;

/**
 * Instances of this class define the context for a node (re)connection request.
 * @author Laurent Cohen
 * @since 4.1
 */
public class ConnectionContext {
  /**
   * Some explanation text for the reconnection.
   */
  private final String message;
  /**
   * A {@link Throwable} that triggered the reconnection.
   */
  private final Throwable throwable;
  /**
   * The reason for the connection or reconnection.
   */
  private final ConnectionReason reason;

  /**
   * Initialize this context with the specified parameters.
   * @param message some explanation text for the reconnection.
   * @param throwable a {@link Throwable} that triggered the reconnection.
   * @param reason the reason for the connection or reconnection.
   * @exclude
   */
  public ConnectionContext(final String message, final Throwable throwable, final ConnectionReason reason) {
    this.message = message;
    this.throwable = throwable;
    this.reason = reason;
  }

  /**
   * Get an explanation text for the reconnection.
   * @return the explanation as a string, possibly {@code null}.
   */
  public String getMessage() {
    return message;
  }

  /**
   * Get an eventual {@link Throwable} that triggered the reconnection.
   * @return a {@link Throwable}, possibly {@code null}.
   */
  public Throwable getThrowable() {
    return throwable;
  }

  /**
   * Get the reason for the connection or reconnection.
   * @return the reason as a {@link ConnectionReason} enum value.
   */
  public ConnectionReason getReason() {
    return reason;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append('[');
    sb.append("message=").append(message);
    sb.append(", throwable=").append(throwable);
    sb.append(", reason=").append(reason);
    sb.append(']');
    return sb.toString();
  }
}
