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

package org.jppf.client;

import java.util.*;

import org.jppf.classloader.*;
import org.jppf.comm.socket.SocketClient;
import org.jppf.io.IOHelper;
import org.jppf.utils.JPPFIdentifiers;
import org.slf4j.*;

/**
 * Abstract implementation of the client end of the JPPF distributed class loader.
 * @author Laurent Cohen
 * @exclude
 */
public abstract class AbstractClassServerDelegate extends AbstractClientConnectionHandler implements ClassServerDelegate, Thread.UncaughtExceptionHandler
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
  private final Map<String, ClassLoader> classLoaderMap = new Hashtable<>();
  /**
   * Determines if the handshake with the server has been performed.
   */
  protected boolean handshakeDone = false;
  /**
   * Readable name for this delegate.
   */
  protected final String formattedName;

  /**
   * Default instantiation of this class is not permitted.
   * @param owner the client connection which owns this connection delegate.
   */
  protected AbstractClassServerDelegate(final JPPFClientConnection owner)
  {
    super(owner, owner.getName() + " - ClassServer");
    formattedName = "[" + name + ']';
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
    if (debugEnabled) log.debug(formattedName + " reading next resource ...");
    return (JPPFResourceWrapper) IOHelper.unwrappedData(socketClient, socketClient.getSerializer());
  }

  /**
   * Write a resource wrapper object to the socket connection.
   * @param resource a <code>JPPFResourceWrapper</code> instance.
   * @throws Exception if any error is raised.
   */
  protected void writeResource(final JPPFResourceWrapper resource) throws Exception
  {
    IOHelper.sendData(socketClient, resource, socketClient.getSerializer());
    socketClient.flush();
    if (debugEnabled) log.debug(formattedName + " data sent to the server");
  }

  /**
   * Perform the handshake with the server.
   * @throws Exception if any error occurs.
   */
  protected void handshake() throws Exception
  {
    if (debugEnabled) log.debug(formattedName + " : sending channel identifier");
    socketClient.writeInt(JPPFIdentifiers.CLIENT_CLASSLOADER_CHANNEL);
    if (owner.isSSLEnabled()) createSSLConnection();
    if (debugEnabled) log.debug(formattedName + " : sending initial resource");
    JPPFResourceWrapper resource = new JPPFResourceWrapper();
    resource.setState(JPPFResourceWrapper.State.PROVIDER_INITIATION);
    resource.addUuid(clientUuid);
    resource.setData(ResourceIdentifier.CONNECTION_UUID, owner.getConnectionUuid());
    writeResource(resource);
    // read the server response
    readResource();
    handshakeDone = true;
    if (debugEnabled) log.debug(formattedName + " : server handshake done");
  }

  /**
   * Process the next class laodign request from the server.
   * @throws Exception if any error occcurs.
   */
  protected void processNextRequest() throws Exception
  {
    boolean found = true;
    JPPFResourceWrapper resource = readResource();
    String name = resource.getName();
    if (debugEnabled) log.debug(formattedName + " resource requested: " + resource);
    ClassLoader cl = getClassLoader(resource.getRequestUuid());
    //if (debugEnabled) log.debug('[' + this.getName() + "] resource requested: " + name + " using classloader=" + cl);
    if (debugEnabled) log.debug(formattedName + " using classloader=" + cl);
    if (resource.getData(ResourceIdentifier.MULTIPLE) != null)
    {
      List<byte[]> list = resourceProvider.getMultipleResourcesAsBytes(name, cl);
      if (list != null) resource.setData(ResourceIdentifier.RESOURCE_LIST, list);
    }
    else if (resource.getData(ResourceIdentifier.MULTIPLE_NAMES) != null)
    {
      String[] names = (String[]) resource.getData(ResourceIdentifier.MULTIPLE_NAMES);
      Map<String, List<byte[]>> result = resourceProvider.getMultipleResourcesAsBytes(cl, names);
      resource.setData(ResourceIdentifier.RESOURCE_MAP, result);
    }
    else
    {
      byte[] b;
      byte[] callable = resource.getCallable();
      if (callable != null) b = resourceProvider.computeCallable(callable);
      else b = resourceProvider.getResource(name, cl);
      if (b == null) found = false;
      if (callable == null) resource.setDefinition(b);
      else resource.setCallable(b);
      if (debugEnabled)
      {
        if (found) log.debug(formattedName + " found resource: " + name + " (" + b.length + " bytes)");
        else log.debug(formattedName + " resource not found: " + name);
      }
    }
    resource.setState(JPPFResourceWrapper.State.PROVIDER_RESPONSE);
    writeResource(resource);
  }


  /**
   * Retrieve the class loader to use from the client.
   * @param uuid the uuid of the request from which the class loader was obtained.
   * @return a <code>ClassLoader</code> instance, or null if none could be found.
   */
  protected ClassLoader getClassLoader(final String uuid)
  {
    return ((AbstractJPPFClientConnection) owner).getClient().getRequestClassLoader(uuid);
  }

  /**
   * Close the socket connection.
   * @see org.jppf.client.ClassServerDelegate#close()
   */
  @Override
  public void close()
  {
    if (!closed)
    {
      if (debugEnabled) log.debug("closing " + getName());
      closed = true;
      stop = true;
      super.close();
      if (debugEnabled) log.debug(getName() + " closed");
    }
  }

  @Override
  public void uncaughtException(final Thread t, final Throwable e)
  {
    log.error("uncaught exception", e);
  }
}
