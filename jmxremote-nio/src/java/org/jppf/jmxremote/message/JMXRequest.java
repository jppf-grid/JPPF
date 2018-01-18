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

import java.io.*;
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
   * Constant for no parameters in the request.
   */
  private static final Object[] NO_PARAMS = {};
  /**
   * The request's parameters.
   */
  private Object[] params;
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
  public JMXRequest(final long messageID, final byte requestType, final Object... params) {
    super(messageID, requestType);
    this.params = (params == null) || (params.length == 0) ? NO_PARAMS : params;
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
      .append("messageID=").append(getMessageID())
      .append(", messageType=").append(getMessageType())
      .append(", params=").append(Arrays.deepToString(params))
      .append(']').toString();
  }

  /**
   * Save the state of this object to a stream (i.e.,serialize it).
   * @param out the output stream to which to write this object. 
   * @throws IOException if any I/O error occurs.
   */
  private void writeObject(final ObjectOutputStream out) throws IOException {
    final int n = params.length;
    out.writeByte(n);
    if (n > 0) {
      for (final Object o: params) out.writeObject(o);
    }
  }

  /**
   * Reconstitute this object from a stream (i.e., deserialize it).
   * @param in the input stream from which to read the object. 
   * @throws IOException if any I/O error occurs.
   * @throws ClassNotFoundException if the class of an object in the object graph could not be found.
   */
  private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
    final int n = in.readByte();
    if (n <= 0) params = NO_PARAMS;
    else {
      params  = new Object[n];
      for (int i=0; i<n; i++) params[i] = in.readObject();
    }
  }
}
