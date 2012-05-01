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
package org.jppf.jca.work;

import static org.jppf.client.JPPFClientConnectionStatus.*;

import java.util.*;

import javax.resource.spi.work.Work;

import org.jppf.JPPFException;
import org.jppf.classloader.JPPFResourceWrapper;
import org.jppf.client.*;
import org.jppf.comm.socket.SocketInitializer;
import org.slf4j.*;

/**
 * Wrapper around an incoming socket connection, whose role is to receive the names of classes
 * to load from the classpath, then send the class files' contents (or bytecode) to the remote client.
 * <p>Instances of this class are part of the JPPF dynamic class loading mechanism. They enable remote nodes
 * to dynamically load classes from the JVM that run's the class server.
 * @author Laurent Cohen
 */
public class JcaClassServerDelegate extends AbstractClassServerDelegate implements Work
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JcaClassServerDelegate.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();

  /**
   * Initialize class server delegate with a specified application uuid.
   * @param name the name given to this this delegate.
   * @param uuid the unique identifier for the local JPPF client.
   * @param host the name or IP address of the host the class server is running on.
   * @param port the TCP port the class server is listening to.
   * @param owner the connection that owns this class server delegate.
   * @throws Exception if the connection could not be opened.
   */
  public JcaClassServerDelegate(final String name, final String uuid, final String host, final int port, final JPPFJcaClientConnection owner) throws Exception
  {
    super(owner);
    this.clientUuid = uuid;
    this.host = host;
    this.port = port;
    setName(name);
    socketInitializer.setName("[" + getName() + " - delegate] ");
  }

  /**
   * Initialize this delegate's resources.
   * @throws Exception if an error is raised during initialization.
   * @see org.jppf.client.ClassServerDelegate#init()
   */
  @Override
  public final void init() throws Exception
  {
    try
    {
      setStatus(CONNECTING);
      if (socketClient == null) initSocketClient();
      if (debugEnabled) log.debug("[client: " + getName() + "] Attempting connection to the class server");
      socketInitializer.initializeSocket(socketClient);
      if (!socketInitializer.isClosed())
      {
        if (socketInitializer.isSuccessful())
        {
          log.info("[client: " + getName() + "] Reconnected to the class server");
          if (owner.isSSL()) createSSLConnection();
          setStatus(ACTIVE);
        }
        else
        {
          throw new JPPFException('[' + getName() + "] Could not reconnect to the class server");
        }
      }
      else
      {
        setStatus(FAILED);
        close();
      }
    }
    catch(Exception e)
    {
      if (!closed) setStatus(DISCONNECTED);
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
          if (getStatus().equals(DISCONNECTED)) performConnection();
          if (getStatus().equals(ACTIVE))
          {
            boolean found = true;
            JPPFResourceWrapper resource = readResource();
            String name = resource.getName();
            if  (debugEnabled) log.debug('[' + this.getName() + "] resource requested: " + name);

            String requestUuid = resource.getRequestUuid();
            ClassLoader cl = ((AbstractJPPFClientConnection) owner).getClient().getRequestClassLoader(requestUuid);
            if (debugEnabled) log.debug("attempting resource lookup using classloader=" + cl + " for request uuid = " + requestUuid);
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
          else
          {
            Thread.sleep(100);
          }
        }
        catch(Exception e)
        {
          if (!closed)
          {
            if (debugEnabled) log.debug('[' + getName()+ "] caught " + e + ", will re-initialise ...", e);
            setStatus(DISCONNECTED);
            //init();
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
   * Establish a connection and perform the initial shakedown with the JPPF driver.
   * @throws Exception if the connection could not be established.
   */
  public void performConnection() throws Exception
  {
    try
    {
      init();
      handshake();
    }
    finally
    {
      if (getStatus().equals(DISCONNECTED))
      {
        Thread.sleep(100);
      }
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
    return new JcaSocketInitializer();
  }

  /**
   * This method does nothing.
   * @see javax.resource.spi.work.Work#release()
   */
  @Override
  public void release()
  {
  }
}
