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

import java.io.IOException;

import org.jppf.JPPFException;
import org.jppf.comm.interceptor.InterceptorHandler;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Wrapper around an incoming socket connection, whose role is to receive the names of classes
 * to load from the classpath, then send the class files' contents (or bytecode) to the remote client.
 * <p>Instances of this class are part of the JPPF dynamic class loading mechanism. They enable remote nodes
 * to dynamically load classes from the JVM that run's the class server.
 * @author Laurent Cohen
 * @author Domingos Creado
 */
class ClassServerDelegateImpl extends AbstractClassServerDelegate {
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
  ClassServerDelegateImpl(final JPPFClientConnection owner, final String uuid, final String host, final int port) throws Exception {
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
    if (owner.isClosed()) {
      log.warn("attempting to init closed " + getClass().getSimpleName() + ", aborting");
      return;
    }
    boolean done = false;
    while (!done) {
      handshakeDone = false;
      if (socketClient == null) initSocketClient();
      final boolean sysoutEnabled = owner.getConnectionPool().getClient().isSysoutEnabled();
      String msg = String.format("[client: %s] Attempting connection to the class server at %s:%d", getName(), host, port);
      if (sysoutEnabled) System.out.println(msg);
      log.info(msg);
      if (!socketInitializer.initialize(socketClient)) throw new JPPFException('[' + getName() + "] Could not reconnect to the class server");
      if (!InterceptorHandler.invokeOnConnect(socketClient, JPPFChannelDescriptor.CLIENT_CLASSLOADER_CHANNEL))
        throw new JPPFException('[' + getName() + "] Could not reconnect to the class server due to interceptor failure");
      if (!socketInitializer.isClosed()) {
        msg = "[client: " + getName() + "] Reconnected to the class server";
        if (sysoutEnabled) System.out.println(msg);
        log.info(msg);
      }
      try {
        if (!handshakeDone) handshake();
        done = true;
      } catch (final IOException e) {
        log.error("handshake error", e);
      }
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
        } catch(final Exception e) {
          if (!isClosed()) {
            if (debugEnabled) log.debug('[' + getName()+ "] caught " + e + ", will re-initialise ...", e);
            else log.warn('[' + getName()+ "] caught " + ExceptionUtils.getMessage(e) + ", will re-initialise ...");
            final JPPFClientConnectionImpl c = (JPPFClientConnectionImpl) owner;
            c.setStatus(JPPFClientConnectionStatus.DISCONNECTED);
            c.submitInitialization();
            break;
          }
        }
      }
    } catch (final Exception e) {
      log.error('[' +getName()+"] "+e.getMessage(), e);
      close();
    }
  }
}
