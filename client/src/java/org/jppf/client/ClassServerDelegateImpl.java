/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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

import org.jppf.JPPFException;
import org.jppf.comm.socket.*;
import org.jppf.utils.ExceptionUtils;
import org.slf4j.*;

/**
 * Wrapper around an incoming socket connection, whose role is to receive the names of classes
 * to load from the classpath, then send the class files' contents (or bytecode) to the remote client.
 * <p>Instances of this class are part of the JPPF dynamic class loading mechanism. They enable remote nodes
 * to dynamically load classes from the JVM that run's the class server.
 * @author Laurent Cohen
 * @author Domingos Creado
 * @exclude
 */
public class ClassServerDelegateImpl extends AbstractClassServerDelegate {
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
  public ClassServerDelegateImpl(final JPPFClientConnection owner, final String uuid, final String host, final int port) throws Exception {
    super(owner);
    this.clientUuid = uuid;
    this.host = host;
    this.port = port;
  }

  /**
   * Initialize this delegate's resources.
   * @throws Exception if an error is raised during initialization.
   */
  @Override
  public final void init() throws Exception {
    try {
      if (owner.isClosed()) {
        log.warn("attempting to init closed " + getClass().getSimpleName() + ", aborting");
        return;
      }
      handshakeDone = false;
      setStatus(CONNECTING);
      if (socketClient == null) initSocketClient();
      String msg = "[client: " + getName() + "] Attempting connection to the class server at " + host + ':' + port;
      System.out.println(msg);
      log.info(msg);
      socketInitializer.initializeSocket(socketClient);
      if (!socketInitializer.isSuccessful() && !socketInitializer.isClosed()) {
        throw new JPPFException('[' + getName() + "] Could not reconnect to the class server");
      }
      if (!socketInitializer.isClosed()) {
        msg = "[client: " + getName() + "] Reconnected to the class server";
        System.out.println(msg);
        log.info(msg);
        setStatus(ACTIVE);
      }
    } catch(Exception e) {
      if (debugEnabled) log.debug(ExceptionUtils.getMessage(e));
      setStatus(FAILED);
      throw e;
    }
  }

  /**
   * Main processing loop of this delegate.
   */
  @Override
  public void run() {
    try {
      Thread.currentThread().setUncaughtExceptionHandler(this);
      while (!stop && !isClosed()) {
        try {
          if (!handshakeDone) handshake();
          processNextRequest();
        } catch(Exception e) {
          if (!isClosed()) {
            if (debugEnabled) log.debug('[' + getName()+ "] caught " + e + ", will re-initialise ...", e);
            else log.warn('[' + getName()+ "] caught " + ExceptionUtils.getMessage(e) + ", will re-initialise ...");
            init();
            if  (debugEnabled) log.debug('[' + this.getName() + "] : successfully initialized");
          }
        }
      }
    } catch (Exception e) {
      log.error('[' +getName()+"] "+e.getMessage(), e);
      close();
    }
  }

  /**
   * Create a socket initializer for this delegate.
   * @return a <code>SocketInitializer</code> instance.
   */
  @Override
  protected SocketInitializer createSocketInitializer() {
    return new SocketInitializerImpl();
  }
}
