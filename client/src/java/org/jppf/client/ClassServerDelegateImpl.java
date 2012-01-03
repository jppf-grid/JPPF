/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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

import static org.jppf.client.JPPFClientConnectionStatus.*;

import java.util.*;

import org.jppf.JPPFException;
import org.jppf.classloader.JPPFResourceWrapper;
import org.jppf.comm.socket.*;
import org.slf4j.*;

/**
 * Wrapper around an incoming socket connection, whose role is to receive the names of classes
 * to load from the classpath, then send the class files' contents (or bytecode) to the remote client.
 * <p>Instances of this class are part of the JPPF dynamic class loading mechanism. They enable remote nodes
 * to dynamically load classes from the JVM that run's the class server.
 * @author Laurent Cohen
 * @author Domingos Creado
 */
public class ClassServerDelegateImpl extends AbstractClassServerDelegate
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(ClassServerDelegateImpl.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();

  /**
   * Initialize class server delegate with a specified application uuid.
   * @param owner the client connection which owns this delegate.
   * @param uuid the unique identifier for the local JPPF client.
   * @param host the name or IP address of the host the class server is running on.
   * @param port the TCP port the class server is listening to.
   * @throws Exception if the connection could not be opened.
   */
  public ClassServerDelegateImpl(final JPPFClientConnection owner, final String uuid, final String host, final int port) throws Exception
  {
    super(owner);
    this.clientUuid = uuid;
    this.host = host;
    this.port = port;
    setName(owner.getName());
  }

  /**
   * Initialize this node's resources.
   * @throws Exception if an error is raised during initialization.
   * @see org.jppf.client.ClassServerDelegate#init()
   */
  @Override
  public final void init() throws Exception
  {
    try
    {
      handshakeDone = false;
      socketInitializer.setName('[' + getName() + " - delegate] ");
      setStatus(CONNECTING);
      if (socketClient == null) initSocketClient();
      String msg = "[client: " + getName() + "] Attempting connection to the class server at " + host + ':' + port;
      System.out.println(msg);
      log.info(msg);
      socketInitializer.initializeSocket(socketClient);
      if (!socketInitializer.isSuccessful() && !socketInitializer.isClosed())
      {
        throw new JPPFException('[' + getName() + "] Could not reconnect to the class server");
      }
      if (!socketInitializer.isClosed())
      {
        msg = "[client: " + getName() + "] Reconnected to the class server";
        System.out.println(msg);
        log.info(msg);
        setStatus(ACTIVE);
      }
    }
    catch(Exception e)
    {
      setStatus(FAILED);
      throw e;
    }
  }

  /**
   * Main processing loop of this delegate.
   * @see org.jppf.client.ClassServerDelegate#run()
   */
  @Override
  public void run()
  {
    try
    {
      while (!stop)
      {
        try
        {
          if (!handshakeDone) handshake();
          boolean found = true;
          JPPFResourceWrapper resource = readResource();
          String name = resource.getName();
          ClassLoader cl = getClassLoader(resource.getRequestUuid());
          if  (debugEnabled) log.debug('[' + this.getName() + "] resource requested: " + name + " using classloader=" + cl);
          if (resource.getData("multiple") != null)
          {
            List<byte[]> list = resourceProvider.getMultipleResourcesAsBytes(name, cl);
            if (list != null) resource.setData("resource_list", list);
          }
          else if (resource.getData("multiple.resources.names") != null)
          {
            String[] names = (String[]) resource.getData("multiple.resources.names");
            Map<String, List<byte[]>> result = resourceProvider.getMultipleResourcesAsBytes(cl, names);
            resource.setData("resource_map", result);
          }
          else
          {
            byte[] b;
            byte[] callable = resource.getCallable();
            if (callable != null) b = resourceProvider.computeCallable(callable);
            else
            {
              if (resource.isAsResource()) b = resourceProvider.getResource(name, cl);
              else b = resourceProvider.getResourceAsBytes(name, cl);
            }
            if (b == null) found = false;
            if (callable == null) resource.setDefinition(b);
            else resource.setCallable(b);
            if (debugEnabled)
            {
              if (found) log.debug('[' +this.getName()+"] sent resource: " + name + " (" + b.length + " bytes)");
              else log.debug('[' +this.getName()+"] resource not found: " + name);
            }
          }
          resource.setState(JPPFResourceWrapper.State.PROVIDER_RESPONSE);
          writeResource(resource);
        }
        catch(Exception e)
        {
          if (!closed)
          {
            log.warn('[' + getName()+ "] caught " + e + ", will re-initialise ...", e);
            init();
            if  (debugEnabled) log.debug('[' + this.getName() + "] : successfully initialized");
          }
        }
      }
    }
    catch (Exception e)
    {
      log.error('[' +getName()+"] "+e.getMessage(), e);
      close();
    }
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
      closed = true;
      stop = true;

      try
      {
        socketInitializer.close();
        socketClient.close();
      }
      catch (Exception e)
      {
        log.error('[' + getName() + "] "+e.getMessage(), e);
      }
    }
  }

  /**
   * Create a socket initializer for this delegate.
   * @return a <code>SocketInitializer</code> instance.
   */
  @Override
  protected SocketInitializer createSocketInitializer()
  {
    return new SocketInitializerImpl();
  }

  /**
   * Retrieve the class loader to use from the client.
   * @param uuid the uuid of the request from which the class loader was obtained.
   * @return a <code>ClassLoader</code> instance, or null if none could be found.
   */
  private ClassLoader getClassLoader(final String uuid)
  {
    return getRequestClassLoader(uuid);
  }
}
