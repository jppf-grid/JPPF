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

package org.jppf.jmxremote.message;

import java.util.Arrays;

/**
 * A specialized message that represents a request to the server.
 * @author Laurent Cohen
 */
public class JMXRequest extends AbstractJMXMessage {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The request's parameters.
   */
  private final Object[] params;
  /**
   * The response to this reqquest.
   */
  private transient JMXResponse response;

  /**
   * Initialize this request with the specified ID, request type and parameters.
   * @param messageID the message id.
   * @param requestType the type of request.
   * @param params the request's parameters.
   */
  public JMXRequest(final long messageID, final JMXMessageType requestType, final Object... params) {
    super(messageID, requestType);
    this.params = params;
  }

  /**
   * @return the request's parameters.
   */
  public Object[] getParams() {
    return params;
  }

  /**
   * @return the response to this reqquest.
   */
  public JMXResponse getResponse() {
    return response;
  }

  /**
   * Set the response to this reqquest.
   * @param response a {@code JMXResponse} object.
   */
  public void setResponse(final JMXResponse response) {
    this.response = response;
  }

  @Override
  public String toString() {
    return new StringBuilder(getClass().getSimpleName()).append('[')
      .append("messageID=").append(messageID)
      .append(", messageType=").append(messageType)
      .append(", params=").append(Arrays.asList(params))
      .append(']').toString();
  }
}
