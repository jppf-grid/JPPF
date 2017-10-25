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

/**
 * 
 * @author Laurent Cohen
 */
public class JMXResponse extends AbstractJMXMessage {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The result of the request.
   */
  private final Object result;
  /**
   * An exception eventually raised when performing the request.
   */
  private final Exception exception;

  /**
   * Initialize this request with the specified ID, request type and parameters.
   * @param messageID the message id.
   * @param requestType the type of request.
   * @param params the request's parameters.
   */
  public JMXResponse(final String messageID, final JMXMessageType requestType, final Object result) {
    super(messageID, requestType);
    this.result = result;
    this.exception = null;
  }

  /**
   * Initialize this request with the specified ID, request type and parameters.
   * @param messageID the message id.
   * @param requestType the type of request.
   * @param Exception an exception eventually raised when performing the request.
   */
  public JMXResponse(final String messageID, final JMXMessageType requestType, final Exception exception) {
    super(messageID, requestType);
    this.result = null;
    this.exception = exception;
  }

  /**
   * @return the request's parameters.
   */
  public Object getResult() {
    return result;
  }

  /**
   * @return an exception eventually raised when performing the request.
   */
  public Exception getException() {
    return exception;
  }
}
