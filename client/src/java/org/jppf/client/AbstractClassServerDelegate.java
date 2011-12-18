/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
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

package org.jppf.client;

import java.util.*;

import org.jppf.classloader.*;
import org.jppf.comm.socket.SocketClient;
import org.jppf.data.transform.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Abstract implementation of the client end of the JPPF distributed class loader.
 * @author Laurent Cohen
 */
public abstract class AbstractClassServerDelegate extends AbstractClientConnectionHandler implements ClassServerDelegate
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AbstractClassServerDelegate.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Indicates whether this socket handler should be terminated and stop processing.
   */
  protected boolean stop = false;
  /**
   * Indicates whether this socket handler is closed, which means it can't handle requests anymore.
   */
  protected boolean closed = false;
  /**
   * Reads resource files from the classpath.
   */
  protected ResourceProvider resourceProvider = new ResourceProvider();
  /**
   * Unique identifier for this class server delegate, obtained from the local JPPF client.
   */
  protected String clientUuid = null;
  /**
   * Mapping of class loader to requests uuids.
   */
  private final Map<String, ClassLoader> classLoaderMap = new Hashtable<String, ClassLoader>();
  /**
   * Determines if the handshake with the server has been performed.
   */
  protected boolean handshakeDone = false;

  /**
   * Default instantiation of this class is not permitted.
   * @param owner the client connection which owns this connection delegate.
   */
  protected AbstractClassServerDelegate(final JPPFClientConnection owner)
  {
    super(owner);
  }

  /**
   * Determine whether the socket connection is closed
   * @return true if the socket connection is closed, false otherwise
   * @see org.jppf.client.ClassServerDelegate#isClosed()
   */
  @Override
  public boolean isClosed()
  {
    return closed;
  }

  /**
   * Get the name of this delegate.
   * @return the name as a string.
   * @see org.jppf.client.ClassServerDelegate#getName()
   */
  @Override
  public String getName()
  {
    return name;
  }

  /**
   * Set the name of this delegate.
   * @param name the name as a string.
   * @see org.jppf.client.ClassServerDelegate#setName(java.lang.String)
   */
  @Override
  public void setName(final String name)
  {
    this.name = name;
  }

  /**
   * Initialize this delegate's resources.
   * @throws Exception if an error is raised during initialization.
   * @see org.jppf.client.ClassServerDelegate#initSocketClient()
   */
  @Override
  public void initSocketClient() throws Exception
  {
    socketClient = new SocketClient();
    socketClient.setHost(host);
    socketClient.setPort(port);
  }

  /**
   * Read a resource wrapper object from the socket connection.
   * @return a <code>JPPFResourceWrapper</code> instance.
   * @throws Exception if any error is raised.
   */
  protected JPPFResourceWrapper readResource() throws Exception
  {
    JPPFBuffer buffer = socketClient.receiveBytes(0);
    JPPFDataTransform transform = JPPFDataTransformFactory.getInstance();
    byte[] data = (transform == null) ? buffer.getBuffer() : JPPFDataTransformFactory.transform(transform, false, buffer.buffer, 0, buffer.length);
    return (JPPFResourceWrapper) socketClient.getSerializer().deserialize(data);
  }

  /**
   * Write a resource wrapper object to the socket connection.
   * @param resource a <code>JPPFResourceWrapper</code> instance.
   * @throws Exception if any error is raised.
   */
  protected void writeResource(final JPPFResourceWrapper resource) throws Exception
  {
    JPPFBuffer buffer = socketClient.getSerializer().serialize(resource);
    JPPFDataTransform transform = JPPFDataTransformFactory.getInstance();
    byte[] data = (transform == null) ? buffer.getBuffer() : JPPFDataTransformFactory.transform(transform, true, buffer.buffer, 0, buffer.length);
    if (debugEnabled) log.debug("sending " + data.length + " bytes to the server");
    socketClient.sendBytes(new JPPFBuffer(data, data.length));
    socketClient.flush();
  }

  /**
   * Add a request uuid to class loader mapping to this submission manager.
   * @param uuid the uuid of the request.
   * @param cl the class loader for the request.
   */
  @Override
  public void addRequestClassLoader(final String uuid, final ClassLoader cl)
  {
    classLoaderMap.put(uuid, cl);
  }

  /**
   * Add a request uuid to class loader mapping to this submission manager.
   * @param uuid the uuid of the request.
   */
  @Override
  public void removeRequestClassLoader(final String uuid)
  {
    classLoaderMap.remove(uuid);
  }

  /**
   * Get a class loader from its request uuid.
   * @param uuid the uuid of the request.
   * @return a <code>ClassLoader</code> instance, or null if none exists for the key.
   */
  @Override
  public ClassLoader getRequestClassLoader(final String uuid)
  {
    return classLoaderMap.get(uuid);
  }

  /**
   * Perform the handshake with the server.
   * @throws Exception if any error occurs.
   */
  protected void handshake() throws Exception
  {
    if  (debugEnabled) log.debug('[' + getName() + "] : sending channel identifier");
    socketClient.writeInt(JPPFIdentifiers.CLIENT_CLASSLOADER_CHANNEL);
    if  (debugEnabled) log.debug('[' + getName() + "] : sending initial resource");
    JPPFResourceWrapper resource = new JPPFResourceWrapper();
    resource.setState(JPPFResourceWrapper.State.PROVIDER_INITIATION);
    resource.addUuid(clientUuid);
    resource.setData("connection.uuid", ((AbstractJPPFClientConnection) owner).getConnectionUuid());
    writeResource(resource);
    // read the server response
    readResource();
    handshakeDone = true;
    if  (debugEnabled) log.debug('[' + getName() + "] : server handshake done");
  }
}
