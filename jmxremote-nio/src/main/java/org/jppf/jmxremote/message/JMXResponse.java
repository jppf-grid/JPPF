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

package org.jppf.jmxremote.message;

import org.jppf.jmx.JMXHelper;

/**
 * A specialized message that represents a repsponse to a previous request.
 * The correlation between request and response is realized via the {@link JMXMessage#getMessageID() messageID}.
 * @author Laurent Cohen
 */
public class JMXResponse extends AbstractJMXMessage {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Whether the result is an exception or a normal result.
   */
  private final boolean isException;
  /**
   * The result of the request.
   */
  private final Object result;

  /**
   * Initialize this request with the ID and request type of the specified message.
   * @param message the message from which to extract the message id and message type.
   * @param result the request's result.
   * @param isException whether the result is an exception or a normal result.
   */
  public JMXResponse(final JMXMessage message, final Object result, final boolean isException) {
    super(message.getMessageID(), message.getMessageType());
    this.result = result;
    this.isException = isException;
  }

  /**
   * @return the request's parameters.
   */
  public Object getResult() {
    return isException ? null : result;
  }

  /**
   * @return an exception eventually raised when performing the request.
   */
  public Exception getException() {
    return isException ? (Exception) result : null;
  }

  @Override
  public String toString() {
    return new StringBuilder(getClass().getSimpleName()).append('[')
      .append("messageID=").append(getMessageID())
      .append(", messageType=").append(JMXHelper.name(getMessageType()))
      .append(", result=").append(checkLengthAndTruncateIfNeed(result))
      .append(", isException=").append(isException)
      .append(']').toString();
  }

  /**
   * 
   * @param source the source string to check.
   * @return the result string.
   */
  private static String checkLengthAndTruncateIfNeed(final Object source) {
    final String s = source == null ? "null" : (source instanceof String ? (String) source : source.toString());
    return (s.length() > 100) ? s.substring(0, 100) : s;
  }
}
