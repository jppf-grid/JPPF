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

package org.jppf.client;

import java.util.*;

import org.jppf.classloader.*;
import org.jppf.comm.socket.SocketClient;
import org.jppf.io.IOHelper;
import org.jppf.utils.*;
import org.jppf.utils.configuration.JPPFProperties;
import org.slf4j.*;

/**
 * Abstract implementation of the client end of the JPPF distributed class loader.
 * @author Laurent Cohen
 * @exclude
 */
public abstract class AbstractClassServerDelegate extends AbstractClientConnectionHandler implements ClassServerDelegate, Thread.UncaughtExceptionHandler {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AbstractClassServerDelegate.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Whether resources should be looked up in the file system if not found in the classpath.
   */
  private final boolean FILE_LOOKUP;
  /**
   * Indicates whether this socket handler should be terminated and stop processing.
   */
  protected boolean stop;
  /**
   * Reads resource files from the classpath.
   */
  protected final ResourceProvider resourceProvider = new ClientResourceProvider();
  //protected final ResourceProvider resourceProvider = new ResourceProviderImpl();
  /**
   * Unique identifier for this class server delegate, obtained from the local JPPF client.
   */
  protected String clientUuid;
  /**
   * Determines if the handshake with the server has been performed.
   */
  protected boolean handshakeDone;
  /**
   * Readable name for this delegate.
   */
  protected final String formattedName;

  /**
   * Default instantiation of this class is not permitted.
   * @param owner the client connection which owns this connection delegate.
   */
  protected AbstractClassServerDelegate(final JPPFClientConnection owner) {
    super(owner, owner.getName() + " - ClassServer");
    FILE_LOOKUP = owner.getConnectionPool().getClient().getConfig().get(JPPFProperties.CLASSLOADER_FILE_LOOKUP);
    formattedName = "[" + name + ']';
    if (debugEnabled) log.debug("resourceProvider={}", resourceProvider);
  }

  /**
   * Get the name of this delegate.
   * @return the name as a string.
   */
  @Override
  public String getName() {
    return name;
  }

  /**
   * Set the name of this delegate.
   * @param name the name as a string.
   */
  @Override
  public void setName(final String name) {
    this.name = name;
  }

  /**
   * Initialize this delegate's resources.
   * @throws Exception if an error is raised during initialization.
   */
  @Override
  public void initSocketClient() throws Exception {
    socketClient = new SocketClient();
    socketClient.setHost(host);
    socketClient.setPort(port);
  }

  /**
   * Read a resource wrapper object from the socket connection.
   * @return a <code>JPPFResourceWrapper</code> instance.
   * @throws Exception if any error is raised.
   */
  protected JPPFResourceWrapper readResource() throws Exception {
    if (debugEnabled) log.debug("{} reading next resource ...", formattedName);
    return (JPPFResourceWrapper) IOHelper.unwrappedData(socketClient, socketClient.getSerializer());
  }

  /**
   * Write a resource wrapper object to the socket connection.
   * @param resource a <code>JPPFResourceWrapper</code> instance.
   * @throws Exception if any error is raised.
   */
  protected void writeResource(final JPPFResourceWrapper resource) throws Exception {
    IOHelper.sendData(socketClient, resource, socketClient.getSerializer());
    socketClient.flush();
    if (debugEnabled) log.debug(formattedName + " data sent to the server");
  }

  /**
   * Perform the handshake with the server.
   * @throws Exception if any error occurs.
   */
  protected void handshake() throws Exception {
    if (debugEnabled) log.debug("{} : sending channel identifier {}", formattedName, JPPFIdentifiers.asString(JPPFIdentifiers.CLIENT_CLASSLOADER_CHANNEL));
    socketClient.writeInt(JPPFIdentifiers.CLIENT_CLASSLOADER_CHANNEL);
    if (owner.isSSLEnabled()) createSSLConnection();
    if (debugEnabled) log.debug("{} : sending initial resource", formattedName);
    final JPPFResourceWrapper resource = new JPPFResourceWrapper();
    resource.setState(JPPFResourceWrapper.State.PROVIDER_INITIATION);
    resource.addUuid(clientUuid);
    resource.setData(ResourceIdentifier.CONNECTION_UUID, owner.getConnectionUuid());
    writeResource(resource);
    // read the server response
    readResource();
    handshakeDone = true;
    if (debugEnabled) log.debug("{} : server handshake done", formattedName);
  }

  /**
   * Process the next class laodign request from the server.
   * @throws Exception if any error occcurs.
   */
  protected void processNextRequest() throws Exception {
    boolean found = true;
    final JPPFResourceWrapper resource = readResource();
    final String name = resource.getName();
    if (debugEnabled) log.debug("{} resource requested: {}, requestUuid={}, resourceIds={}", formattedName, resource, resource.getRequestUuid(), resource.getResourceIds());
    final Collection<ClassLoader> loaders = owner.getConnectionPool().getClient().getRegisteredClassLoaders(resource.getRequestUuid());
    if (debugEnabled) log.debug("{} using classloaders={}", formattedName, loaders);
    final boolean fileLookup = (Boolean) resource.getData(ResourceIdentifier.FILE_LOOKUP_ALLOWED, true) && FILE_LOOKUP;
    if (resource.getData(ResourceIdentifier.MULTIPLE) != null) {
      final List<byte[]> list = resourceProvider.getMultipleResourcesAsBytes(name, loaders, fileLookup);
      if (list != null) resource.setData(ResourceIdentifier.RESOURCE_LIST, list);
    } else if (resource.getData(ResourceIdentifier.MULTIPLE_NAMES) != null) {
      final String[] names = (String[]) resource.getData(ResourceIdentifier.MULTIPLE_NAMES);
      final Map<String, List<byte[]>> result = resourceProvider.getMultipleResourcesAsBytes(loaders, fileLookup, names);
      resource.setData(ResourceIdentifier.RESOURCE_MAP, result);
    } else {
      byte[] b;
      final byte[] callable = resource.getCallable();
      if (callable != null) b = resourceProvider.computeCallable(callable);
      else {
        b = resourceProvider.getResource(name, loaders, fileLookup);
      }
      if (b == null) found = false;
      if (callable == null) resource.setDefinition(b);
      else resource.setCallable(b);
      if (debugEnabled) {
        if (found) log.debug("{} found resource: {} ({} bytes)", formattedName, name, b.length);
        else log.debug("{} resource not found: {}", formattedName, name);
      }
    }
    resource.setState(JPPFResourceWrapper.State.PROVIDER_RESPONSE);
    writeResource(resource);
  }

  /**
   * Close the socket connection.
   */
  @Override
  public void close() {
    if (debugEnabled) log.debug("closing {}", getName());
    stop = true;
    super.close();
    if (debugEnabled) log.debug("{} closed", getName());
  }

  @Override
  public void uncaughtException(final Thread t, final Throwable e) {
    log.error("uncaught exception", e);
  }
}
