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

package org.jppf.process;

import java.io.*;
import java.net.Socket;

import org.jppf.utils.ExceptionUtils;
import org.slf4j.*;

/**
 * Instances of this class listen to a socket connection setup in the ProcessLauncher, to handle the situation when the Launcher dies unexpectedly.<br>
 * In that situation, the connection is broken and this process knows that it must exit.
 * @author Laurent Cohen
 * @exclude
 */
public class LauncherListener extends Thread {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(LauncherListener.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The port on which to listen for the launcher signals.
   */
  private final int port;
  /**
   * The action handler for this listener.
   * @since 5.0
   */
  private LauncherListenerProtocolHandler actionHandler;

  /**
   * Initialize this LauncherListener with the specified port.
   * @param port the port to listen to.
   */
  public LauncherListener(final int port) {
    super("LauncherListener thread");
    this.port = port;
  }

  /**
   * Initialize this LauncherListener with the specified port.
   * @param port the port to listen to.
   * @param actionHandler the action handler for this listener.
   * @since 5.0
   */
  public LauncherListener(final int port, final LauncherListenerProtocolHandler actionHandler) {
    super("LauncherListener thread");
    this.port = port;
    this.actionHandler = actionHandler;
  }

  /**
   * Create a socket connection and listen to it, and exit this process when the connection is broken.
   */
  @Override
  public void run() {
    try {
      Socket s = new Socket("localhost", port);
      DataInputStream dis = new DataInputStream(s.getInputStream());
      if (debugEnabled) log.debug("launcher listener initialized on port {}", port);
      while (true) {
        int n = dis.readInt();
        if (n == -1) throw new EOFException("eof");
        if (debugEnabled) log.debug("received command code {} from controling process", ProcessCommands.getCommandName(n));
        LauncherListenerProtocolHandler ah = getActionHandler();
        if (ah != null) ah.performAction(n);
      }
    } catch(Throwable t) {
      if (debugEnabled) log.debug("exiting with exception: " + ExceptionUtils.getMessage(t));
      System.exit(0);
    }
  }

  /**
   * Get the action handler for this listener.
   * @return a {@link LauncherListenerProtocolHandler} object.
   * @since 5.0
   */
  public synchronized LauncherListenerProtocolHandler getActionHandler() {
    return actionHandler;
  }

  /**
   * Set the action handler for this listener.
   * @param actionHandler a {@link LauncherListenerProtocolHandler} object.
   * @since 5.0
   */
  public synchronized void setActionHandler(final LauncherListenerProtocolHandler actionHandler) {
    this.actionHandler = actionHandler;
  }
}
