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
   * The result of the request.
   */
  private Object result;
  /**
   * An exception eventually raised when performing the request.
   */
  private Exception exception;

  /**
   * Initialize this request with the specified ID, request type and parameters.
   * @param messageID the message id.
   * @param requestType the type of request.
   * @param result the request's result.
   */
  public JMXResponse(final long messageID, final byte requestType, final Object result) {
    this(messageID, requestType, result, null);
  }

  /**
   * Initialize this request with the specified ID, request type and parameters.
   * @param messageID the message id.
   * @param requestType the type of request.
   * @param exception an exception eventually raised when performing the request.
   */
  public JMXResponse(final long messageID, final byte requestType, final Exception exception) {
    this(messageID, requestType, null, exception);
  }

  /**
   * Initialize this request with the specified ID, request type and parameters.
   * @param messageID the message id.
   * @param requestType the type of request.
   * @param result the request's result.
   * @param exception an exception eventually raised when performing the request.
   */
  public JMXResponse(final long messageID, final byte requestType, final Object result, final Exception exception) {
    super(messageID, requestType);
    this.result = result;
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

  @Override
  public String toString() {
    return new StringBuilder(getClass().getSimpleName()).append('[')
      .append("messageID=").append(getMessageID())
      .append(", messageType=").append(getMessageType())
      .append(", result=").append(Arrays.asList(result))
      .append(", exception=").append(exception)
      .append(']').toString();
  }

  /**
   * Save the state of this object to a stream (i.e.,serialize it).
   * @param out the output stream to which to write this object. 
   * @throws IOException if any I/O error occurs.
   */
  private void writeObject(final ObjectOutputStream out) throws IOException {
    boolean hasException = exception != null;
    out.writeBoolean(hasException);
    out.writeObject(hasException ? exception : result);
  }

  /**
   * Reconstitute this object from a stream (i.e., deserialize it).
   * @param in the input stream from which to read the object. 
   * @throws IOException if any I/O error occurs.
   * @throws ClassNotFoundException if the class of an object in the object graph could not be found.
   */
  private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
    boolean hasException = in.readBoolean();
    Object o = in.readObject();
    if (hasException) exception = (Exception) o;
    else result = o;
  }
}
