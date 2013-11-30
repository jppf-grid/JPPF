/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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

package org.jppf.classloader;

import org.jppf.comm.socket.SocketWrapper;
import org.jppf.io.IOHelper;
import org.jppf.serialization.ObjectSerializer;

/**
 * Encapsulates a remote resource request submitted asynchronously
 * via the single-thread executor.
 */
class RemoteResourceRequest extends AbstractResourceRequest
{
  /**
   * Used to serialize the requets and deserialze the response.
   */
  private final ObjectSerializer serializer;
  /**
   * The socket client used to get response for request.
   */
  private final SocketWrapper socketClient;

  /**
   * Initialize with the specified request.
   * @param request the request to send.
   * @param serializer used to serialize the requets and deserialze the response.
   * @param socketClient the socket client used to get response for request.
   * @throws Exception if any error occurs.
   */
  public RemoteResourceRequest(final JPPFResourceWrapper request, final ObjectSerializer serializer, final SocketWrapper socketClient) throws Exception
  {
    super(request);
    if (socketClient == null) throw new IllegalArgumentException("socketClient is null");
    
    this.serializer = serializer;
    this.socketClient = socketClient;
  }

  /**
   * Initialize with the specified serializer.
   * @param serializer used to serialize the requets and deserialze the response.
   * @param socketClient the socket client used to get response for request.
   * @throws Exception if any error occurs.
   */
  public RemoteResourceRequest(final ObjectSerializer serializer, final SocketWrapper socketClient) throws Exception
  {
    if (socketClient == null) throw new IllegalArgumentException("socketClient is null");

    this.serializer = serializer;
    this.socketClient = socketClient;
  }

  @Override
  public void run()
  {
    try
    {
      throwable = null;
      IOHelper.sendData(socketClient, request, serializer);
      response = (JPPFResourceWrapper) IOHelper.unwrappedData(socketClient, serializer);
    }
    catch (Throwable t)
    {
      throwable = t;
    }
  }
}
