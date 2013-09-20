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
package org.jppf.jca.work;

import static org.jppf.client.JPPFClientConnectionStatus.*;

import javax.resource.spi.work.Work;

import org.jppf.JPPFException;
import org.jppf.client.*;
import org.jppf.comm.socket.*;
import org.jppf.utils.JPPFConfiguration;
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
   *
   * @param owner the connection that owns this class server delegate.
   * @param name the name given to this this delegate.
   * @param uuid the unique identifier for the local JPPF client.
   * @param host the name or IP address of the host the class server is running on.
   * @param port the TCP port the class server is listening to.
   * @throws Exception if the connection could not be opened.
   */
  public JcaClassServerDelegate(final JPPFJcaClientConnection owner, final String name, final String uuid, final String host, final int port) throws Exception
  {
    super(owner);
    this.clientUuid = uuid;
    this.host = host;
    this.port = port;
    setName(name);
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
      if (((AbstractJPPFClientConnection) owner).isClosed()) throw new IllegalStateException("this class server connection is closed");
      setStatus(CONNECTING);
      if (socketClient == null) initSocketClient();
      if (debugEnabled) log.debug("[client: " + getName() + "] Attempting connection to the class server");
      if (debugEnabled) log.debug("JPPF configuration: " + JPPFConfiguration.getProperties());
      socketInitializer.initializeSocket(socketClient);
      if (!socketInitializer.isClosed())
      {
        if (socketInitializer.isSuccessful())
        {
          log.info("[client: " + getName() + "] Reconnected to the class server");
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

  @Override
  public void run()
  {
    try
    {
      Thread.currentThread().setUncaughtExceptionHandler(this);
      while (!stop)
      {
        try
        {
          if (getStatus().equals(DISCONNECTED)) performConnection();
          if (getStatus().equals(ACTIVE)) processNextRequest();
          else Thread.sleep(100);
        }
        catch(Exception e)
        {
          if (!closed)
          {
            if (debugEnabled) log.debug('[' + getName()+ "] caught " + e + ", will re-initialise ...", e);
            setStatus(DISCONNECTED);
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
   * Create a socket initializer for this delegate.
   * @return a <code>SocketInitializer</code> instance.
   */
  @Override
  protected SocketInitializer createSocketInitializer()
  {
    //return new JcaSocketInitializer();
    return new SocketInitializerImpl();
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
